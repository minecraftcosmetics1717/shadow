
package com.ghosts.abyss.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class BlockJujutsuFlightMixin {

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$blockFlightWithoutSource(
            MobEffectInstance effect,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (abyss$shouldBlock(effect)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$blockFlightWithSource(
            MobEffectInstance effect,
            Entity source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (abyss$shouldBlock(effect)) {
            cir.setReturnValue(false);
        }
    }

    private boolean abyss$shouldBlock(MobEffectInstance effect) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!(entity instanceof Player)) {
            return false;
        }

        if (effect == null) {
            return false;
        }

        MobEffect originalFlight = ForgeRegistries.MOB_EFFECTS.getValue(
                new ResourceLocation("jujutsucraft", "fly_effect")
        );

        return originalFlight != null
                && effect.getEffect() == originalFlight;
    }
}
