package com.ghosts.abyss.domain;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public final class AbyssDomainCast {

    public static final double DAGON_SKILL_DOMAIN = 8.0;
    /** Same as CreateBarrier: long-press finishes when cnt3 reaches 20 */
    public static final int LONG_PRESS_MAX = 20;
    public static final int OVERWORLD_RADIUS = 22;
    public static final int DURATION_TICKS = 20 * 160;
    public static final double BASE_CE_PER_SECOND = 5.0;

    private static final String NBT_ACTIVE = "abyss_domain_active";
    private static final String NBT_END_TICK = "abyss_domain_end_tick";
    private static final String NBT_COMMITTED = "abyss_domain_committed";

    private AbyssDomainCast() {}

    public static void tickDagonDomain(LevelAccessor world, LivingEntity entity) {
        if (entity.level().isClientSide) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        var data = entity.getPersistentData();

        // Already in an Abyss session as owner
        if (DomainInstanceManager.getByOwner(entity.getUUID()) != null) {
            data.putBoolean(NBT_ACTIVE, true);
            tickActiveDrain(entity);
            return;
        }

        if (entity.level().dimension().equals(DomainDimension.ID)) {
            return;
        }

        // Don't re-enter long-press after we've committed this cast
        if (data.getBoolean(NBT_COMMITTED)) {
            return;
        }

        // ----- JC-identical long press (CreateBarrier cnt3 < 20) -----
        double cnt3 = data.getDouble("cnt3");
        boolean pressZ = data.getBoolean("PRESS_Z");

        if (cnt3 < LONG_PRESS_MAX) {
            if (entity instanceof Player) {
                if (pressZ) {
                    int boxes = (int) Math.round((LONG_PRESS_MAX - cnt3) * 0.25);
                    // cnt3=0 → 5 boxes; cnt3=20 → 0 boxes
                    boxes = Math.max(0, Math.min(5, boxes));

                    String core = Component.translatable("jujutsu.message.long_press").getString();
                    // If lang is wrong/empty, force the exact string you want:
                    if (core == null || core.isEmpty() || core.startsWith("jujutsu.")) {
                        core = "Activate on Long Press";
                    }

                    String block = "⬛".repeat(boxes);
                    String bar = block + core + block;
                    ((Player) entity).displayClientMessage(Component.literal(bar), true);

                    data.putDouble("cnt3", cnt3 + 1.0);
                }}
            return;
        }

        // ----- Long press complete → Abyss domain -----
        data.putBoolean(NBT_COMMITTED, true);
        data.putDouble("select", DAGON_SKILL_DOMAIN);
        data.putDouble("skill_domain", DAGON_SKILL_DOMAIN);

        Vec3 center = entity.position();
        data.putDouble("x_pos_doma", center.x);
        data.putDouble("y_pos_doma", center.y);
        data.putDouble("z_pos_doma", center.z);

        DomainOverworldShell.place(level, center, OVERWORLD_RADIUS);

        long endTick = level.getServer().getTickCount() + DURATION_TICKS;
        data.putLong(NBT_END_TICK, endTick);
        data.putBoolean(NBT_ACTIVE, true);

        DomainLifecycle.startAbyssDomain(entity, DAGON_SKILL_DOMAIN, center, OVERWORLD_RADIUS);

        // Clear JC technique lock so you're not stuck in skill 820
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
        // Match JC: stop the technique
        // If you have access to removing cursed_technique effect, do it here
    }

    private static void tickActiveDrain(LivingEntity entity) {
        // ... keep your existing drain / duration end logic ...
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (entity.tickCount % 20 != 0) return;

        DomainInstance inst = DomainInstanceManager.getByOwner(entity.getUUID());
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
        double m = 1.0;
        var sixEyes = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(
                new net.minecraft.resources.ResourceLocation("jujutsucraft", "six_eyes"));
        if (sixEyes != null && player.hasEffect(sixEyes)) {
            m *= 0.1; // JC six eyes is very strong CE reduction (~90%)
        }
        return m;
    }

    private static void drainPlayerCe(ServerPlayer player, double amount) {
        try {
            player.getCapability(
                    net.mcreator.jujutsucraft.network.JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
            ).ifPresent(cap -> {
                cap.PlayerCursePowerChange -= amount;
                if (cap.PlayerCursePower + cap.PlayerCursePowerChange <= 0) {
                    DomainLifecycle.end(player);
                    player.getPersistentData().putBoolean(NBT_ACTIVE, false);
                    player.getPersistentData().putBoolean(NBT_COMMITTED, false);
                }
            });
        } catch (Throwable ignored) {
            // capability missing in dev, etc.
        }
    }
}