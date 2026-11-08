
package com.ghosts.abyss.mixin;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.StartCursedTechniqueProcedure",
        remap = false
)
public class PiercingBloodCostMixin {

    @Redirect(
            method = "execute",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/mcreator/jujutsucraft/network/JujutsucraftModVariables$PlayerVariables;PlayerSelectCurseTechniqueCost:D",
                    opcode = 180
            ),
            remap = false
    )
    private static double abyss$replaceInitialCost(
            JujutsucraftModVariables.PlayerVariables vars,
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity
    ) {
        double technique = vars.SecondTechnique
                ? vars.PlayerCurseTechnique2
                : vars.PlayerCurseTechnique;

        double skill = technique * 100.0D
                + vars.PlayerSelectCurseTechnique;

        if ((int) skill == 1007) {
            return 0.0D;
        }

        return vars.PlayerSelectCurseTechniqueCost;
    }
}
