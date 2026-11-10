package com.ghosts.abyss.mixin;

import com.ghosts.abyss.blackflash.BlackFlashTiming;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.TechniqueBluePunchProcedure", remap = false)
public class GojoBluePunchBlackFlashMixin {
    @Inject(method = "execute", at = @At("HEAD"), remap = false)
    private static void abyss$blueHold(LevelAccessor world, Entity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayer && !world.isClientSide()) {
            BlackFlashTiming.blueHolding(entity);
        }
    }

    @Inject(method = "execute", at = @At("TAIL"), remap = false)
    private static void abyss$blueRelease(LevelAccessor world, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof ServerPlayer player) || world.isClientSide()) return;
        // Confirmed transition in Jujutsucraft's Blue Punch source.
        if (player.getPersistentData().getDouble("skill") == 101.0D
                && player.getPersistentData().getDouble("cnt1") == 8.0D) {
            BlackFlashTiming.blueFinisher(player);
        }
    }
}