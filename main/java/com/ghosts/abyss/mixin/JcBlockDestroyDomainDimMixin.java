package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents JC block destruction (shrine dig, barrier holes, etc.) inside abyss:domain.
 * Entity damage / sure-hit procedures are unaffected.
 */
@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.BlockDestroyAllDirectionProcedure",
        remap = false
)
public class JcBlockDestroyDomainDimMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$noDestroyInDomainDim(
            LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (isAbyssDomain(world)) {
            ci.cancel();
        }
    }

    private static boolean isAbyssDomain(LevelAccessor world) {
        if (world instanceof ServerLevel level) {
            return level.dimension().equals(DomainDimension.ID);
        }
        if (world instanceof Level level) {
            return level.dimension().equals(DomainDimension.ID);
        }
        return false;
    }
}