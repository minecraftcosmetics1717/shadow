package com.ghosts.abyss.client;

import com.ghosts.abyss.technique.TechniqueCooldownManager;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Calculates Abyss's HUD countdowns while letting Jujutsucraft draw the text
 * in its established positions. Does not change the actual cooldown timers.
 */
public final class AbyssCooldownNumbersOverlay {

    private AbyssCooldownNumbersOverlay() {}

    private static final String YUTA_INDEX_KEY = "abyss_ui_yuta_copy_index";
    private static final String YUTA_SKILL_KEY = "abyss_ui_yuta_original_skill";

    private static int copiedSkillId(Player player, double ct, double slot) {
        if (ct != 5.0D || slot < 100.0D) return 0;
        int slotIndex = (int) Math.round(slot) - 100;
        CompoundTag data = player.getPersistentData();
        if (!data.contains(YUTA_INDEX_KEY)
                || data.getInt(YUTA_INDEX_KEY) != slotIndex) return 0;
        return data.getInt(YUTA_SKILL_KEY);
    }

    /** Returns 0 when no server-confirmed Yuta copied selection exists. */
    public static int selectedYutaSkill(Player player) {
        if (!player.level().isClientSide()) return 0;
        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
        if (vars == null) return 0;
        double ct = vars.SecondTechnique
                ? vars.PlayerCurseTechnique2 : vars.PlayerCurseTechnique;
        return copiedSkillId(player, ct, vars.PlayerSelectCurseTechnique);
    }

    /** True when the copied technique's individual timer is still visible. */
    public static boolean hasSelectedYutaCooldown(Player player) {
        int id = selectedYutaSkill(player);
        return id != 0 && smoothTicks(player, id) > 0.0D;
    }

    private static double smoothTicks(Player player, int skillId) {
        CompoundTag data = player.getPersistentData();
        String key = Integer.toString(skillId);
        int value = Math.max(0, data.getInt("abyss_ui_cd_" + key));
        String fromKey = "abyss_ui_cd_from_" + key;
        if (!data.contains(fromKey)) return value;

        int from = Math.max(0, data.getInt(fromKey));
        if (from <= value) return value;

        long start = data.getLong("abyss_ui_cd_start_" + key);
        long interval = Math.max(1L, data.getLong("abyss_ui_cd_interval_" + key));
        if (start == 0L) return value;

        double progress = Math.max(0.0D, Math.min(1.0D,
                (System.nanoTime() - start) / (double) interval));
        return Math.max(0.0D, from + (value - from) * progress);
    }

    private static String formatTicks(double ticks) {
        if (ticks <= 0.0D) return "";
        return Long.toString((long) Math.ceil(ticks * 0.1D));
    }

    /** Used for the selected-skill countdown above JC's ability icon. */
    public static String selectedYutaText(Entity entity) {
        if (!(entity instanceof Player player) || !player.level().isClientSide()) {
            return null;
        }
        int skillId = selectedYutaSkill(player);
        return skillId == 0 ? null : formatTicks(smoothTicks(player, skillId));
    }

    /**
     * @param combatCall true for Jujutsucraft's combat text position.
     * @return an Abyss number or blank for managed skills, or null to keep
     * Jujutsucraft's original behavior for unmanaged ordinary abilities.
     */
    public static String textFor(Entity entity, boolean combatCall) {
        if (!(entity instanceof Player player) || !player.level().isClientSide()) {
            return null;
        }

        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
        if (vars == null) return null;

        double ct = vars.SecondTechnique
                ? vars.PlayerCurseTechnique2 : vars.PlayerCurseTechnique;
        double slot = vars.PlayerSelectCurseTechnique;
        boolean copied = ct == 5.0D && slot >= 100.0D;

        int skillId;
        if (copied) {
            skillId = copiedSkillId(player, ct, slot);
            if (skillId == 0) return null;
        } else {
            skillId = (int) Math.round(ct * 100.0D + slot);
            if (!TechniqueCooldownManager.isManaged(skillId)) return null;
        }

        // Jujutsucraft decides whether to draw its magic or combat text
        // using PhysicalAttack. Use that same position for our number.
        if (vars.PhysicalAttack != combatCall) return "";

        // The server includes burnout and Unstable in the selected nonphysical
        // timer, so both accelerate smoothly with the individual cooldown.
        return formatTicks(smoothTicks(player, skillId));
    }
}
