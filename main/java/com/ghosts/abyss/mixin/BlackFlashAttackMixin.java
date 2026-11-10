
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.blackflash.BlackFlashTiming;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class BlackFlashAttackMixin {

    @Pseudo
    @Mixin(
            targets = "net.mcreator.jujutsucraft.procedures.AttackWeakPunchProcedure",
            remap = false
    )
    public static class Weak {

        @Inject(method = "execute", at = @At("HEAD"), remap = false)
        private static void abyss$start(
                LevelAccessor world,
                double x, double y, double z,
                Entity entity,
                CallbackInfo ci
        ) {
            if (entity != null && !world.isClientSide()) {
                BlackFlashTiming.beginAttack(entity);
            }
        }
    }

    @Pseudo
    @Mixin(
            targets = "net.mcreator.jujutsucraft.procedures.AttackStrongPunchProcedure",
            remap = false
    )
    public static class Strong {

        @Inject(method = "execute", at = @At("HEAD"), remap = false)
        private static void abyss$start(
                LevelAccessor world,
                double x, double y, double z,
                Entity entity,
                CallbackInfo ci
        ) {
            if (entity != null && !world.isClientSide()) {
                BlackFlashTiming.beginAttack(entity);
            }
        }
    }

    @Pseudo
    @Mixin(
            targets = "net.mcreator.jujutsucraft.procedures.AttackWeakSwordProcedure",
            remap = false
    )
    public static class WeakSword {

        @Inject(method = "execute", at = @At("HEAD"), remap = false)
        private static void abyss$start(
                LevelAccessor world,
                double x, double y, double z,
                Entity entity,
                CallbackInfo ci
        ) {
            if (entity != null && !world.isClientSide()) {
                BlackFlashTiming.beginAttack(entity);
            }
        }
    }

    @Pseudo
    @Mixin(
            targets = "net.mcreator.jujutsucraft.procedures.AttackStrongSwordProcedure",
            remap = false
    )
    public static class StrongSword {

        @Inject(method = "execute", at = @At("HEAD"), remap = false)
        private static void abyss$start(
                LevelAccessor world,
                double x, double y, double z,
                Entity entity,
                CallbackInfo ci
        ) {
            if (entity != null && !world.isClientSide()) {
                BlackFlashTiming.beginAttack(entity);
            }
        }
    }

}
