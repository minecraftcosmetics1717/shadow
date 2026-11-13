
package com.ghosts.abyss.grade;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.server.level.ServerPlayer;

public final class GradeHealthCalculator {

    private GradeHealthCalculator() {}

    public static double calculateBonus(ServerPlayer player) {

        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);

        if (vars == null) return 0.0;

        double level = vars.PlayerLevel;
        double ct1 = vars.PlayerCurseTechnique;
        double ct2 = vars.PlayerCurseTechnique2;
        double sukunaLevel = 0.0;

        // Match JC's PlayerCT1 != 0 condition.
        if (ct1 == 0.0) return 0.0;

        boolean yuji = ct1 == 21.0 || ct2 == 21.0;

        if (yuji) {
            level = Math.max(level + 3.0, level * 1.2);
        }

        var sukuna = player.getEffect(
                JujutsucraftModMobEffects.SUKUNA_EFFECT.get()
        );

        if (sukuna != null) {
            sukunaLevel = Math.min(sukuna.getAmplifier() + 1, 20);

            if (sukunaLevel <= 10.0) {
                level = Math.max(
                        level,
                        15.0 + Math.round(Math.floor(sukunaLevel * 0.5))
                );
            } else if (sukunaLevel <= 19.0) {
                level = Math.max(
                        level,
                        10.0 + Math.round(Math.floor(sukunaLevel))
                );
            } else {
                level = Math.max(level, 35.0);
            }
        }

        if (vars.PlayerCursePowerFormer == 0.0) {
            level = Math.max(level + 6.0, level * 1.3);
        } else if (vars.PlayerCursePowerFormer < 100.0) {
            level = Math.max(level + 3.0, level * 1.2);
        }

        if (level <= 0.0) return 0.0;

        level = Math.max(level - 1.0, 0.0);

        double amplifier = Math.round(
                level >= 19.0
                        ? 9.0
                        : Math.min(level / 3.0, 4.0)
        );

        if (sukunaLevel > 0.0) {
            amplifier = Math.round(Math.max(amplifier, 14.0));
        }

        // Vanilla Health Boost adds 4 HP per effect level.
        return 4.0 * (amplifier + 1.0);
    }
}
