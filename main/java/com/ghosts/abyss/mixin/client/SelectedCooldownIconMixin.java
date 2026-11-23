
package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.technique.TechniqueCooldownManager;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.OverlayCooldown2Procedure;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        value = OverlayCooldown2Procedure.class,
        remap = false
)
public class SelectedCooldownIconMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$individualCooldownIcon(
            Entity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(entity instanceof Player player)) {
            return;
        }

        if (!player.level().isClientSide()) {
            return;
        }

        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);

        if (vars == null) {
            return;
        }

        double technique = vars.SecondTechnique
                ? vars.PlayerCurseTechnique2
                : vars.PlayerCurseTechnique;

        int skillId = (int) Math.round(
                technique * 100.0D
                        + vars.PlayerSelectCurseTechnique
        );

        if (!TechniqueCooldownManager.isManaged(skillId)) {
            return;
        }

        int remaining = player.getPersistentData().getInt(
                "abyss_ui_cd_" + skillId
        );

        if (remaining > 0) {
            // Tell JC to draw its existing cooldown icon.
            cir.setReturnValue(true);
        }

        // Otherwise, retain JC's original HUD checks.
    }
}
