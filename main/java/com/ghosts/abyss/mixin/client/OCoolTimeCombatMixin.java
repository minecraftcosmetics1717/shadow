
package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.technique.TechniqueCooldownManager;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.OCoolTimeCombatProcedure;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = OCoolTimeCombatProcedure.class, remap = false)
public class OCoolTimeCombatMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$combatCooldown(
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

        if (vars == null || !vars.PhysicalAttack) {
            return;
        }

        double ct = vars.SecondTechnique
                ? vars.PlayerCurseTechnique2
                : vars.PlayerCurseTechnique;

        int skillId = (int) Math.round(
                ct * 100.0D + vars.PlayerSelectCurseTechnique
        );

        if (!TechniqueCooldownManager.isManaged(skillId)) {
            return;
        }

        int remaining = player.getPersistentData()
                .getInt("abyss_ui_cd_" + skillId);

        if (remaining > 0) {
            long displayed = (long) Math.ceil(remaining * 0.1D);
            cir.setReturnValue(Long.toString(displayed));
        }

        // Otherwise, use JC's original behavior.
    }
}
