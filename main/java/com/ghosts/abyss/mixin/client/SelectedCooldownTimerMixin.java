package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.client.AbyssCooldownNumbersOverlay;
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

@Mixin(value = OCoolTimeSelectingProcedure.class, remap = false)
public class SelectedCooldownTimerMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$individualCooldownTimer(Entity entity,
                                                      CallbackInfoReturnable<String> cir) {
        if (!(entity instanceof Player player) || !player.level().isClientSide()) return;

        String copiedNumber = AbyssCooldownNumbersOverlay.selectedYutaText(entity);
        if (copiedNumber != null) {
            cir.setReturnValue(copiedNumber);
            return;
        }

        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
        if (vars == null) return;

        double technique = vars.SecondTechnique
                ? vars.PlayerCurseTechnique2 : vars.PlayerCurseTechnique;
        int skillId = (int) Math.round(
                technique * 100.0D + vars.PlayerSelectCurseTechnique);
        if (!TechniqueCooldownManager.isManaged(skillId)) return;

        // Ordinary managed skills share the same interpolated display.
        // The original bottom-left countdown is supplied by textFor().
        String text = AbyssCooldownNumbersOverlay.textFor(entity, vars.PhysicalAttack);
        if (text != null && !text.isEmpty()) {
            cir.setReturnValue(text);
        }
    }
}
