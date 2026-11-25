package com.ghosts.abyss.mixin;

import com.ghosts.abyss.mahito.MahitoForms;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Turn each toggle on/off once per Z/ability-button press, not every effect tick. */
@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyPressedProcedure", remap = false)
public final class MahitoToggleKeyMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$onToggleKey(LevelAccessor world, double x, double y, double z,
                                          Entity entity, CallbackInfo ci) {
        if (!(entity instanceof ServerPlayer player) || !MahitoForms.isMahito(player)) return;
        var vars = player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
        if (vars == null) return;
        int slot = (int) Math.round(vars.PlayerSelectCurseTechnique);
        if (slot != MahitoForms.BLADES && slot != MahitoForms.DIMINUTION
                && slot != MahitoForms.TRUE_FORM) return;
        MahitoForms.toggle(player, slot);
        ci.cancel();
    }
}
