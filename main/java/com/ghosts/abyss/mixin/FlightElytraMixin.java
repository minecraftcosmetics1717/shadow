
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.effect.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class FlightElytraMixin {

    @Shadow
    protected int fallFlyTicks;

    @Inject(
            method = "updateFallFlying",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$maintainFlightWithoutElytra(CallbackInfo ci) {

        LivingEntity entity = (LivingEntity) (Object) this;

        if (!(entity instanceof Player player)) return;

        if (!player.hasEffect(ModEffects.FLIGHT.get())) return;
        if (!player.isFallFlying()) return;

        // Maintain actual fall-flying and its animation timer
        // without requiring an Elytra in the chest slot.
        this.fallFlyTicks++;

        ci.cancel();
    }
}
