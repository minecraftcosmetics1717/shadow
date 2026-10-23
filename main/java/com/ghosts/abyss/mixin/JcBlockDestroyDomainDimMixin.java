package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.BlockDestroyAllDirectionProcedure",
        remap = false
)
public class JcBlockDestroyDomainDimMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$noDestroyInDomainDim(
            LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity != null && entity.level() != null
                && entity.level().dimension().equals(DomainDimension.ID)) {
            ci.cancel();
            return;
        }
        if (world instanceof ServerLevel sl && sl.dimension().equals(DomainDimension.ID)) {
            ci.cancel();
            return;
        }
        if (world instanceof Level level && level.dimension().equals(DomainDimension.ID)) {
            ci.cancel();
        }
    }
}