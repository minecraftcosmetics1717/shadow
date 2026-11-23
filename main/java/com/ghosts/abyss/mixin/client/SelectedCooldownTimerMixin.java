
package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.technique.TechniqueCooldownManager;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.OCoolTimeSelectingProcedure;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        value = OCoolTimeSelectingProcedure.class,
        remap = false
)
public class SelectedCooldownTimerMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$individualCooldownTimer(
            Entity entity,
            CallbackInfoReturnable<String> cir
    ) {
        if (!(entity instanceof Player player)
                || !player.level().isClientSide()) {
            return;
        }

        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);

        if (vars == null) return;

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

        int remaining = player.getPersistentData()
                .getInt("abyss_ui_cd_" + skillId);

        if (remaining <= 0) {
            // Let JC handle its normal HUD behavior.
            return;
        }

        // If Unstable is also active, display the longer
        // restriction for nonphysical techniques.
        if (!vars.PhysicalAttack) {
            var unstable = player.getEffect(
                    JujutsucraftModMobEffects.UNSTABLE.get()
            );

            if (unstable != null) {
                remaining = Math.max(
                        remaining,
                        unstable.getDuration()
                );
            }
        }

        // Match Jujutsucraft's original display formatting.
        long displayed = (long) Math.ceil(
                remaining * 0.1D
        );

        cir.setReturnValue(Long.toString(displayed));
    }
}
