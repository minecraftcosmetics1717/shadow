
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.HakariGambleManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.BulletBallRangedItemUsedProcedure",
        remap = false
)
public class HakariBallThrowMixin {

    @Inject(
            method = "execute",
            at = @At("RETURN"),
            remap = false
    )
    private static void abyss$onReserveBallThrown(
            Entity entity,
            ItemStack itemstack,
            CallbackInfo ci
    ) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        // Only run on the server.
        if (entity.level().isClientSide) {
            return;
        }

        // The original procedure has finished spawning
        // the Reserve Ball projectile.
        //
        // The manager verifies that this entity owns
        // an active, uncontested Hakari domain.
        HakariGambleManager.usedBall(living);
    }
}
