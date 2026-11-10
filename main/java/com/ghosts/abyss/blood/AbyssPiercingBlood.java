
package com.ghosts.abyss.blood;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketPiercingBloodPose;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.PlayAnimationProcedure;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.network.PacketDistributor;

public final class AbyssPiercingBlood {

    public static final int PREPARATION_TICKS = 40;
    private static final double FIRING_CE_PER_TICK = 3.0D;
    private static final String CHARGE_KEY =
            "abyss_piercing_blood_charge";

    private static final String READY_KEY =
            "abyss_piercing_blood_ready";
    private static final String POSE_KEY =
            "abyss_piercing_blood_pose_active";
    // Slowness II
    private static final int SLOWNESS_AMPLIFIER = 3;

    private AbyssPiercingBlood() {}

    private static void playPiercingBloodAnimation(
            ServerPlayer player
    ) {
        AttributeInstance animation = player.getAttribute(
                JujutsucraftModAttributes.ANIMATION_1.get()
        );

        if (animation == null) {
            return;
        }

        // This is the same animation value used by
        // Jujutsucraft's original Piercing Blood.
        animation.setBaseValue(-4.0D);

        PlayAnimationProcedure.execute(
                player.level(),
                player
        );
    }

    public static void execute(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity
    ) {
        // The original Piercing Blood procedure
        // is intentionally disabled.
        //
        // Our server tick handler controls the
        // preparation and firing states.
    }

    public static void tick(ServerPlayer player) {

        CompoundTag data = player.getPersistentData();

        boolean activeSkill =
                (int) data.getDouble("skill") == 1007;

        boolean holding =
                data.getBoolean("PRESS_Z");

        // If our technique was previously running
        // but is no longer active, clean it up.
        if (!activeSkill || !holding) {

            syncFirstPersonPose(player, false);

            if (isRunning(player)) {
                stop(player);
            }

            return;
        }

// Start the first-person hand pose.
        syncFirstPersonPose(player, true);

        // Apply short-duration slowness continuously
        // during both preparation and firing.
        applySlowness(player);
        playPiercingBloodAnimation(player);
        int charge = data.getInt(CHARGE_KEY);

        if (data.getBoolean(READY_KEY)) {

            if (!consumeFiringEnergy(player)) {
                stop(player);
                return;
            }

            PiercingBloodBeam.update(player);
            return;
        }

        charge++;

        data.putInt(CHARGE_KEY, charge);

        // Spawn critical-hit particles only
        // during the preparation phase.
        if (charge <= PREPARATION_TICKS) {
            spawnPreparationParticles(player);
        }

        if (charge >= PREPARATION_TICKS) {
            data.putBoolean(READY_KEY, true);

            if (!consumeFiringEnergy(player)) {
                stop(player);
                return;
            }

            PiercingBloodBeam.update(player);
        }
    }
    private static void syncFirstPersonPose(
            ServerPlayer player,
            boolean active
    ) {
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(POSE_KEY) == active) {
            return;
        }

        data.putBoolean(POSE_KEY, active);

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketPiercingBloodPose(active)
        );
    }
    private static void applySlowness(
            ServerPlayer player
    ) {

        player.addEffect(
                new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        3,
                        SLOWNESS_AMPLIFIER,
                        false,
                        false,
                        false
                )
        );
    }

    private static void spawnPreparationParticles(
            ServerPlayer player
    ) {

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        level.sendParticles(
                ParticleTypes.CRIT,

                player.getX(),
                player.getY() + player.getBbHeight() * 0.5D,
                player.getZ(),

                4,

                player.getBbWidth() * 0.6D,
                player.getBbHeight() * 0.4D,
                player.getBbWidth() * 0.6D,

                0.03D
        );
    }

    private static boolean isRunning(Entity entity) {

        CompoundTag data = entity.getPersistentData();

        return data.contains(CHARGE_KEY)
                || data.getBoolean(READY_KEY);
    }

    public static void stop(ServerPlayer player) {

        CompoundTag data = player.getPersistentData();
        syncFirstPersonPose(player, false);
        boolean wasRunning = isRunning(player);

        if (wasRunning) {
            PiercingBloodBeam.stop(player);
        }

        reset(player);
        // Only end JC's technique if our Piercing
        // Blood was actually running.
        if (!wasRunning) {
            return;
        }

        // Clear the active skill.
        if ((int) data.getDouble("skill") == 1007) {
            data.putDouble("skill", 0.0D);

            // JC uses this effect to track
            // whether a technique is active.
            player.removeEffect(
                    JujutsucraftModMobEffects
                            .CURSED_TECHNIQUE.get()
            );
        }

        // Do not remove JC's cooldown effects.
        // The temporary slowness expires naturally.
    }

    public static void reset(ServerPlayer player) {

        CompoundTag data = player.getPersistentData();

        data.remove(CHARGE_KEY);
        data.remove(READY_KEY);
        syncFirstPersonPose(player, false);
    }

    private static boolean consumeFiringEnergy(
            ServerPlayer player
    ) {
        // Creative players do not consume CE.
        if (player.isCreative()) {
            return true;
        }

        JujutsucraftModVariables.PlayerVariables vars =
                player.getCapability(
                        JujutsucraftModVariables
                                .PLAYER_VARIABLES_CAPABILITY
                ).orElse(null);

        if (vars == null) {
            return false;
        }

        if (vars.PlayerCursePower < FIRING_CE_PER_TICK) {
            return false;
        }

        vars.PlayerCursePower -= FIRING_CE_PER_TICK;

        // Synchronize the CE display with the client.
        vars.syncPlayerVariables(player);

        return true;
    }

    public static boolean isReady(Entity entity) {

        return entity.getPersistentData()
                .getBoolean(READY_KEY);
    }
}
