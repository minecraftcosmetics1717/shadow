package com.ghosts.abyss.grade;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Computes JC-equivalent health using its PlayerLevel, without Health Boost. */
public final class GradeHealthCalculator {
    private GradeHealthCalculator() {}

    public static double calculateBonus(ServerPlayer player) {
        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
        if (vars == null) return 0.0D;

        // The rank is now determined by JC's advancement system.
        // PlayerLevel is the effective level JC uses for combat, including
        // its normal special-character adjustments below.
        double level = vars.PlayerLevel;
        double ct1 = vars.PlayerCurseTechnique;
        double ct2 = vars.PlayerCurseTechnique2;
        double sukunaLevel = 0.0D;

        // Match JC's PlayerCT1 != 0 requirement.
        if (ct1 == 0.0D) return 0.0D;

        if (ct1 == 21.0D || ct2 == 21.0D) {
            level = Math.max(level + 3.0D, level * 1.2D);
        }

        var sukuna = player.getEffect(
                JujutsucraftModMobEffects.SUKUNA_EFFECT.get());
        if (sukuna != null) {
            sukunaLevel = Math.min(sukuna.getAmplifier() + 1, 20);
            if (sukunaLevel <= 10.0D) {
                level = Math.max(level,
                        15.0D + Math.round(Math.floor(sukunaLevel * 0.5D)));
            } else if (sukunaLevel <= 19.0D) {
                level = Math.max(level,
                        10.0D + Math.round(Math.floor(sukunaLevel)));
            } else {
                level = Math.max(level, 35.0D);
            }
        }

        if (vars.PlayerCursePowerFormer == 0.0D) {
            level = Math.max(level + 6.0D, level * 1.3D);
        } else if (vars.PlayerCursePowerFormer < 100.0D) {
            level = Math.max(level + 3.0D, level * 1.2D);
        }

        if (level <= 0.0D) return 0.0D;
        level = Math.max(level - 1.0D, 0.0D);

        double amplifier = Math.round(level >= 19.0D
                ? 9.0D : Math.min(level / 3.0D, 4.0D));
        if (sukunaLevel > 0.0D) {
            amplifier = Math.round(Math.max(amplifier, 14.0D));
        }

        // Preserve Abyss's Special Grade Gojo exception: 40 hearts total.
        // Use the actual JC advancement, NOT a separate stored Abyss grade.
        if (ct1 == 2.0D && hasSpecialGrade(player)) {
            return 60.0D; // 20 vanilla + 60 modifier = 80 HP = 40 hearts
        }

        // Equivalent to JC Health Boost's +4 max HP per amplifier level.
        return 4.0D * (amplifier + 1.0D);
    }

    private static boolean hasSpecialGrade(ServerPlayer player) {
        Advancement adv = player.server.getAdvancements().getAdvancement(
                new ResourceLocation("jujutsucraft", "sorcerer_grade_special"));
        return adv != null && player.getAdvancements()
                .getOrStartProgress(adv).isDone();
    }
}
