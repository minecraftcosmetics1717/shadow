package com.ghosts.abyss.mixin;

import com.ghosts.abyss.client.FlightModeClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client-predicted constant-speed prone flight, with a shared boost curve. */
@Mixin(Player.class)
public abstract class FlightTravelMixin {

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"), cancellable = true)
    private void abyss$poweredFlight(Vec3 travelVector, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!(player instanceof LocalPlayer local)) return;
        if (!FlightModeClient.isPowered(local)) return;

        Vec3 motion = Vec3.ZERO;
        if (local.isSprinting() && local.input.forwardImpulse > 0.0F) {
            motion = local.getLookAngle().normalize()
                    .scale(FlightModeClient.currentSpeed(local));
        }

        local.move(MoverType.SELF, motion);
        local.setDeltaMovement(Vec3.ZERO);
        local.fallDistance = 0.0F;
        ci.cancel();
    }
}
