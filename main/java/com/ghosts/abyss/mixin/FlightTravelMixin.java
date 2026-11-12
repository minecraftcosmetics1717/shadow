
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.effect.ModEffects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class FlightTravelMixin {

    public static final double FLIGHT_SPEED = 0.65D;

    @Inject(
            method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$poweredFlight(
            Vec3 travelVector,
            CallbackInfo ci
    ) {
        Player player = (Player) (Object) this;

        if (!(player instanceof LocalPlayer)) return;
        if (!player.hasEffect(ModEffects.FLIGHT.get())) return;
        if (!player.isFallFlying()) return;

        Vec3 movement = Vec3.ZERO;

        if (player.isSprinting()) {
            movement = player.getLookAngle()
                    .normalize()
                    .scale(FLIGHT_SPEED);
        }

        player.move(MoverType.SELF, movement);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;

        ci.cancel();
    }
}
