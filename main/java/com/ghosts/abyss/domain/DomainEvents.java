package com.ghosts.abyss.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
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
        if (entity.getPersistentData().getBoolean("abyss_domain_handled")) return;
        if (entity.getPersistentData().getBoolean("abyss_domain_transferring")) return;

        entity.getPersistentData().putBoolean("abyss_domain_pending", true);
        entity.getPersistentData().putInt("abyss_domain_pending_tick", 3);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity.getPersistentData().getBoolean("abyss_domain_pending")
                && !entity.getPersistentData().getBoolean("abyss_domain_handled")) {

            int wait = entity.getPersistentData().getInt("abyss_domain_pending_tick");
            if (wait > 0) {
                entity.getPersistentData().putInt("abyss_domain_pending_tick", wait - 1);
                return;
            }

            MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(DOMAIN_EFFECT_ID);
            if (domainEffect == null || !entity.hasEffect(domainEffect)) {
                entity.getPersistentData().putBoolean("abyss_domain_pending", false);
                return;
            }

            var data = entity.getPersistentData();
            double ox = data.getDouble("x_pos_doma");
            double oy = data.getDouble("y_pos_doma");
            double oz = data.getDouble("z_pos_doma");
            boolean hasCenter = ox != 0 || oy != 0 || oz != 0;

            if (hasCenter) {
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

                // Keep amp >= 1 every tick so JC's ~22-block leave check does not fire
                MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(DOMAIN_EFFECT_ID);
                if (domainEffect != null
                        && !entity.getPersistentData().getBoolean("abyss_domain_transferring")) {
                    MobEffectInstance cur = entity.getEffect(domainEffect);
                    if (cur == null) {
                        entity.getPersistentData().putBoolean("abyss_domain_handled", true);
                        entity.addEffect(new MobEffectInstance(
                                domainEffect, 20 * 60 * 5, 1, false, false, true));
                    } else if (cur.getAmplifier() < 1) {
                        entity.getPersistentData().putBoolean("abyss_domain_handled", true);
                        entity.addEffect(new MobEffectInstance(
                                domainEffect, cur.getDuration(), 1, false, false, true));
                    }
                }
            } else {
                // Only our arena edge (r=63) should end the domain
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

    /** Owner dies in domain dim (or anywhere) → end domain + overworld barrier */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity entity = event.getEntity();
        DomainInstance inst = DomainInstanceManager.getByOwner(entity.getUUID());
        if (inst == null) return;

        // end() restores people, clears arena, triggers JC break
        entity.getPersistentData().putBoolean("abyss_domain_transferring", false);
        DomainLifecycle.end(entity);
    }

    /** Drops from a domain owner → spawn at overworld domain origin instead of domain dim */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity entity = event.getEntity();
        DomainInstance inst = DomainInstanceManager.getByOwner(entity.getUUID());
        // Owner may already be released by onDeath — check persistent flag or still-in-dim
        if (inst == null) {
            // Fallback: if they died in domain dim and had skill_domain data, still redirect
            if (!entity.level().dimension().equals(DomainDimension.ID)) return;
            // Try find instance by scanning slots for matching return entry
            inst = findInstanceContaining(entity.getUUID());
            if (inst == null) return;
        }

        net.minecraft.server.MinecraftServer server = entity.level().getServer();
        if (server == null) return;
        ServerLevel overworld = server.getLevel(inst.returnDim);
        if (overworld == null) overworld = server.overworld();

        Vec3 dropAt = inst.overworldCenter;

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem().copy();
            ItemEntity overDrop = new ItemEntity(
                    overworld, dropAt.x, dropAt.y + 0.5, dropAt.z, stack);
            overDrop.setDefaultPickUpDelay();
            overworld.addFreshEntity(overDrop);
        }
        event.getDrops().clear();
    }

    private static DomainInstance findInstanceContaining(java.util.UUID id) {
        for (int i = 0; i < DomainDimension.MAX_SLOTS; i++) {
            DomainInstance inst = DomainInstanceManager.getBySlot(i);
            if (inst != null && (id.equals(inst.ownerId) || inst.returnPositions.containsKey(id))) {
                return inst;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEffectInstance() == null) return;
        if (!isDomainEffect(event.getEffectInstance().getEffect())) return;

        LivingEntity entity = event.getEntity();
        if (entity.getPersistentData().getBoolean("abyss_domain_transferring")) return;
        if (!entity.isAlive()) {
            // Death handled by onDeath — avoid double end
            return;
        }

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
        if (entity.getPersistentData().getBoolean("abyss_domain_transferring")) return;
        if (!entity.isAlive()) return;

        entity.getPersistentData().putBoolean("abyss_domain_handled", false);
        entity.getPersistentData().putBoolean("abyss_domain_pending", false);
        DomainLifecycle.end(entity);
    }
}