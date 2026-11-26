package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.HakariDomainCards;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.AISLOTProcedure", remap = false)
public class HakariCardAiMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$visualOnly(LevelAccessor world, Entity entity, CallbackInfo ci) {
        if (HakariDomainCards.isCard(entity)) ci.cancel();
    }
}
