
package com.ghosts.abyss.gojo;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketBlueControlState;
import com.ghosts.abyss.progression.GojoProgression;

import net.mcreator.jujutsucraft.entity.BlueEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.SetRangedAmmoProcedure;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public final class ControlledBlueManager {

    public static final String MODE = "abyss_blue_mode";
    private static final String ENTITY_ID = "abyss_blue_entity_id";
    private static final String ANCHOR_TICKS = "abyss_blue_anchor_ticks";

    public static final int CONTROLLED = 1;
    public static final int ANCHORED = 2;

    private static final double MAX_DISTANCE = 24.0D;
    private static final double MIN_DISTANCE = 2.0D;
    private static final double SCROLL_STEP = 1.0D;
    private static final String BLUE_DISTANCE =
            "abyss_blue_control_distance";
    private static final double CE_PER_TICK = 1.0D;
    private static final int ANCHOR_DURATION = 200;

    private ControlledBlueManager() {}

    public static void tick(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        int skill = (int) data.getDouble("skill");
        boolean holding = data.getBoolean("PRESS_Z");

        BlueEntity blue = getBlue(player);

        if (blue != null) {
            int mode = blue.getPersistentData().getInt(MODE);

            if (mode == ANCHORED) {
                tickAnchored(player, blue);
                return;
            }

            if (mode == CONTROLLED) {
                if (!holding || skill != 206
                        || !GojoProgression.hasSixEyes(player)) {
                    stop(player, blue);
                    return;
                }

                if (!consumeEnergy(player)) {
                    stop(player, blue);
                    return;
                }

                moveToCrosshair(player, blue);
                return;
            }
        }

        if (data.hasUUID(ENTITY_ID)) {
            data.remove(ENTITY_ID);
            syncCamera(player, false);
        }

        if (skill != 206 || !holding) {
            return;
        }

        if (!GojoProgression.hasSixEyes(player)) {
            return;
        }

        spawn(player);
    }

    private static void spawn(ServerPlayer player) {
        BlueEntity blue = JujutsucraftModEntities.BLUE.get()
                .create(player.serverLevel());
        player.getPersistentData().putDouble(
                BLUE_DISTANCE,
                MAX_DISTANCE
        );
        if (blue == null) {
            return;
        }

        Vec3 target = getCrosshairPosition(player);
        blue.moveTo(target.x, target.y, target.z,
                player.getYRot(), player.getXRot());

        // Preserve Jujutsucraft's normal owner setup.
        SetRangedAmmoProcedure.execute(player, blue);

        CompoundTag blueData = blue.getPersistentData();
        blueData.putInt(MODE, CONTROLLED);
        blueData.putBoolean("circle", false);
        blueData.putDouble("cnt6", 0.0D);
        blueData.putDouble("cnt1", 0.0D);

        // Allow the existing Blue AI to operate immediately.
        blue.getEntityData().set(BlueEntity.DATA_flag_start, true);

        var health = blue.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(200.0D);
            blue.setHealth(200.0F);
        }

        var size = blue.getAttribute(
                JujutsucraftModAttributes.SIZE.get()
        );
        if (size != null) {
            size.setBaseValue(5.0D);
        }

        if (player.serverLevel().addFreshEntity(blue)) {
            player.getPersistentData()
                    .putUUID(ENTITY_ID, blue.getUUID());

            syncCamera(player, true);
        }
    }


    private static Vec3 getCrosshairPosition(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        double distance = data.contains(BLUE_DISTANCE)
                ? data.getDouble(BLUE_DISTANCE)
                : MAX_DISTANCE;

        distance = Math.max(
                MIN_DISTANCE,
                Math.min(MAX_DISTANCE, distance)
        );

        Vec3 start = player.getEyePosition();

        Vec3 end = start.add(
                player.getLookAngle().scale(distance)
        );

        var hit = player.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        return hit.getType() == HitResult.Type.MISS
                ? end
                : hit.getLocation();
    }


    private static void moveToCrosshair(
            ServerPlayer player,
            BlueEntity blue
    ) {
        Vec3 target = getCrosshairPosition(player);

        blue.setDeltaMovement(Vec3.ZERO);
        blue.setPos(target.x, target.y, target.z);
        blue.hasImpulse = true;
    }

    private static boolean consumeEnergy(ServerPlayer player) {
        if (player.isCreative()) {
            return true;
        }

        var vars = player.getCapability(
                JujutsucraftModVariables
                        .PLAYER_VARIABLES_CAPABILITY,
                (Direction) null
        ).orElse(null);

        if (vars == null || vars.PlayerCursePower < CE_PER_TICK) {
            return false;
        }

        vars.PlayerCursePower -= CE_PER_TICK;
        vars.syncPlayerVariables(player);
        return true;
    }

    public static void anchor(ServerPlayer player) {
        if (!GojoProgression.hasSixEyes(player)) {
            return;
        }

        if (!player.getPersistentData().getBoolean("PRESS_Z")) {
            return;
        }

        if ((int) player.getPersistentData()
                .getDouble("skill") != 206) {
            return;
        }

        BlueEntity blue = getBlue(player);
        if (blue == null) {
            return;
        }

        CompoundTag data = blue.getPersistentData();
        if (data.getInt(MODE) != CONTROLLED) {
            return;
        }

        data.putInt(MODE, ANCHORED);
        data.putInt(ANCHOR_TICKS, ANCHOR_DURATION);

        blue.setDeltaMovement(Vec3.ZERO);
        player.getPersistentData().remove(BLUE_DISTANCE);
        syncCamera(player, false);
        clearSkill(player);
    }

    private static void tickAnchored(
            ServerPlayer player,
            BlueEntity blue
    ) {
        CompoundTag data = blue.getPersistentData();

        int remaining = data.getInt(ANCHOR_TICKS) - 1;
        data.putInt(ANCHOR_TICKS, remaining);

        blue.setDeltaMovement(Vec3.ZERO);

        if (remaining <= 0) {
            blue.discard();
            player.getPersistentData().remove(ENTITY_ID);
            player.getPersistentData().remove(BLUE_DISTANCE);
        }
    }

    private static void stop(
            ServerPlayer player,
            BlueEntity blue
    ) {
        blue.discard();
        player.getPersistentData().remove(ENTITY_ID);
        syncCamera(player, false);
        clearSkill(player);
        player.getPersistentData().remove(BLUE_DISTANCE);
    }

    private static void clearSkill(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        if ((int) data.getDouble("skill") == 206) {
            data.putDouble("skill", 0.0D);
            player.removeEffect(
                    JujutsucraftModMobEffects
                            .CURSED_TECHNIQUE.get()
            );
        }
    }

    private static BlueEntity getBlue(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        if (!data.hasUUID(ENTITY_ID)) {
            return null;
        }

        var entity = player.serverLevel().getEntity(
                data.getUUID(ENTITY_ID)
        );

        return entity instanceof BlueEntity blue
                ? blue
                : null;
    }

    public static void cleanup(ServerPlayer player) {
        BlueEntity blue = getBlue(player);

        if (blue != null) {
            blue.discard();
        }

        player.getPersistentData().remove(ENTITY_ID);
        player.getPersistentData().remove(BLUE_DISTANCE);
        syncCamera(player, false);
        clearSkill(player);
    }
    private static void syncCamera(
            ServerPlayer player,
            boolean controlling
    ) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketBlueControlState(controlling)
        );
    }

    public static void scrollDistance(
            ServerPlayer player,
            int direction
    ) {
        if (direction != 1 && direction != -1) {
            return;
        }

        if (!GojoProgression.hasSixEyes(player)) {
            return;
        }

        if (!player.getPersistentData()
                .getBoolean("PRESS_Z")) {
            return;
        }

        if ((int) player.getPersistentData()
                .getDouble("skill") != 206) {
            return;
        }

        BlueEntity blue = getBlue(player);

        if (blue == null || blue.getPersistentData()
                .getInt(MODE) != CONTROLLED) {
            return;
        }

        CompoundTag data = player.getPersistentData();

        double distance = data.contains(BLUE_DISTANCE)
                ? data.getDouble(BLUE_DISTANCE)
                : MAX_DISTANCE;

// Scroll up (+1) = farther.
// Scroll down (-1) = closer.
        distance += direction * SCROLL_STEP;

        distance = Math.max(
                MIN_DISTANCE,
                Math.min(MAX_DISTANCE, distance)
        );

        data.putDouble(BLUE_DISTANCE, distance);
    }

}
