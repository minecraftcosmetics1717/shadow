package com.ghosts.abyss.mixin;

import net.mcreator.jujutsucraft.potion.MythicalBeastAmberEffectMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MythicalBeastAmberEffectMobEffect.class)
public abstract class MixinMythicalBeastAmber extends MobEffect {
   public MixinMythicalBeastAmber() {
      super(MobEffectCategory.NEUTRAL, -65281);
   }

   @Dynamic("Bypass third-party mod method resolution")
   @SuppressWarnings("UnresolvedMixinReference")
   @Inject(
           method = "removeAttributeModifiers(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/ai/attributes/AttributeMap;I)V",
           at = @At("HEAD"),
           cancellable = true
   )
   public void onRemoveAttributeModifiers(LivingEntity livingEntity, AttributeMap attributeMap, int i, CallbackInfo ci) {
      super.removeAttributeModifiers(livingEntity, attributeMap, i);

      if (!livingEntity.level().isClientSide()) {
         livingEntity.setHealth(4.0F);

         // Reset generic attack strength attribute back to default base value (1.0)
         AttributeInstance attackDamage = livingEntity.getAttribute(Attributes.ATTACK_DAMAGE);
         if (attackDamage != null) {
            attackDamage.setBaseValue(10.0D);
         }
      }

      ci.cancel();
   }
}