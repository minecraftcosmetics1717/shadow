
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

            // ===== Sukuna =====
            105, 106, 107, 111, 112, 113,

            // ===== Gojo =====
            206, 207, 208, 209, 215,

            // ===== Choso / Blood Manipulation =====
            1005, 1006, 1007, 1008, 1016, 1018, 1019,

            // ===== Hakari =====
            2904, 2905, 2906, 2907,

            // ===== Naoya =====
            1907, 1910,

            // ===== Uraume =====
            2405, 2406, 2408, 2409,

            // ===== Dagon =====
            805, 806, 807, 809, 810,

            // ===== Yuki Tsukumo =====
            904, 905, 906,

            // ===== Yorozu =====
            3905, 3906, 3907, 3908, 3909,

            // ===== Nanami =====
            1306, 1307, 1310,

            // ===== Jogo =====
            405, 406, 407, 408, 409, 415,

            // ===== Megumi / Ten Shadows =====
            605, 606, 607, 608, 609, 610, 612, 613, 614, 615, 617,

            // ===== Kashimo =====
            703, 704, 716, 717, 719,

            // ===== Mahito =====
            1503, 1504, 1505, 1506, 1507, 1508, 1509, 1510, 1515, 1516
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
        return
                // Sukuna's physical Dismantle combos
                skillId == 111
                        || skillId == 112
                        || skillId == 113

                        // Mahito's physical attacks
                        || skillId == 1503
                        || skillId == 1504
                        || skillId == 1516
                        || skillId == 2904

                        || skillId == 904
                        || skillId == 3905
                        || skillId == 3906
                        || skillId == 3907

                        || skillId == 1306 // Nanami Jump Attack
                        || skillId == 1307 // Nanami Consecutive Attacks
                        || skillId == 703  // Kashimo Rotating Kick
                        || skillId == 704  // Kashimo Jump Attack
                        || skillId == 719;  // Kashimo Flying Kick

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
