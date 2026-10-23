package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainDimBlockGuard;
import com.ghosts.abyss.domain.DomainDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class DomainDimSetBlockMixin {

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$guardDomainDim(
            BlockPos pos, BlockState state, int flags, int recursionLeft,
            CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level) (Object) this;
        if (!(self instanceof ServerLevel serverLevel)) return;
        if (!serverLevel.dimension().equals(DomainDimension.ID)) return;
        if (DomainDimBlockGuard.isOpen()) return;

        cir.setReturnValue(false);
    }
}