package com.ghosts.abyss.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "abyss")
public class DomainEvents {

    private static final ResourceLocation DOMAIN_EFFECT_ID =
            new ResourceLocation("jujutsucraft", "domain_expansion");

    private static boolean isDomainEffect(MobEffect effect) {
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return DOMAIN_EFFECT_ID.equals(id);
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEffectInstance() == null) return;
        if (!isDomainEffect(event.getEffectInstance().getEffect())) return;

        LivingEntity entity = event.getEntity();

        // Ignore amplifier refreshes / re-applies from our own code
        if (entity.getPersistentData().getBoolean("abyss_domain_handled")) return;

        entity.getPersistentData().putBoolean("abyss_domain_pending", true);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity.getPersistentData().getBoolean("abyss_domain_pending")
                && !entity.getPersistentData().getBoolean("abyss_domain_handled")) {
            var data = entity.getPersistentData();
            boolean ready = data.contains("x_pos_doma") || data.getDouble("skill_domain") != 0;
            if (ready) {
                entity.getPersistentData().putBoolean("abyss_domain_pending", false);
                DomainLifecycle.tryStart(entity);
            }
        }

        if (entity.level().dimension().equals(DomainDimension.ID)) {
            DomainInstance inst = DomainInstanceManager.getByOwner(entity.getUUID());
            if (inst == null) return;

            double dx = entity.getX() - (inst.arenaCenter.getX() + 0.5);
            double dz = entity.getZ() - (inst.arenaCenter.getZ() + 0.5);
            double distSq = dx * dx + dz * dz;
            double maxR = DomainDimension.INNER_RADIUS;

            if (distSq <= maxR * maxR) {
                var data = entity.getPersistentData();
                data.putDouble("x_pos_doma", inst.arenaCenter.getX() + 0.5);
                data.putDouble("y_pos_doma", inst.arenaCenter.getY());
                data.putDouble("z_pos_doma", inst.arenaCenter.getZ() + 0.5);

                MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(DOMAIN_EFFECT_ID);
                if (domainEffect != null) {
                    MobEffectInstance cur = entity.getEffect(domainEffect);
                    // Only bump amp if needed — does not clear handled
                    if (cur != null && cur.getAmplifier() < 1) {
                        entity.getPersistentData().putBoolean("abyss_domain_handled", true);
                        entity.addEffect(new MobEffectInstance(
                                domainEffect, cur.getDuration(), 1, false, false, true));
                    }
                }
            } else {
                entity.getPersistentData().putDouble("x_pos_doma", inst.overworldCenter.x);
                entity.getPersistentData().putDouble("y_pos_doma", inst.overworldCenter.y);
                entity.getPersistentData().putDouble("z_pos_doma", inst.overworldCenter.z);

                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(DOMAIN_EFFECT_ID);
                if (effect != null) {
                    entity.removeEffect(effect);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEffectInstance() == null) return;
        if (!isDomainEffect(event.getEffectInstance().getEffect())) return;

        LivingEntity entity = event.getEntity();
        entity.getPersistentData().putBoolean("abyss_domain_handled", false);
        entity.getPersistentData().putBoolean("abyss_domain_pending", false);
        DomainLifecycle.end(entity);
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEffectInstance() == null) return;
        if (!isDomainEffect(event.getEffectInstance().getEffect())) return;

        LivingEntity entity = event.getEntity();
        entity.getPersistentData().putBoolean("abyss_domain_handled", false);
        entity.getPersistentData().putBoolean("abyss_domain_pending", false);
        DomainLifecycle.end(entity);
    }
}