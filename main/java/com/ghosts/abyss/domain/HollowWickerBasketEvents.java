
package com.ghosts.abyss.domain;

import com.ghosts.abyss.Abyss;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles active Hollow Wicker Basket mechanics.
 *
 * Prepared for extraction from EventListeners.
 *
 * Intentionally not registered to the Forge event bus yet.
 */
public final class HollowWickerBasketEvents {

    private static final Map<UUID, Integer> hwbParticleTick =
            new HashMap<>();

    private static final List<String> HWB_STRIP_EFFECTS = List.of(
            "jujutsucraft:neutralization",
            "jujutsucraft:brain_damage"
    );

    private HollowWickerBasketEvents() {}

    /**
     * Clears Hollow Wicker Basket state when a player dies.
     *
     * Called by PlayerLifecycleEvents.
     */
    public static void clearPlayer(ServerPlayer player) {

        // Disable Hollow Wicker Basket.
        player.getPersistentData().putBoolean(
                "hwb_active",
                false
        );

        // Reset the particle counter.
        hwbParticleTick.remove(player.getUUID());
    }

    /**
     * Clears the particle counter for a player.
     *
     * Can be called during player disconnect cleanup.
     */
    public static void clearPlayer(UUID uuid) {

        hwbParticleTick.remove(uuid);
    }

    /**
     * Called once per server player tick after migration.
     *
     * Preserves the original ordering:
     * strip effects -> particles -> action lock.
     */
    public static void onPlayerTick(ServerPlayer player) {
        stripDomainEffects(player);
        spawnHwbParticle(player);
        applyHwbActionLock(player);
    }

    private static void applyHwbActionLock(ServerPlayer player) {

        if (!player.getPersistentData().getBoolean("perfect_body")) {

            if (player.getPersistentData().getBoolean("hwb_active")) {

                try {
                    Class<?> effectsClass = Class.forName(
                            "net.mcreator.jujutsucraft.init."
                                    + "JujutsucraftModMobEffects"
                    );

                    for (String fieldName : List.of(
                            "COOLDOWN_TIME",
                            "COOLDOWN_TIME_COMBAT"
                    )) {

                        Field f = effectsClass.getDeclaredField(
                                fieldName
                        );

                        f.setAccessible(true);

                        Object supplier = f.get(null);

                        MobEffect effect = (MobEffect) supplier
                                .getClass()
                                .getMethod("get")
                                .invoke(supplier);

                        player.addEffect(
                                new MobEffectInstance(
                                        effect,
                                        5,
                                        0,
                                        true,
                                        false
                                )
                        );
                    }

                } catch (Exception e) {
                    Abyss.LOGGER.warn(
                            "HWB: could not apply action lock: {}",
                            e.getMessage()
                    );
                }
            }
        }
    }

    private static void spawnHwbParticle(ServerPlayer player) {

        if (player.getPersistentData().getBoolean("hwb_active")) {

            if (player.level() instanceof ServerLevel serverLevel) {

                int tick = hwbParticleTick.merge(
                        player.getUUID(),
                        1,
                        Integer::sum
                );

                if (tick % 2 == 1) {

                    try {
                        Class<?> particleClass = Class.forName(
                                "net.mcreator.jujutsucraft.init."
                                        + "JujutsucraftModParticleTypes"
                        );

                        Field f = particleClass.getDeclaredField(
                                "PARTICLE_HOLLOW_WICKER_BASKET"
                        );

                        f.setAccessible(true);

                        Object supplier = f.get(null);

                        SimpleParticleType particleType =
                                (SimpleParticleType) supplier
                                        .getClass()
                                        .getMethod("get")
                                        .invoke(supplier);

                        double yaw = Math.toRadians(
                                player.getYRot() + 90.0F
                        );

                        double pitch = Math.toRadians(
                                player.getXRot()
                        );

                        double xPos =
                                player.getX()
                                        + Math.cos(yaw)
                                        * Math.cos(pitch)
                                        * (-1.0D - player.getBbWidth());

                        double yPos =
                                player.getY()
                                        + player.getBbHeight() * 0.5D;

                        double zPos =
                                player.getZ()
                                        + Math.sin(yaw)
                                        * Math.cos(pitch)
                                        * (-1.0D - player.getBbWidth());

                        serverLevel.sendParticles(
                                particleType,
                                xPos,
                                yPos,
                                zPos,
                                0,
                                0.0D,
                                0.0D,
                                0.0D,
                                0.0D
                        );

                    } catch (Exception e) {
                        Abyss.LOGGER.warn(
                                "HWB: could not spawn particle: {}",
                                e.getMessage()
                        );
                    }
                }
            }
        }
    }

    private static void stripDomainEffects(ServerPlayer player) {

        if (player.getPersistentData().getBoolean("hwb_active")) {

            for (String effectId : HWB_STRIP_EFFECTS) {

                Optional<MobEffect> effectToRemove =
                        player.getActiveEffects()
                                .stream()
                                .filter(e -> {

                                    ResourceLocation loc =
                                            ForgeRegistries.MOB_EFFECTS
                                                    .getKey(e.getEffect());

                                    return loc != null
                                            && loc.toString()
                                            .equals(effectId);
                                })
                                .map(MobEffectInstance::getEffect)
                                .findFirst();

                effectToRemove.ifPresent(player::removeEffect);
            }
        }
    }

    /**
     * Future LivingHurtEvent handler.
     *
     * During final migration, register this method with
     * @SubscribeEvent(priority = EventPriority.LOWEST).
     */
    public static void onPlayerHurtCancelHwb(LivingHurtEvent event) {

        if (event.getEntity() instanceof ServerPlayer player) {

            if (player.getPersistentData().getBoolean("hwb_active")) {

                float maxHp = player.getMaxHealth();

                if (maxHp > 0.0F) {

                    float healthAfter =
                            player.getHealth() - event.getAmount();

                    boolean heavyBlow =
                            (event.getAmount() / maxHp) >= 0.3F;

                    boolean lowHealth =
                            (healthAfter / maxHp) < 0.35F;

                    if (heavyBlow || lowHealth) {

                        player.getPersistentData().putBoolean(
                                "hwb_active",
                                false
                        );

                        player.getPersistentData().putLong(
                                "jslop_hwb_cooldown_until",
                                player.level().getGameTime() + 200L
                        );

                        hwbParticleTick.remove(player.getUUID());

                        try {
                            Class<?> attrsClass = Class.forName(
                                    "net.mcreator.jujutsucraft.init."
                                            + "JujutsucraftModAttributes"
                            );

                            for (String field : new String[]{
                                    "ANIMATION_1",
                                    "ANIMATION_2"
                            }) {

                                Field f = attrsClass.getDeclaredField(
                                        field
                                );

                                f.setAccessible(true);

                                Object supplier = f.get(null);

                                Attribute attr = (Attribute) supplier
                                        .getClass()
                                        .getMethod("get")
                                        .invoke(supplier);

                                AttributeInstance inst =
                                        player.getAttribute(attr);

                                if (inst != null) {
                                    inst.setBaseValue(0.0D);
                                }
                            }

                        } catch (Exception e) {
                            Abyss.LOGGER.warn(
                                    "HWB break: could not reset animation: {}",
                                    e.getMessage()
                            );
                        }
                    }
                }
            }
        }
    }

    /**
     * Future LivingEntityUseItemEvent.Start handler.
     */
    public static void onItemUseStart(
            LivingEntityUseItemEvent.Start event) {

        if (event.getEntity() instanceof ServerPlayer player) {

            if (player.getPersistentData().getBoolean("hwb_active")) {
                event.setCanceled(true);
            }
        }
    }
}
