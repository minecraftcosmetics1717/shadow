
package com.ghosts.abyss.domain;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import static com.ghosts.abyss.domain.DomainLifecycle.clearCastState;

public final class AbyssDomainCast {

    public static final double DAGON_SKILL_DOMAIN = 8.0;
    public static final double MAHITO_SKILL_DOMAIN = 15.0;

    /** Same as CreateBarrier: long-press finishes when cnt3 reaches 20. */
    public static final int LONG_PRESS_MAX = 20;

    public static final int OVERWORLD_RADIUS = 22;
    public static final int DURATION_TICKS = 20 * 160;
    public static final double BASE_CE_PER_SECOND = 15.0;

    private static final String NBT_ACTIVE = "abyss_domain_active";
    private static final String NBT_END_TICK = "abyss_domain_end_tick";
    private static final String NBT_COMMITTED = "abyss_domain_committed";

    public static final String NBT_CANCEL_CHARGING =
            "abyss_domain_cancel_charging";

    public static final String NBT_CANCEL_CHARGE =
            "abyss_domain_cancel_charge";

    /** Same length as the domain long-press. */
    public static final int CANCEL_CHARGE_TICKS = 20;

    private AbyssDomainCast() {
    }

    public static void tickDagonDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, DAGON_SKILL_DOMAIN);
    }

    public static void tickMahitoDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, MAHITO_SKILL_DOMAIN);
    }

    private static void tickCustomDomain(
            LevelAccessor world,
            LivingEntity entity,
            double domainId
    ) {
        if (entity.level().isClientSide) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        var data = entity.getPersistentData();

        // Already in an Abyss session as owner.
        if (DomainInstanceManager.getByOwner(entity.getUUID()) != null) {
            data.putBoolean(NBT_ACTIVE, true);
            tickActiveDrain(entity);
            return;
        }


        // Don't re-enter long-press after we've committed this cast.
        if (data.getBoolean(NBT_COMMITTED)) {
            return;
        }

        // ----- Long-press charging -----
        double cnt3 = data.getDouble("cnt3");
        boolean pressZ = data.getBoolean("PRESS_Z");

        if (cnt3 < LONG_PRESS_MAX) {
            if (entity instanceof Player player) {
                // Releasing the button before the hold completes cancels it.
                if (!pressZ) {
                    cancelLongPress(entity);
                    return;
                }

                double next = cnt3 + 1.0;
                data.putDouble("cnt3", next);

                // Still charging: show the progress message.
// This tick finishes the hold.
// Do not return: continue below to activate the domain now.
                if (next >= LONG_PRESS_MAX) {
                    data.putDouble("cnt3", LONG_PRESS_MAX);
                } else {
                    // Still charging: update progress and show the charge indicator.
                    data.putDouble("cnt3", next);

                    int boxes = (int) Math.round(
                            (LONG_PRESS_MAX - next) * 0.25
                    );
                    boxes = Math.max(0, Math.min(5, boxes));

                    String core = "Activate on Long Press";
                    String block = "⬛".repeat(boxes);

                    ((Player) entity).displayClientMessage(
                            Component.literal(block + core + block),
                            true
                    );

                    return;
                }

                // Reached the final tick.
                // Continue to domain activation below.
            } else {
                // Mobs automatically charge without needing PRESS_Z.
                double next = cnt3 + 1.0;
                data.putDouble("cnt3", next);

                if (next < LONG_PRESS_MAX) {
                    return;
                }

                // Reached the final tick; continue to activation below.
            }
        }

        // ----- Long press complete: activate the Abyss domain -----
        // ----- Long press complete: activate the Abyss domain -----
        data.putBoolean(NBT_COMMITTED, true);

        data.putDouble("select", domainId);
        data.putDouble("skill_domain", domainId);

// If we're already inside a domain, challenge its owner.
// Do not create another arena or overworld barrier.
        if (entity.level().dimension().equals(DomainDimension.ID)) {

            DomainClash.tryStartClashInDimension(entity);

            // Release JC's active technique state.
            data.putDouble("skill", 0.0);
            data.putDouble("cnt3", 0.0);

            // Allow another attempt if this clash ends or fails.
            data.putBoolean(
                    NBT_COMMITTED,
                    data.getBoolean("abyss_domain_clashing")
            );

            return;
        }

        Vec3 center = entity.position();

        data.putDouble("x_pos_doma", center.x);
        data.putDouble("y_pos_doma", center.y);
        data.putDouble("z_pos_doma", center.z);

        DomainOverworldShell.place(level, center, OVERWORLD_RADIUS);

        long endTick =
                level.getServer().getTickCount() + DURATION_TICKS;

        data.putLong(NBT_END_TICK, endTick);
        data.putBoolean(NBT_ACTIVE, true);

        DomainLifecycle.startAbyssDomain(
                entity,
                domainId,
                center,
                OVERWORLD_RADIUS
        );

        // Clear the JC technique lock so the user isn't stuck in skill 820.
        data.putDouble("skill", 0.0);
        data.putDouble("cnt3", 0.0);
    }

    private static void cancelLongPress(LivingEntity entity) {
        var data = entity.getPersistentData();

        data.putDouble("cnt3", 0.0);
        data.putDouble("select", 0.0);
        data.putDouble("skill", 0.0);

        data.putBoolean(NBT_COMMITTED, false);
        data.putBoolean(NBT_ACTIVE, false);

        // Match JC: stop the technique.
        removeCursedTechniqueEffect(entity);
    }

    private static void tickActiveDrain(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;

        // Process duration and CE drain once per second.
        if (entity.tickCount % 20 != 0) return;

        DomainInstance inst =
                DomainInstanceManager.getByOwner(entity.getUUID());

        if (inst == null) {
            entity.getPersistentData().putBoolean(NBT_ACTIVE, false);
            entity.getPersistentData().putBoolean(NBT_COMMITTED, false);
            return;
        }

        long end = entity.getPersistentData().getLong(NBT_END_TICK);

        if (end > 0 && level.getServer().getTickCount() >= end) {
            DomainLifecycle.end(entity);

            entity.getPersistentData().putBoolean(NBT_ACTIVE, false);
            entity.getPersistentData().putBoolean(NBT_COMMITTED, false);
            return;
        }

        if (entity instanceof ServerPlayer player) {
            double drain = BASE_CE_PER_SECOND * ceMultiplier(player);
            drainPlayerCe(player, drain);
        }
    }

    private static double ceMultiplier(ServerPlayer player) {
        double multiplier = 1.0;

        var sixEyes = ForgeRegistries.MOB_EFFECTS.getValue(
                new ResourceLocation("jujutsucraft", "six_eyes")
        );

        if (sixEyes != null && player.hasEffect(sixEyes)) {
            multiplier *= 0.1;
        }

        return multiplier;
    }

    private static void drainPlayerCe(ServerPlayer player, double amount) {
        try {
            player.getCapability(
                    net.mcreator.jujutsucraft.network
                            .JujutsucraftModVariables
                            .PLAYER_VARIABLES_CAPABILITY
            ).ifPresent(cap -> {
                cap.PlayerCursePowerChange -= amount;

                if (cap.PlayerCursePower + cap.PlayerCursePowerChange <= 0) {
                    DomainLifecycle.end(player);

                    player.getPersistentData().putBoolean(
                            NBT_ACTIVE, false
                    );

                    player.getPersistentData().putBoolean(
                            NBT_COMMITTED, false
                    );
                }
            });
        } catch (Throwable ignored) {
            // Capability may be unavailable in a development environment.
        }
    }

    /**
     * Called every tick while Cancel Domain (slot 21) is the active
     * technique and the skill is running.
     */
    public static void tickCancelDomain(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        var data = entity.getPersistentData();

        // Only owners of an Abyss session can cancel this way.
        DomainInstance inst =
                DomainInstanceManager.getByOwner(entity.getUUID());

        if (inst == null) {
            resetCancelCharge(entity);

            // Not our domain: clear the technique to prevent a soft-lock.
            data.putDouble("skill", 0);
            removeCursedTechniqueEffect(entity);
            return;
        }

        boolean pressZ = data.getBoolean("PRESS_Z");
        int charge = data.getInt(NBT_CANCEL_CHARGE);

        if (!pressZ) {
            // Released early: abort cancellation, but keep the domain running.
            resetCancelCharge(entity);
            data.putDouble("skill", 0);
            removeCursedTechniqueEffect(entity);
            return;
        }

        if (!data.getBoolean(NBT_CANCEL_CHARGING)) {
            data.putBoolean(NBT_CANCEL_CHARGING, true);
            charge = 0;
        }

        int next = charge + 1;

        // The final tick actually cancels the domain.
        if (next >= CANCEL_CHARGE_TICKS) {
            resetCancelCharge(entity);
            commitCancel(entity);
            return;
        }

        data.putInt(NBT_CANCEL_CHARGE, next);

        if (entity instanceof Player player) {
            int boxes = (int) Math.round(
                    (CANCEL_CHARGE_TICKS - next) * 0.25
            );

            boxes = Math.max(0, Math.min(5, boxes));

            String core = "Cancel Domain";
            String block = "⬛".repeat(boxes);

            player.displayClientMessage(
                    Component.literal(block + core + block),
                    true
            );
        }
    }

    public static void commitCancel(LivingEntity entity) {
        // Fully tear down the Abyss domain.
        DomainLifecycle.end(entity);
        clearCastState(entity);
        removeCursedTechniqueEffect(entity);

        entity.getPersistentData().putDouble("skill", 0);
    }

    private static void resetCancelCharge(LivingEntity entity) {
        var data = entity.getPersistentData();

        data.putBoolean(NBT_CANCEL_CHARGING, false);
        data.putInt(NBT_CANCEL_CHARGE, 0);
    }

    private static void removeCursedTechniqueEffect(LivingEntity entity) {
        var cursedTechnique = ForgeRegistries.MOB_EFFECTS.getValue(
                new ResourceLocation("jujutsucraft", "cursed_technique")
        );

        if (cursedTechnique != null) {
            entity.removeEffect(cursedTechnique);
        }
    }
}
