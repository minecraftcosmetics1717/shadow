package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainDimension;
import com.ghosts.abyss.domain.DomainInstanceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.IdleDeathGambleActiveProcedure", remap = false)
public class HakariNativeGambleMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$replaceNativeRoll(LevelAccessor world, Entity entity, CallbackInfo ci) {
        if (entity == null || !entity.level().dimension().equals(DomainDimension.ID)) return;
        var inst = DomainInstanceManager.getByOwner(entity.getUUID());
        if (inst != null && inst.skillDomain == 29.0D) ci.cancel();
    }
}
