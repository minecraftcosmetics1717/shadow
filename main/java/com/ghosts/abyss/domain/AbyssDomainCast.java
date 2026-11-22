
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

    public static final double SUKUNA_SKILL_DOMAIN = 1.0;
    public static final double GOJO_SKILL_DOMAIN = 2.0;
    public static final double MEGUMI_SKILL_DOMAIN = 6.0;
    public static final double YUTA_SKILL_DOMAIN = 5.0;
    public static final double JOGO_SKILL_DOMAIN = 4.0;
    public static final double DAGON_SKILL_DOMAIN = 8.0;
    public static final double MAHITO_SKILL_DOMAIN = 15.0;
    public static final double NAOYA_SKILL_DOMAIN = 19.0;
    public static final double HAKARI_SKILL_DOMAIN = 29.0;
    public static final double URAUME_SKILL_DOMAIN = 24.0;
    public static final double RYU_SKILL_DOMAIN = 12.0;
    public static final double CHOSO_SKILL_DOMAIN = 10.0;
    public static final double YOROZU_SKILL_DOMAIN = 39.0;
    public static final double URO_SKILL_DOMAIN = 38.0;
    public static final double YUKI_SKILL_DOMAIN = 9.0;

    /** Same as CreateBarrier: long-press finishes when cnt3 reaches 20. */
    public static final int LONG_PRESS_MAX = 20;
    public static final double DOMAIN_ACTIVATION_COST = 1250.0D;
    public static final int OVERWORLD_RADIUS = 22;
    public static final int DURATION_TICKS = 20 * 160;

    private static final String NBT_ACTIVE = "abyss_domain_active";
    private static final String NBT_END_TICK = "abyss_domain_end_tick";
    private static final String NBT_COMMITTED = "abyss_domain_committed";

    public static final String NBT_CANCEL_CHARGING =
            "abyss_domain_cancel_charging";

    public static final String NBT_CANCEL_CHARGE =
            "abyss_domain_cancel_charge";

    /** Same length as the domain long-press. */
    public static final int CANCEL_CHARGE_TICKS = 20;

    private AbyssDomainCast() {}
    public static void tickSukunaDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, SUKUNA_SKILL_DOMAIN);}
    public static void tickGojoDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, GOJO_SKILL_DOMAIN);}
    public static void tickMegumiDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, MEGUMI_SKILL_DOMAIN);}
    public static void tickJogoDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, JOGO_SKILL_DOMAIN);}
    public static void tickDagonDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, DAGON_SKILL_DOMAIN);}
    public static void tickMahitoDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, MAHITO_SKILL_DOMAIN);}
    public static void tickNaoyaDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, NAOYA_SKILL_DOMAIN);}
    public static void tickYutaDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, YUTA_SKILL_DOMAIN);}
    public static void tickHakariDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, HAKARI_SKILL_DOMAIN);}
    public static void tickUraumeDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, URAUME_SKILL_DOMAIN);}
    public static void tickRyuDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, RYU_SKILL_DOMAIN);}
    public static void tickChosoDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, CHOSO_SKILL_DOMAIN);}
    public static void tickYorozuDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, YOROZU_SKILL_DOMAIN);}
    public static void tickUroDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, URO_SKILL_DOMAIN);}
    public static void tickYukiDomain(LevelAccessor world, LivingEntity entity) {
        tickCustomDomain(world, entity, YUKI_SKILL_DOMAIN);}


    private static boolean payDomainActivationCost(
            LivingEntity entity) {
        // JC cursed energy capability is player-specific.
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }

        var capability = player.getCapability(
                net.mcreator.jujutsucraft.network
                        .JujutsucraftModVariables
                        .PLAYER_VARIABLES_CAPABILITY).resolve();

        if (capability.isEmpty()) {
            return false;
        }
        var cap = capability.get();
        double available =
                cap.PlayerCursePower
                        + cap.PlayerCursePowerChange;
        if (available < DOMAIN_ACTIVATION_COST) {
            player.displayClientMessage(
                    Component.literal(
                            "Not enough cursed energy"),
                    true);
            return false;
        }

        // Deduct the complete cost once.
        cap.PlayerCursePowerChange -= DOMAIN_ACTIVATION_COST;

        return true;
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
// Brain Damage and Unstable prevent new domain expansions.
        if (DomainCooldown.cannotExpand(entity)) {

            // Stop the attempted cast.
            data.putDouble("cnt3", 0.0);
            data.putDouble("skill", 0.0);

            data.putBoolean(NBT_COMMITTED, false);

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
// Pay once when the long press completes.
// This runs before the committed flag is set.
        if (!payDomainActivationCost(entity)) {
            data.putDouble("cnt3", 0.0D);
            data.putDouble("skill", 0.0D);
            data.putBoolean(NBT_COMMITTED, false);
            data.putBoolean(NBT_ACTIVE, false);
            removeCursedTechniqueEffect(entity);
            return;
        }
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


// The domain is starting, but its duration
// does not begin until entry is confirmed.
        data.putLong(NBT_END_TICK, 0L);
        data.putBoolean(NBT_ACTIVE, true);

        DomainLifecycle.startAbyssDomain(
                entity,
                domainId,
                center,
                OVERWORLD_RADIUS
        );

// Clear JC's running skill state.
        data.putDouble("skill", 0.0D);
        data.putDouble("cnt3", 0.0D);

// Open domains don't teleport the caster or give them
// a replacement domain effect. Release JC's technique
// effect after the open-domain session has started.
        if (OpenDomainLifecycle.isActive(entity.getUUID())) {
            removeCursedTechniqueEffect(entity);
        }
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

// The caster is still entering the domain.
// Don't start duration checks or CE drain yet.
        if (end <= 0L) {
            return;
        }

        if (level.getServer().getTickCount() >= end) {
            DomainLifecycle.end(entity);

            entity.getPersistentData().putBoolean(NBT_ACTIVE, false);
            entity.getPersistentData().putBoolean(NBT_COMMITTED, false);
            return;
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

        if (inst == null
                && !OpenDomainLifecycle.isActive(entity.getUUID())) {
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
        if (entity.level().isClientSide) return;

        var data = entity.getPersistentData();

        // Stop either an open domain or a closed domain.
        DomainLifecycle.end(entity);

        // Ensure the cancellation procedure cannot
        // leave its authorization or charge active.
        data.putBoolean("abyss_cancel_domain_auth", false);
        data.putBoolean(NBT_CANCEL_CHARGING, false);
        data.putInt(NBT_CANCEL_CHARGE, 0);

        // Release JC's technique state.
        data.putDouble("skill", 0.0D);
        data.putDouble("cnt3", 0.0D);

        clearCastState(entity);
        removeCursedTechniqueEffect(entity);
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
