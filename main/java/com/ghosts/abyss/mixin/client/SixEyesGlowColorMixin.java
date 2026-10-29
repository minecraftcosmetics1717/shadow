
package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.client.sixeyes.SixEyesGlowRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class SixEyesGlowColorMixin {

    @Inject(
            method = "getTeamColor",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$sixEyesGlowColor(
            CallbackInfoReturnable<Integer> cir
    ) {
        Entity entity = (Entity) (Object) this;

        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (!SixEyesGlowRules.shouldGlow(entity)) {
            return;
        }

        cir.setReturnValue(
                SixEyesGlowRules.getGlowColor(living)
        );
    }
}
