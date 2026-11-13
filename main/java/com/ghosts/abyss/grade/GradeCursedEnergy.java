
package com.ghosts.abyss.grade;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.server.level.ServerPlayer;

public final class GradeCursedEnergy {

    private GradeCursedEnergy() {}

    public static void update(ServerPlayer player) {

        player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).ifPresent(vars -> {

            double newMax = vars.PlayerCursePowerFormer
                    * Math.round((2.0 + vars.PlayerLevel) / 1.1);

            var sukuna = player.getEffect(
                    JujutsucraftModMobEffects.SUKUNA_EFFECT.get()
            );

            if (sukuna != null) {
                int sukunaLevel = Math.min(
                        sukuna.getAmplifier() + 1, 20
                );

                double minimum = sukunaLevel <= 9
                        ? 3000.0 + (sukunaLevel - 1) * 300.0
                        : 8000.0 + (sukunaLevel - 10) * 1000.0;

                newMax = Math.max(newMax, minimum);
            }

            newMax = Math.max(0.0, newMax);

            boolean changed = vars.PlayerCursePowerMAX != newMax;

            if (changed) {
                vars.PlayerCursePowerMAX = newMax;
            }

            // Do not automatically refill cursed energy.
            // Only clamp it when the new limit is lower.
            if (vars.PlayerCursePower > newMax) {
                vars.PlayerCursePower = newMax;
                changed = true;
            }

            if (changed) {
                vars.syncPlayerVariables(player);
            }
        });
    }
}
