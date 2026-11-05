
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.HakariGambleManager;
import net.mcreator.jujutsucraft.entity.BulletBallProjectileEntity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = BulletBallProjectileEntity.class, remap = false)
public class HakariBallThrowMixin {

    @Inject(
            method = "shoot(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/util/RandomSource;)Lnet/mcreator/jujutsucraft/entity/BulletBallProjectileEntity;",
            at = @At("RETURN"),
            remap = false
    )
    private static void abyss$onReserveBallThrown(
            Level world,
            LivingEntity entity,
            RandomSource random,
            CallbackInfoReturnable<BulletBallProjectileEntity> cir
    ) {
        // Server-side only.
        if (world.isClientSide) return;

        // Only register a throw when a projectile was created.
        BulletBallProjectileEntity projectile = cir.getReturnValue();
        if (projectile == null || projectile.isRemoved()) return;

        // HakariGambleManager checks that the player
        // owns an active, uncontested Hakari domain.
        HakariGambleManager.usedBall(entity);
    }
}
