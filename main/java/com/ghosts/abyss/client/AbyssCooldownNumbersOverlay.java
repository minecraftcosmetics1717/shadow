package com.ghosts.abyss.client;

import com.ghosts.abyss.technique.TechniqueCooldownManager;
import com.ghosts.abyss.yuta.ClientYutaTechniqueCache;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Supplies Abyss's cooldown numbers to Jujutsucraft's existing HUD draw calls.
 *
 * This intentionally is NOT a separate render-event subscriber. The previous
 * render-event implementation could suppress JC's text without successfully
 * displaying its own. Keeping JC's verified text positions avoids that issue.
 */
public final class AbyssCooldownNumbersOverlay {

    private AbyssCooldownNumbersOverlay() {}

    /**
     * @param combatCall true for OCoolTimeCombatProcedure, false for OCoolTimeProcedure
     * @return an Abyss-owned countdown (possibly empty) for managed skills;
     *         null to retain JC's ordinary behavior for unmanaged skills.
     */
    public static String textFor(Entity entity, boolean combatCall) {
        if (!(entity instanceof Player player) || !player.level().isClientSide()) {
            return null;
        }

        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);
        if (vars == null) return null;

        double activeCT = vars.SecondTechnique
                ? vars.PlayerCurseTechnique2
                : vars.PlayerCurseTechnique;
        double slot = vars.PlayerSelectCurseTechnique;

        boolean copied = activeCT == 5.0D && slot >= 100.0D;
        int originalSkillId;

        if (copied) {
            int index = (int) Math.round(slot) - 100;
            var copies = ClientYutaTechniqueCache.getAll();
            if (index < 0 || index >= copies.size()) return null;

            originalSkillId = (int) Math.round(copies.get(index).skill());
            if (originalSkillId <= 0) return null;
        } else {
            originalSkillId = (int) Math.round(activeCT * 100.0D + slot);
            if (!TechniqueCooldownManager.isManaged(originalSkillId)) {
                return null; // Never change JC's display for unmanaged abilities.
            }
        }

        // JC's selected-technique HUD chooses which number method to call
        // based on this value. Never display the same number in both slots.
        if (vars.PhysicalAttack != combatCall) {
            return "";
        }

        int ticks = Math.max(0, player.getPersistentData().getInt(
                "abyss_ui_cd_" + originalSkillId
        ));

        // A copied ability's actual combat classification comes from its
        // original skill, not from Yuta's selected-slot number.
        boolean physical = copied
                ? TechniqueCooldownManager.usesCombatCooldown(originalSkillId)
                : vars.PhysicalAttack;

        if (!physical) {
            MobEffectInstance unstable = player.getEffect(
                    JujutsucraftModMobEffects.UNSTABLE.get()
            );
            if (unstable != null) {
                ticks = Math.max(ticks, unstable.getDuration());
            }
        }

        if (ticks <= 0) return "";
        return Long.toString((long) Math.ceil(ticks * 0.1D));
    }
}
