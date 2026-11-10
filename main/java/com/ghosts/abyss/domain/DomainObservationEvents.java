
package com.ghosts.abyss.domain;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.progression.AdvTechPoints;

import java.util.List;

import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles witnessing Hollow Wicker Basket and Open Domain.
 *
 * Prepared for extraction from EventListeners.
 *
 * This class is intentionally not registered to Forge's
 * event bus. The existing EventListeners class remains
 * responsible until the final migration.
 */
public final class DomainObservationEvents {

    private static final List<String> HWB_TEACHERS = List.of(
            "jujutsucraft:sukuna_perfect",
            "jujutsucraft:sukuna",
            "jujutsucraft:sukuna_fushiguro",
            "jujutsucraft:reggie_star",
            "jujutsucraft:kashimo_hajime",
            "jujutsucrafts:kashimo_old"
    );

    private static final List<String> OPEN_DOMAIN_NPC_TYPES = List.of(
            "jujutsucraft:kenjaku",
            "jujutsucraft:sukuna",
            "jujutsucraft:sukuna_fushiguro",
            "jujutsucraft:sukuna_perfect"
    );

    private DomainObservationEvents() {}

    /**
     * Call once per server player tick after migration.
     */
    public static void onPlayerTick(ServerPlayer player) {
        checkWitnessedHWB(player);
        checkWitnessedOpenDomain(player);
    }

    /**
     * Detects when a qualified player witnesses an NPC
     * using Hollow Wicker Basket / Simple Domain.
     */
    private static void checkWitnessedHWB(ServerPlayer player) {

        if (!player.getPersistentData()
                .getBoolean("witnessed_hwb")) {

            if (player.level() instanceof ServerLevel) {

                if (hasAdvancement(
                        player,
                        "jujutsucraft:mastery_simple_domain"
                )) {

                    if (hasAdvancement(
                            player,
                            "jujutsucraft:sorcerer_grade_1"
                    )) {

                        Vec3 pos = player.position();

                        List<Entity> nearby =
                                player.level().getEntities(
                                        player,
                                        new AABB(pos, pos).inflate(100.0D),
                                        e -> isHwbTeacher(e)
                                                && hasSimpleDomainEffect(e)
                                );

                        if (!nearby.isEmpty()) {

                            player.getPersistentData().putBoolean(
                                    "witnessed_hwb",
                                    true
                            );

                            player.displayClientMessage(
                                    Component.translatable(
                                            "message.abyss.hwb_witnessed"
                                    ),
                                    true
                            );

                            Abyss.LOGGER.debug(
                                    "{} has witnessed Hollow Wicker Basket.",
                                    player.getDisplayName().getString()
                            );

                            AdvTechPoints.sync(player);
                        }
                    }
                }
            }
        }
    }

    /**
     * Detects when a qualified player witnesses
     * another player's or an NPC's Open Domain.
     */
    private static void checkWitnessedOpenDomain(
            ServerPlayer player) {

        if (!player.getPersistentData()
                .getBoolean("witnessed_open_domain")) {

            if (player.level() instanceof ServerLevel) {

                if (hasAdvancement(
                        player,
                        "jujutsucraft:sorcerer_grade_special"
                )) {

                    Vec3 pos = player.position();

                    List<Entity> nearby =
                            player.level().getEntities(
                                    player,
                                    new AABB(pos, pos).inflate(100.0D),
                                    e -> isOpenDomainActive(e, player)
                            );

                    if (!nearby.isEmpty()) {

                        player.getPersistentData().putBoolean(
                                "witnessed_open_domain",
                                true
                        );

                        player.displayClientMessage(
                                Component.translatable(
                                        "message.abyss.open_domain_witnessed"
                                ),
                                true
                        );

                        Abyss.LOGGER.debug(
                                "{} witnessed an Open Domain.",
                                player.getDisplayName().getString()
                        );

                        OpenDomain.sync(player);
                    }
                }
            }
        }
    }

    private static boolean isOpenDomainActive(
            Entity entity,
            ServerPlayer observer) {

        if (entity instanceof ServerPlayer other) {
            if (other != observer) {
                return other.getPersistentData()
                        .getBoolean("open_domain");
            }
        }

        ResourceLocation type =
                ForgeRegistries.ENTITY_TYPES
                        .getKey(entity.getType());

        if (type == null) {
            return false;

        } else if (!OPEN_DOMAIN_NPC_TYPES.contains(
                type.toString()
        )) {
            return false;

        } else if (entity instanceof LivingEntity living) {

            return living.getActiveEffects()
                    .stream()
                    .anyMatch(effect -> {

                        ResourceLocation loc =
                                ForgeRegistries.MOB_EFFECTS
                                        .getKey(effect.getEffect());

                        return loc != null
                                && loc.toString().equals(
                                "jujutsucraft:domain_expansion"
                        );
                    });

        } else {
            return false;
        }
    }

    private static boolean isHwbTeacher(Entity entity) {

        ResourceLocation type =
                ForgeRegistries.ENTITY_TYPES
                        .getKey(entity.getType());

        return type != null
                && HWB_TEACHERS.contains(type.toString());
    }

    private static boolean hasSimpleDomainEffect(Entity entity) {

        if (entity instanceof LivingEntity living) {

            return living.getActiveEffects()
                    .stream()
                    .anyMatch(effect -> {

                        ResourceLocation loc =
                                ForgeRegistries.MOB_EFFECTS
                                        .getKey(effect.getEffect());

                        return loc != null
                                && loc.toString().equals(
                                "jujutsucraft:simple_domain"
                        );
                    });
        }

        return false;
    }

    private static boolean hasAdvancement(
            ServerPlayer player,
            String id) {

        Advancement adv =
                player.server.getAdvancements()
                        .getAdvancement(new ResourceLocation(id));

        return adv != null
                && player.getAdvancements()
                .getOrStartProgress(adv)
                .isDone();
    }
}
