
package com.ghosts.abyss.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class BloodPoisoningEffect extends MobEffect {

    public BloodPoisoningEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B1020);
    }

    @Override
    public boolean isDurationEffectTick(
            int duration,
            int amplifier
    ) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(
            LivingEntity entity,
            int amplifier
    ) {
        if (entity.level().isClientSide) {
            return;
        }

        float damage = entity.getMaxHealth() * 0.05f;

        entity.hurt(
                new net.minecraft.world.damagesource.DamageSource(
                        entity.level().registryAccess()
                                .registryOrThrow(
                                        net.minecraft.core.registries.Registries.DAMAGE_TYPE
                                )
                                .getHolderOrThrow(
                                        net.minecraft.resources.ResourceKey.create(
                                                net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                                                new net.minecraft.resources.ResourceLocation(
                                                        "abyss",
                                                        "blood_poisoning"
                                                )
                                        )
                                )
                ),
                damage
        );
    }
}
