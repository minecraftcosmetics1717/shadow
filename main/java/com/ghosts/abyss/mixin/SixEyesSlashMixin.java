package com.ghosts.abyss.mixin;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.CanSeeSukunaSlashProcedure;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CanSeeSukunaSlashProcedure.class)
public class SixEyesSlashMixin {

    @Inject(
            method = "execute",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void checkSixEyes(LevelAccessor world, Entity entity, Entity player, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        if (player instanceof Player) {
            JujutsucraftModVariables.PlayerVariables variables = player.getCapability(
                    JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY,
                    (Direction) null
            ).orElse(new JujutsucraftModVariables.PlayerVariables());
            if (variables.FlagSixEyes) {
                cir.setReturnValue(true);
            }
        }
    }
}