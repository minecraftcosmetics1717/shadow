
package com.ghosts.abyss.sukuna;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncPerfectBodyVisible;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Perfect Body gameplay mechanics.
 *
 * Prepared for extraction from EventListeners.
 *
 * IMPORTANT:
 * This class is intentionally NOT registered as an event subscriber.
 * EventListeners still handles these mechanics until the final switch.
 */
public final class PerfectBodyEvents {

    private static final UUID PERFECT_BODY_MODIFIER_UUID =
            UUID.fromString(
                    "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            );

    private static final String PERFECT_BODY_MODIFIER_NAME =
            "perfect_body_bonus";

    private static final Map<UUID, Double> lastCnt5 =
            new HashMap<>();

    private static final Map<UUID, Boolean> lastKnownPerfectBody =
            new HashMap<>();

    private PerfectBodyEvents() {}

    /**
     * Called once per server player tick after activation.
     */
    public static void onPlayerTick(ServerPlayer player) {
        applyPerfectBodyStats(player);
        applyPerfectBodyEffects(player);
        boostChantIfPerfectBody(player);
        checkPerfectBodyChanged(player);
    }
    public static void initializePlayer(ServerPlayer player) {

        boolean state = player.getPersistentData()
                .getBoolean("perfect_body");

        lastKnownPerfectBody.put(player.getUUID(), state);

        broadcastPerfectBodyToAll(player, state);
    }
    private static void applyPerfectBodyStats(ServerPlayer player) {
        boolean perfectBody =
                player.getPersistentData().getBoolean("perfect_body");

        boolean fingerHbActive =
                hasSukunaEffect(player) && getHbAmplifier(player) > 9;

        boolean shouldApply = perfectBody && !fingerHbActive;

        for (Attribute attribute : List.of(
                Attributes.MAX_HEALTH,
                Attributes.ARMOR,
                Attributes.ARMOR_TOUGHNESS
        )) {

            AttributeInstance inst = player.getAttribute(attribute);

            if (inst != null) {
                AttributeModifier existing =
                        inst.getModifier(PERFECT_BODY_MODIFIER_UUID);

                if (shouldApply && existing == null) {
                    inst.addPermanentModifier(
                            new AttributeModifier(
                                    PERFECT_BODY_MODIFIER_UUID,
                                    PERFECT_BODY_MODIFIER_NAME,
                                    0.25D,
                                    AttributeModifier.Operation.MULTIPLY_TOTAL
                            )
                    );

                    if (attribute == Attributes.MAX_HEALTH) {
                        double currentMax = player.getMaxHealth();

                        double roundedMax =
                                Math.ceil(currentMax / 2.0D) * 2.0D;

                        inst.setBaseValue(roundedMax / 1.25D);

                        player.setHealth(
                                Math.min(
                                        player.getHealth(),
                                        player.getMaxHealth()
                                )
                        );
                    }

                } else if (!shouldApply && existing != null) {
                    inst.removeModifier(PERFECT_BODY_MODIFIER_UUID);

                    if (attribute == Attributes.MAX_HEALTH) {
                        inst.setBaseValue(20.0D);

                        player.setHealth(
                                Math.min(
                                        player.getHealth(),
                                        player.getMaxHealth()
                                )
                        );
                    }
                }
            }
        }

        AttributeInstance kbInst =
                player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);

        if (kbInst != null) {
            AttributeModifier kbExisting =
                    kbInst.getModifier(PERFECT_BODY_MODIFIER_UUID);

            if (perfectBody && kbExisting == null) {
                kbInst.addPermanentModifier(
                        new AttributeModifier(
                                PERFECT_BODY_MODIFIER_UUID,
                                PERFECT_BODY_MODIFIER_NAME,
                                1.0D,
                                AttributeModifier.Operation.ADDITION
                        )
                );

            } else if (!perfectBody && kbExisting != null) {
                kbInst.removeModifier(PERFECT_BODY_MODIFIER_UUID);
            }
        }
    }

    private static void applyPerfectBodyEffects(ServerPlayer player) {
        if (isPerfectBodyActive(player)) {

            MobEffect resistance =
                    MobEffects.DAMAGE_RESISTANCE;

            MobEffectInstance existingRes =
                    player.getEffect(resistance);

            if (existingRes == null
                    || existingRes.getAmplifier() < 3) {

                player.addEffect(
                        new MobEffectInstance(
                                resistance,
                                Integer.MAX_VALUE,
                                3,
                                false,
                                false
                        )
                );
            }

            MobEffect healthBoost =
                    MobEffects.HEALTH_BOOST;

            MobEffectInstance existingHb =
                    player.getEffect(healthBoost);

            if (existingHb == null
                    || existingHb.getAmplifier() < 9) {

                player.addEffect(
                        new MobEffectInstance(
                                healthBoost,
                                Integer.MAX_VALUE,
                                9,
                                false,
                                false
                        )
                );
            }
        }
    }

    private static void boostChantIfPerfectBody(ServerPlayer player) {
        if (isPerfectBodyActive(player)) {

            double cnt5 =
                    player.getPersistentData().getDouble("cnt5");

            Double prev =
                    lastCnt5.put(player.getUUID(), cnt5);

            if (prev != null && cnt5 > prev && cnt5 < 20.0D) {
                double boosted = cnt5 + 1.0D;

                player.getPersistentData().putDouble(
                        "cnt5", boosted
                );

                lastCnt5.put(player.getUUID(), boosted);

                Abyss.LOGGER.debug(
                        "[JSlop] chantBoost {} cnt5 {} → {}",
                        player.getGameProfile().getName(),
                        cnt5,
                        boosted
                );

            } else if (prev != null
                    && cnt5 < prev
                    && prev > 0.0D) {

                Abyss.LOGGER.info(
                        "[JSlop] chantBoost {} interrupted: cnt5 {} → {} cooldown_time={}",
                        player.getGameProfile().getName(),
                        prev,
                        cnt5,
                        hasCooldownTime(player)
                );
            }
        }
    }

    private static boolean hasCooldownTime(ServerPlayer player) {
        MobEffect fx =
                ForgeRegistries.MOB_EFFECTS.getValue(
                        new ResourceLocation(
                                "jujutsucraft",
                                "cooldown_time"
                        )
                );

        return fx != null && player.hasEffect(fx);
    }

    private static boolean isPerfectBodyActive(
            ServerPlayer player) {

        return player.getPersistentData()
                .getBoolean("perfect_body");
    }

    private static boolean hasSukunaEffect(
            ServerPlayer player) {

        MobEffect sukunaFx =
                ForgeRegistries.MOB_EFFECTS.getValue(
                        new ResourceLocation(
                                "jujutsucraft",
                                "sukuna_effect"
                        )
                );

        return sukunaFx != null && player.hasEffect(sukunaFx);
    }

    private static int getHbAmplifier(ServerPlayer player) {
        MobEffectInstance hb =
                player.getEffect(MobEffects.HEALTH_BOOST);

        return hb != null ? hb.getAmplifier() : -1;
    }

    private static void checkPerfectBodyChanged(
            ServerPlayer player) {

        if (player.tickCount % 20 == 3) {
            boolean current =
                    player.getPersistentData()
                            .getBoolean("perfect_body");

            Boolean prev =
                    lastKnownPerfectBody.get(player.getUUID());

            if (prev == null || prev != current) {
                lastKnownPerfectBody.put(
                        player.getUUID(), current
                );

                broadcastPerfectBodyToAll(player, current);
            }
        }
    }

    /**
     * Retains the original public synchronization method.
     */
    public static void broadcastPerfectBodyToAll(
            ServerPlayer subject,
            boolean state) {

        boolean effectiveState =
                state && !subject.getPersistentData()
                        .getBoolean("jslop_hide_four_arms");

        PacketSyncPerfectBodyVisible pkt =
                new PacketSyncPerfectBodyVisible(
                        subject.getUUID(),
                        effectiveState
                );

        for (ServerPlayer recipient :
                subject.server.getPlayerList().getPlayers()) {

            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> recipient),
                    pkt
            );
        }
    }
}
