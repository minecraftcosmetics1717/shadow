package com.ghosts.abyss.gojo;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketRedControlState;
import com.ghosts.abyss.progression.GojoProgression;

import net.mcreator.jujutsucraft.entity.RedEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.SetRangedAmmoProcedure;
import net.mcreator.jujutsucraft.procedures.TechniqueRedProcedure;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import virtuoel.pehkui.api.ScaleTypes;

public final class ControlledRedManager {
    public static final String MODE = "abyss_red_mode";
    public static final int CONTROLLED = 1;
    public static final int FIRED = 2;
    public static final int FLYING = 3;
    private static final String ACTIVE = "abyss_red_active";
    private static final String PRESS_TICK = "abyss_red_press_tick";
    private static final String ENTITY_ID = "abyss_red_entity_id";
    private static final String DISTANCE = "abyss_red_control_distance";
    private static final String ALLOW_ORIGINAL = "abyss_red_allow_original";

    private static final int HOLD_TICKS = 5;
    private static final double CE_PER_TICK = 5.0D;
    private static final double MIN_DISTANCE = 2.0D;
    private static final double MAX_DISTANCE = 24.0D;
    private static final double INITIAL_DISTANCE = 3.0D;

    private ControlledRedManager() {}

    // Called AFTER Jujutsucraft's key-pressed procedure selects the skill.
    public static void onPressed(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!GojoProgression.hasSixEyes(player)
                || (int) data.getDouble("skill") != 207
                || !data.getBoolean("PRESS_Z")
                || data.getBoolean(ACTIVE)) {
            return;
        }

        data.putBoolean(ACTIVE, true);
        data.putLong(PRESS_TICK, player.serverLevel().getGameTime());
        data.putDouble(DISTANCE, INITIAL_DISTANCE);
    }

    // Called BEFORE Jujutsucraft clears PRESS_Z on key release.
    public static void onReleased(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(ACTIVE)) {
            return;
        }

        RedEntity red = getControlledRed(player);
        long heldTicks = Math.max(0L,
                player.serverLevel().getGameTime() - data.getLong(PRESS_TICK));

        if (red == null && heldTicks < HOLD_TICKS
                && (int) data.getDouble("skill") == 207
                && GojoProgression.hasSixEyes(player)) {
            // A short tap uses the actual Jujutsucraft procedure.
            data.putBoolean(ALLOW_ORIGINAL, true);
            try {
                TechniqueRedProcedure.execute(player.serverLevel(),
                        player.getX(), player.getY(), player.getZ(), player);
            } finally {
                data.remove(ALLOW_ORIGINAL);
            }
        }

        if (red != null) {
            red.discard();
        }
        reset(player, true);
    }

    public static boolean allowOriginal(ServerPlayer player) {
        return player.getPersistentData().getBoolean(ALLOW_ORIGINAL);
    }

    public static void tick(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(ACTIVE)) {
            return;
        }

        // Key release normally calls onReleased directly. This also covers
        // interruptions where the release callback was missed.
        if (!data.getBoolean("PRESS_Z")) {
            onReleased(player);
            return;
        }

        if ((int) data.getDouble("skill") != 207
                || !GojoProgression.hasSixEyes(player)) {
            cancel(player);
            return;
        }

        RedEntity red = getControlledRed(player);
        if (data.hasUUID(ENTITY_ID) && red == null) {
            cancel(player); // Don't keep respawning an interrupted Red.
            return;
        }

        long heldTicks = player.serverLevel().getGameTime()
                - data.getLong(PRESS_TICK);
        if (red == null) {
            if (heldTicks < HOLD_TICKS) {
                return;
            }
            if (!consumeEnergy(player)) {
                cancel(player);
                return;
            }
            spawnControlled(player);
            return;
        }

        if (!consumeEnergy(player)) {
            cancel(player);
            return;
        }
        Vec3 target = getTarget(player);

        red.setDeltaMovement(Vec3.ZERO);
        red.setPos(target.x, target.y, target.z);
        red.hasImpulse = true;

// Damage and repel nearby entities.
// Apply Blue-style block destruction.
        ControlledRedEffects.tick(player, red);
    }

    private static void spawnControlled(ServerPlayer player) {
        RedEntity red = JujutsucraftModEntities.RED.get()
                .create(player.serverLevel());
        if (red == null) {
            cancel(player);
            return;
        }

        Vec3 target = getTarget(player);
        red.moveTo(target.x, target.y, target.z,
                player.getYRot(), player.getXRot());
        SetRangedAmmoProcedure.execute(player, red);

        CompoundTag tag = red.getPersistentData();
        tag.putUUID("abyss_red_caster", player.getUUID());
        tag.putInt(MODE, CONTROLLED);
        tag.putDouble("cnt1", 0.0D);
        tag.putDouble("cnt6", 0.0D);
        red.getEntityData().set(RedEntity.DATA_flag_start, false);
        red.getEntityData().set(RedEntity.DATA_flag_purple, false);

        var health = red.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(200.0D);
            red.setHealth(200.0F);
        }
        ScaleTypes.BASE.getScaleData(red).setScale(2.3F);

        if (player.serverLevel().addFreshEntity(red)) {
            player.getPersistentData().putUUID(ENTITY_ID, red.getUUID());
            syncControl(player, true);
        } else {
            cancel(player);
        }
    }

    public static void scrollDistance(ServerPlayer player, int direction) {
        if (direction != 1 && direction != -1
                || !isValidControl(player)) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        double current = data.getDouble(DISTANCE);
        // Matches Blue: scroll up increases distance.
        data.putDouble(DISTANCE, Math.max(MIN_DISTANCE,
                Math.min(MAX_DISTANCE, current + direction)));
    }


    public static void fire(ServerPlayer player) {
        if (!isValidControl(player)) {
            return;
        }

        RedEntity red = getControlledRed(player);
        if (red == null) {
            return;
        }

        Vec3 direction = player.getLookAngle().normalize();
        CompoundTag tag = red.getPersistentData();

        // Store the flight direction.
        tag.putDouble("abyss_red_dir_x", direction.x);
        tag.putDouble("abyss_red_dir_y", direction.y);
        tag.putDouble("abyss_red_dir_z", direction.z);

        tag.putDouble("abyss_red_travel", 0.0D);
        tag.putInt(MODE, FLYING);

        // Do not activate the original instant attack yet.
        red.getEntityData().set(
                RedEntity.DATA_flag_start,
                false
        );

        red.setYRot(player.getYRot());
        red.setXRot(player.getXRot());
        red.setDeltaMovement(Vec3.ZERO);

        // Restore the player's controls.
        // Red remains alive and will fly independently.
        reset(player, true);
    }


    private static boolean isValidControl(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.getBoolean(ACTIVE)
                && data.getBoolean("PRESS_Z")
                && (int) data.getDouble("skill") == 207
                && GojoProgression.hasSixEyes(player)
                && getControlledRed(player) != null;
    }

    private static Vec3 getTarget(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        double distance = data.contains(DISTANCE)
                ? data.getDouble(DISTANCE) : MAX_DISTANCE;
        distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance));
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(distance));
        var hit = player.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    private static boolean consumeEnergy(ServerPlayer player) {
        if (player.isCreative()) {
            return true;
        }
        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY,
                (Direction) null).orElse(null);
        if (vars == null || vars.PlayerCursePower < CE_PER_TICK) {
            return false;
        }
        vars.PlayerCursePower -= CE_PER_TICK;
        vars.syncPlayerVariables(player);
        return true;
    }

    private static RedEntity getControlledRed(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.hasUUID(ENTITY_ID)) {
            return null;
        }
        var entity = player.serverLevel().getEntity(data.getUUID(ENTITY_ID));
        if (entity instanceof RedEntity red && red.isAlive()
                && red.getPersistentData().getInt(MODE) == CONTROLLED) {
            return red;
        }
        return null;
    }

    private static void cancel(ServerPlayer player) {
        RedEntity red = getControlledRed(player);
        if (red != null) {
            red.discard();
        }
        reset(player, true);
    }

    public static void cleanup(ServerPlayer player) {
        cancel(player);
    }

    private static void reset(ServerPlayer player, boolean clearSkill) {
        CompoundTag data = player.getPersistentData();
        data.remove(ACTIVE);
        data.remove(PRESS_TICK);
        data.remove(ENTITY_ID);
        data.remove(DISTANCE);
        data.remove(ALLOW_ORIGINAL);
        syncControl(player, false);
        if (clearSkill && (int) data.getDouble("skill") == 207) {
            data.putDouble("skill", 0.0D);
            player.removeEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get());
        }
    }

    private static void syncControl(ServerPlayer player, boolean value) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketRedControlState(value));
    }
}
