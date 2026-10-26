package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainDimBlockGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.DomainExpansionBattleProcedure",
        remap = false
)
public class JcDomainBattleAllowMixin {

    @Inject(method = "execute", at = @At("HEAD"), remap = false)
    private static void abyss$openGuard(CallbackInfo ci) {
        DomainDimBlockGuard.open();
    }

    @Inject(method = "execute", at = @At("RETURN"), remap = false)
    private static void abyss$closeGuard(CallbackInfo ci) {
        DomainDimBlockGuard.close();
    }
}