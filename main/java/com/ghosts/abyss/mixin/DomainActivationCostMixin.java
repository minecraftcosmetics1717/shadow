
package com.ghosts.abyss.mixin;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.StartCursedTechniqueProcedure",
        remap = false
)
public class DomainActivationCostMixin {

    @Redirect(
            method = "execute",
            at = @At(
                    value = "FIELD",
                    target =
                            "Lnet/mcreator/jujutsucraft/network/JujutsucraftModVariables$PlayerVariables;PlayerSelectCurseTechniqueCost:D",
                    opcode = 180,
                    remap = false
            ),
            remap = false,
            require = 1
    )
    private static double abyss$skipDomainUpfrontCost(
            JujutsucraftModVariables.PlayerVariables vars,
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity
    ) {
        // The original cost for all other techniques.
        double originalCost =
                vars.PlayerSelectCurseTechniqueCost;

        if (entity == null) {
            return originalCost;
        }

        // Domains use technique slot 20.
        if (vars.PlayerSelectCurseTechnique != 20.0D) {
            return originalCost;
        }

        double technique = vars.SecondTechnique
                ? vars.PlayerCurseTechnique2
                : vars.PlayerCurseTechnique;

        int id = (int) technique;

        // Only techniques currently handled by
        // AbyssDomainCast should bypass JC's upfront cost.
        boolean abyssDomain = switch (id) {
            case 1, 2, 4, 5, 6, 8, 9, 10,
                 12, 15, 19, 24, 29, 38, 39 -> true;

            default -> false;
        };

        if (!abyssDomain) {
            return originalCost;
        }

        // JC starts the skill without deducting CE.
        // Abyss charges 1250 after the hold completes.
        return 0.0D;
    }
}
