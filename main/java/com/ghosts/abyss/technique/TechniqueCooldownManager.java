package com.ghosts.abyss.technique;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class TechniqueCooldownManager {

    private static final String COOLDOWNS_KEY =
            "abyss_individual_cooldowns";
    private static final String POST_DOMAIN_COOLDOWN_KEY =
            "abyss_post_domain_cooldown_end";
    public static final int POST_DOMAIN_COOLDOWN_TICKS = 20 * 45;

    private TechniqueCooldownManager() {}

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
            1305, 1306, 1307, 1310,

            // ===== Angel =====
            2815,

            // ===== Ryu Ishigori =====
            1205, 1206,

            // ===== Jogo =====
            405, 406, 407, 408, 409, 415,

            // ===== Megumi / Ten Shadows =====
            605, 606, 607, 608, 609, 610, 612, 613, 614, 615, 617,

            // ===== Kashimo =====
            703, 704, 716, 717, 719,

            // ===== Physical Gifted =====
            -100, -99, -98, -95, -94, -93,

            // ===== Nobara Kugisaki =====
            3405, 3406, 3407,

            // ===== Junpei =====
            3505,

            // ===== Aoi =====
            2003, 2004, 2005, 2006, 2007, 2015,

            // ===== Uro Takako =====
            3809, 3810,
            // ===== Mahito =====
            1503, 1504, 1505, 1506, 1507, 1508, 1509, 1510, 1511, 1515, 1516,

            // ===== Cursed Speech =====
            304, 305, 306, 307, 308, 309,

            // ===== Kaori Itadori =====
            4105, 4110,

            // ===== Masamichi Yaga =====
            3306, 3310, 3315,

            // ===== Ogi Zenin =====
            2605, 2611, 2612, 2613, 2614, 2615, 2619,

            // ===== Hanami =====
            1405, 1406, 1410, 1415,

            // ===== Reggie Star =====
            4405, 4410, 4411, 4415,

            // ===== Kurourushi =====
            2304, 2305, 2306, 2307, 2308,

            // ===== Miguel =====
            3005, 3006,

            // ===== Higuruma =====
            2705, 2706, 2707, 2708, 2710, 2711, 2719,

            // ===== Nishimiya =====
            3605, 3606, 3610,

            // ===== Chojuro =====
            3205, 3209,

            // ===== Kusakabe =====
            3105, 3107,

            // ===== Smallpox Deity =====
            2505, 2506,

            // ===== Ranta Zenin =====
            4610,

            // ===== Dhruv =====
            3705, 3706,

            // ===== Takaba =====
            1705, 1706, 1710, 1715,

            // ===== Jinichi Zenin =====
            2205, 2207, 2208, 2210,

            // ===== Hazenoki =====
            4505, 4510,

            // ===== Geto / Cursed Spirit Manipulation =====
            1812, 1815
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

    public static boolean usesCombatCooldown(int skillId) {
        return
                // Sukuna's physical Dismantle combos
                skillId == 111
                        || skillId == 112
                        || skillId == 113

                        // Mahito's physical attacks
                        || skillId == 1503
                        || skillId == 1504
                        || skillId == 1511
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
                        || skillId == 719 // Kashimo Flying Kick

                        // ===== Aoi =====
                        || skillId == 2003 // Overhead Attack
                        || skillId == 2004 // Jump Attack
                        || skillId == 2006 // Lariat
                        || skillId == 2007 // Consecutive Attacks
                        || skillId == 2015 // Black Flash Attack
                        // Physical Gifted
                        || skillId == -100
                        || skillId == -99
                        || skillId == -98
                        || skillId == -95
                        || skillId == -94
                        || skillId == -93

                        // ===== Kurourushi =====
                        || skillId == 2304

                        // ===== Miguel =====
                        || skillId == 3006

                        // ===== Uro =====
                        || skillId == 3809

                        // ===== Higuruma =====
                        || skillId == 2705
                        || skillId == 2706
                        || skillId == 2707
                        || skillId == 2708
                        || skillId == 2710
                        || skillId == 2711
                        || skillId == 2719

                        // ===== Kusakabe =====
                        || skillId == 3105
                        || skillId == 3107

                        // ===== Takaba =====
                        || skillId == 1706;

    }

    // Get the full skill ID from JC's selected technique.
    // Supports primary and secondary cursed techniques.
    public static int getSelectedSkill(ServerPlayer player) {
        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
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

    // Apply 2 minutes of cursed technique burnout
    // after a domain ends or the caster loses a clash.
    public static void startPostDomainCooldown(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag data = player.getPersistentData();

        long endTick = now(player) + POST_DOMAIN_COOLDOWN_TICKS;
        long previousEnd = data.getLong(POST_DOMAIN_COOLDOWN_KEY);

        // Never shorten an existing burnout.
        if (endTick > previousEnd) {
            data.putLong(POST_DOMAIN_COOLDOWN_KEY, endTick);
        }
    }

    public static int getPostDomainRemainingTicks(ServerPlayer player) {
        long endTick = player.getPersistentData()
                .getLong(POST_DOMAIN_COOLDOWN_KEY);

        long remaining = Math.max(0L, endTick - now(player));

        return (int) Math.min(remaining, Integer.MAX_VALUE);
    }

    // JujutsuFIN-style RCT acceleration:
    // subtract 20 EXTRA ticks per game tick.
    // Physical combat cooldowns are not accelerated.
    public static boolean accelerateNonPhysicalCooldowns(
            ServerPlayer player, int extraTicks
    ) {
        if (extraTicks <= 0) return false;

        CompoundTag data = player.getPersistentData();
        long currentTick = now(player);
        boolean changed = false;

        // 1. Post-domain cursed technique burnout.
        long burnoutEnd =
                data.getLong(POST_DOMAIN_COOLDOWN_KEY);

        if (burnoutEnd - currentTick > 0L) {
            data.putLong(
                    POST_DOMAIN_COOLDOWN_KEY,
                    Math.max(currentTick, burnoutEnd - extraTicks)
            );
            changed = true;
        }

        // 2. Ordinary nonphysical individual cooldowns.
        CompoundTag cooldowns =
                data.getCompound(COOLDOWNS_KEY);

        boolean changedSkills = false;

        for (int skillId : MANAGED_SKILLS) {
            if (usesCombatCooldown(skillId)) {
                continue;
            }

            String key = Integer.toString(skillId);
            long end = cooldowns.getLong(key);

            if (end - currentTick > 0L) {
                cooldowns.putLong(
                        key,
                        Math.max(currentTick, end - extraTicks));

                changed = true;
                changedSkills = true;
            }
        }

        if (changedSkills) {
            data.put(COOLDOWNS_KEY, cooldowns);
        }

        return changed;
    }

    // JC tracks whether the selected technique is physical.
    // This is more accurate than checking the combat cooldown list.
    public static boolean isSelectedPhysicalSkill(ServerPlayer player) {
        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
        return vars != null && vars.PhysicalAttack;
    }

    // Preserve burnout if a player respawns after dying.
    public static void copyPostDomainCooldown(
            Player original,
            Player replacement) {
        long endTick = original.getPersistentData()
                .getLong(POST_DOMAIN_COOLDOWN_KEY);

        if (endTick > 0L) {
            replacement.getPersistentData()
                    .putLong(POST_DOMAIN_COOLDOWN_KEY, endTick);
        }
    }

    public static void startCooldown(
            ServerPlayer player,
            int skillId,
            int durationTicks) {
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
            int skillId) {
        CompoundTag cooldowns =
                player.getPersistentData()
                        .getCompound(COOLDOWNS_KEY);

        long end = cooldowns.getLong(
                Integer.toString(skillId));

        long remaining = Math.max(
                0L,
                end - now(player));

        return (int) Math.min(
                remaining,
                Integer.MAX_VALUE);
    }

    public static boolean isOnCooldown(
            ServerPlayer player,
            int skillId) {
        return getRemainingTicks(player, skillId) > 0;
    }

    // Called whenever JC tries to apply its old
    // shared cooldown during a managed move.
    public static void captureJcCooldown(
            ServerPlayer player,
            int skillId,
            int durationTicks) {
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
                        durationTicks));

        startCooldown(
                player,
                skillId,
                durationTicks);
    }
}
