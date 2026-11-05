
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
        targets = "net.mcreator.jujutsucraft.procedures.BulletBallCanUseRangedItemProcedure",
        remap = false
)
public class HakariReserveBallCooldownMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$allowReserveBallInDomain(
            Entity entity,
            ItemStack itemstack,
            CallbackInfo ci
    ) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (entity.level().isClientSide) {
            return;
        }

        // Only bypass JC's automatic cooldown for
        // the owner of an active, uncontested Hakari domain.
        if (HakariGambleManager.isHakariOwner(living)) {
            ci.cancel();
        }
    }
}
