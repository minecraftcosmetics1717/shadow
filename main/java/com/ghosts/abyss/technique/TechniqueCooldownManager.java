
package com.ghosts.abyss.technique;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class TechniqueCooldownManager {

    private static final String COOLDOWNS_KEY =
            "abyss_individual_cooldowns";

    private TechniqueCooldownManager() {}


    // Mahito's moves managed by Abyss.
    // Slot 1520 (Domain Expansion) is intentionally excluded.
    private static final int[] MANAGED_SKILLS = {
            1503, // Attack Speed
            1504, // Attack Overhead (future Soul Grasp)
            1505, // Idle Transfiguration
            1506, // Mahito Hand 3
            1507, // Mahito Gun
            1508, // Body Repel Single
            1509, // Body Repel
            1510, // Polymorphic Soul Isomer
            1515, // Instant Spirit Body
            1516  // Attack Strong Mahito
    };

    public static int[] getManagedSkillIds() {
        return MANAGED_SKILLS.clone();
    }

    public static boolean isManaged(int skillId) {
        for (int id : MANAGED_SKILLS) {
            if (id == skillId) {
                return true;
            }
        }

        return false;
    }

    // These moves use JC's physical combat cooldown
    // rather than its magic technique cooldown.
    public static boolean usesCombatCooldown(int skillId) {
        return skillId == 1503
                || skillId == 1504
                || skillId == 1516;
    }


    // Get the full skill ID from JC's selected technique.
    // Supports primary and secondary cursed techniques.
    public static int getSelectedSkill(ServerPlayer player) {
        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);

        if (vars == null) {
            return 0;
        }

        double technique = vars.SecondTechnique
                ? vars.PlayerCurseTechnique2
                : vars.PlayerCurseTechnique;

        double slot = vars.PlayerSelectCurseTechnique;

        return (int) Math.round(technique * 100.0D + slot);
    }

    // Use the overworld's persistent game clock so
    // cooldowns survive ordinary reconnects/restarts.
    private static long now(ServerPlayer player) {
        return player.serverLevel()
                .getServer()
                .overworld()
                .getGameTime();
    }

    public static void startCooldown(
            ServerPlayer player,
            int skillId,
            int durationTicks
    ) {
        if (durationTicks <= 0) {
            return;
        }

        CompoundTag playerData =
                player.getPersistentData();

        CompoundTag cooldowns =
                playerData.getCompound(COOLDOWNS_KEY);

        String key = Integer.toString(skillId);

        long newEnd = now(player) + durationTicks;
        long previousEnd = cooldowns.getLong(key);

        // Don't shorten an existing cooldown.
        if (newEnd > previousEnd) {
            cooldowns.putLong(key, newEnd);
            playerData.put(COOLDOWNS_KEY, cooldowns);
        }
    }

    public static int getRemainingTicks(
            ServerPlayer player,
            int skillId
    ) {
        CompoundTag cooldowns =
                player.getPersistentData()
                        .getCompound(COOLDOWNS_KEY);

        long end = cooldowns.getLong(
                Integer.toString(skillId)
        );

        long remaining = Math.max(
                0L,
                end - now(player)
        );

        return (int) Math.min(
                remaining,
                Integer.MAX_VALUE
        );
    }

    public static boolean isOnCooldown(
            ServerPlayer player,
            int skillId
    ) {
        return getRemainingTicks(player, skillId) > 0;
    }

    // Called whenever JC tries to apply its old
    // shared cooldown during a managed move.
    public static void captureJcCooldown(
            ServerPlayer player,
            int skillId,
            int durationTicks
    ) {
        if (!isManaged(skillId)
                || durationTicks <= 0) {
            return;
        }

        // JC's attack procedures also read
        // COOLDOWN_TICKS, so preserve that value.
        CompoundTag data = player.getPersistentData();

        data.putDouble(
                "COOLDOWN_TICKS",
                Math.max(
                        data.getDouble("COOLDOWN_TICKS"),
                        durationTicks
                )
        );

        startCooldown(
                player,
                skillId,
                durationTicks
        );
    }
}
