
package com.ghosts.abyss.mixin;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.StartCursedTechniqueProcedure",
        remap = false
)
public class DomainActivationCostMixin {

    private static final String SAVED_COST =
            "abyss_saved_domain_start_cost";

    // JC recalculates the technique's cost inside
    // KeyChangeTechniqueOnKeyPressedProcedure.
    //
    // We must change the cost AFTER that call,
    // but BEFORE StartCursedTechnique deducts CE.
    @Inject(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/mcreator/jujutsucraft/procedures/KeyChangeTechniqueOnKeyPressedProcedure;execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            remap = false,
            require = 1
    )
    private static void abyss$beforeDeduction(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (!(entity instanceof Player player)) return;

        player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).ifPresent(vars -> {

            // Slot 20 = Domain Expansion.
            if (vars.PlayerSelectCurseTechnique != 20.0D) {
                return;
            }

            double technique = vars.SecondTechnique
                    ? vars.PlayerCurseTechnique2
                    : vars.PlayerCurseTechnique;

            int id = (int) technique;

            boolean abyssDomain = switch (id) {
                case 1, 2, 4, 5, 6, 8, 9, 10,
                     12, 15, 19, 24, 29, 38, 39 -> true;
                default -> false;
            };

            if (!abyssDomain) return;

            var data = player.getPersistentData();

            // Remember the normal selected cost.
            data.putDouble(
                    SAVED_COST,
                    vars.PlayerSelectCurseTechniqueCost
            );

            // JC will now deduct zero when starting
            // the domain's long-press animation.
            vars.PlayerSelectCurseTechniqueCost = 0.0D;
        });
    }

    // Restore the selected technique's normal cost
    // once JC has finished starting the technique.
    @Inject(
            method = "execute",
            at = @At("RETURN"),
            remap = false
    )
    private static void abyss$afterStart(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (!(entity instanceof Player player)) return;

        var data = player.getPersistentData();

        if (!data.contains(SAVED_COST)) return;

        double originalCost = data.getDouble(SAVED_COST);

        player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).ifPresent(vars ->
                vars.PlayerSelectCurseTechniqueCost = originalCost
        );

        data.remove(SAVED_COST);
    }
}
