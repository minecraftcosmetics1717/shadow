package com.ghosts.abyss.mixin;

import com.ghosts.abyss.effect.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keep the real Elytra-sized hitbox without requiring an equipped Elytra.
 * Ground contact is fine inside a low tunnel; restore vanilla handling when
 * standing room exists so the player doesn't get stuck gliding on the floor.
 */
@Mixin(LivingEntity.class)
public abstract class FlightElytraMixin {

    @Shadow protected int fallFlyTicks;

    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void abyss$maintainFlightWithoutElytra(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player)) return;
        if (!player.hasEffect(ModEffects.FLIGHT.get())) return;
        if (!player.isFallFlying()) return;

        if (!player.isAlive() || player.isPassenger()
                || player.isInWaterOrBubble() || player.isInLava()) {
            return; // Let vanilla end the state in invalid conditions.
        }

        if (player.onGround()) {
            AABB standingBox = player.getDimensions(Pose.STANDING)
                    .makeBoundingBox(player.position());
            if (player.level().noCollision(player, standingBox)) {
                return; // On open ground, regular standing is safe.
            }
        }

        this.fallFlyTicks++;
        ci.cancel();
    }
}
