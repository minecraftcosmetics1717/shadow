
package com.ghosts.abyss.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class DomainCooldown {

    private static final ResourceLocation BRAIN_DAMAGE =
            new ResourceLocation("jujutsucraft", "brain_damage");

    private static final ResourceLocation UNSTABLE =
            new ResourceLocation("jujutsucraft", "unstable");

    public static final int DURATION = 20 * 30;

    private DomainCooldown() {}

    public static boolean cannotExpand(LivingEntity entity) {
        if (entity == null) return true;

        MobEffect brainDamage =
                ForgeRegistries.MOB_EFFECTS.getValue(BRAIN_DAMAGE);

        MobEffect unstable =
                ForgeRegistries.MOB_EFFECTS.getValue(UNSTABLE);

        return (brainDamage != null && entity.hasEffect(brainDamage))
                || (unstable != null && entity.hasEffect(unstable));
    }

    public static void apply(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) return;
        if (!entity.isAlive()) return;

        MobEffect unstable =
                ForgeRegistries.MOB_EFFECTS.getValue(UNSTABLE);

        if (unstable == null) return;

        // Avoid restarting an existing cooldown.
        if (entity.hasEffect(unstable)) return;

        entity.addEffect(new MobEffectInstance(
                unstable,
                DURATION,
                0,
                false,
                true
        ));
    }
}
