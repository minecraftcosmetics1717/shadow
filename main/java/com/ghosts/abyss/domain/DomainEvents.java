package com.ghosts.abyss.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "abyss")
public class DomainEvents {

    public static final ResourceLocation DOMAIN_EFFECT_ID =
            new ResourceLocation("jujutsucraft", "domain_expansion");

    private static final int DEATH_END_DELAY = 80;
    public static final int DOMAIN_DURATION_TICKS = 20 * 160;

    private static boolean isDomainEffect(MobEffect effect) {
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return DOMAIN_EFFECT_ID.equals(id);
    }

    private static boolean isDyingOrDead(LivingEntity entity) {
        return !entity.isAlive()
                || entity.isDeadOrDying()
                || entity.getHealth() <= 0.0f
                || entity.getPersistentData().getBoolean("abyss_domain_owner_dead");
    }

    private static void scheduleEndAfterDeath(LivingEntity entity, DomainInstance inst) {
        MinecraftServer server = entity.level().getServer();
        if (server == null) return;

        final java.util.UUID ownerId = entity.getUUID();
        if (entity.getPersistentData().getBoolean("abyss_domain_end_scheduled")) return;
        entity.getPersistentData().putBoolean("abyss_domain_end_scheduled", true);

        server.tell(new TickTask(server.getTickCount() + DEATH_END_DELAY, () -> {
            DomainInstance still = DomainInstanceManager.getByOwner(ownerId);
            if (still == null) return;

            ServerLevel domainLevel = server.getLevel(DomainDimension.ID);
            ServerLevel over = server.getLevel(still.returnDim);
            if (over == null) over = server.overworld();

            LivingEntity ref = null;
            if (domainLevel != null) {
                Entity e = domainLevel.getEntity(ownerId);
                if (e instanceof LivingEntity living) ref = living;
            }
            if (ref == null && over != null) {
                Entity e = over.getEntity(ownerId);
                if (e instanceof LivingEntity living) ref = living;
            }
            if (ref != null) {
                ref.getPersistentData().putBoolean("abyss_domain_transferring", false);
                DomainLifecycle.end(ref);
            } else {
                DomainLifecycle.endByOwnerId(ownerId);
            }
        }));
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
// If no active clasher nearby, clear JC sure-hit suppression flags
        if (!entity.getPersistentData().getBoolean("abyss_domain_clashing")) {
            boolean otherDomain = false;
            MobEffect de = ForgeRegistries.MOB_EFFECTS.getValue(DOMAIN_EFFECT_ID);
            if (de != null && entity.level() instanceof ServerLevel sl) {
                DomainInstance inst2 = DomainInstanceManager.getByOwner(entity.getUUID());
                if (inst2 != null) {
                    LivingEntity other = DomainClash.findActiveDomainInArena(sl, inst2, entity.getUUID());
                    otherDomain = other != null;
                }
            }
            if (!otherDomain) {
                entity.getPersistentData().putBoolean("Failed", false);
                entity.getPersistentData().putBoolean("Cover", false);
                entity.getPersistentData().putBoolean("DomainDefeated", false);
            }
        }
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

            boolean inDim = entity.level().dimension().equals(DomainDimension.ID);
            if (inDim || ox != 0 || oy != 0 || oz != 0) {
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
                if (domainEffect != null
                        && !entity.getPersistentData().getBoolean("abyss_domain_transferring")) {
                    MobEffectInstance cur = entity.getEffect(domainEffect);
                    if (cur == null) {
                        entity.getPersistentData().putBoolean("abyss_domain_handled", true);
                        entity.addEffect(new MobEffectInstance(
                                domainEffect, DomainEvents.DOMAIN_DURATION_TICKS, 1, false, false, true));
                    } else if (cur.getAmplifier() < 1) {
                        // Keep remaining time; only fix amplifier
                        entity.getPersistentData().putBoolean("abyss_domain_handled", true);
                        entity.addEffect(new MobEffectInstance(
                                domainEffect, cur.getDuration(), 1, false, false, true));
                    }
// Do NOT refresh just because duration < DOMAIN_DURATION_TICKS
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
        if (entity instanceof ServerPlayer sp && entity.tickCount % 20 == 0) {
            DomainHudSync.tick(sp);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity entity = event.getEntity();
        DomainInstance inst = DomainInstanceManager.getByOwner(entity.getUUID());
        if (inst == null) return;

        entity.getPersistentData().putBoolean("abyss_domain_owner_dead", true);
        entity.getPersistentData().putDouble("abyss_drop_x", inst.overworldCenter.x);
        entity.getPersistentData().putDouble("abyss_drop_y", inst.overworldCenter.y);
        entity.getPersistentData().putDouble("abyss_drop_z", inst.overworldCenter.z);
        entity.getPersistentData().putString("abyss_drop_dim", inst.returnDim.location().toString());

        scheduleEndAfterDeath(entity, inst);
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity entity = event.getEntity();
        var data = entity.getPersistentData();
        if (!data.getBoolean("abyss_domain_owner_dead") && !data.contains("abyss_drop_x")) {
            return;
        }

        MinecraftServer server = entity.level().getServer();
        if (server == null) return;

        double dx = data.getDouble("abyss_drop_x");
        double dy = data.getDouble("abyss_drop_y");
        double dz = data.getDouble("abyss_drop_z");

        ServerLevel overworld = server.overworld();
        String dimStr = data.getString("abyss_drop_dim");
        if (dimStr != null && !dimStr.isEmpty()) {
            ResourceLocation dimId = new ResourceLocation(dimStr);
            for (ServerLevel level : server.getAllLevels()) {
                if (level.dimension().location().equals(dimId)) {
                    overworld = level;
                    break;
                }
            }
        }

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem().copy();
            if (stack.isEmpty()) continue;
            ItemEntity overDrop = new ItemEntity(overworld, dx, dy + 0.5, dz, stack);
            overDrop.setDefaultPickUpDelay();
            overworld.addFreshEntity(overDrop);
        }
        event.getDrops().clear();
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEffectInstance() == null) return;
        if (!isDomainEffect(event.getEffectInstance().getEffect())) return;

        LivingEntity entity = event.getEntity();
        if (entity.getPersistentData().getBoolean("abyss_domain_transferring")) return;

        if (isDyingOrDead(entity)) {
            DomainInstance inst = DomainInstanceManager.getByOwner(entity.getUUID());
            if (inst != null) {
                entity.getPersistentData().putBoolean("abyss_domain_owner_dead", true);
                entity.getPersistentData().putDouble("abyss_drop_x", inst.overworldCenter.x);
                entity.getPersistentData().putDouble("abyss_drop_y", inst.overworldCenter.y);
                entity.getPersistentData().putDouble("abyss_drop_z", inst.overworldCenter.z);
                entity.getPersistentData().putString("abyss_drop_dim", inst.returnDim.location().toString());
                scheduleEndAfterDeath(entity, inst);
            }
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

        if (isDyingOrDead(entity)) {
            DomainInstance inst = DomainInstanceManager.getByOwner(entity.getUUID());
            if (inst != null) {
                entity.getPersistentData().putBoolean("abyss_domain_owner_dead", true);
                entity.getPersistentData().putDouble("abyss_drop_x", inst.overworldCenter.x);
                entity.getPersistentData().putDouble("abyss_drop_y", inst.overworldCenter.y);
                entity.getPersistentData().putDouble("abyss_drop_z", inst.overworldCenter.z);
                entity.getPersistentData().putString("abyss_drop_dim", inst.returnDim.location().toString());
                scheduleEndAfterDeath(entity, inst);
            }
            return;
        }

        entity.getPersistentData().putBoolean("abyss_domain_handled", false);
        entity.getPersistentData().putBoolean("abyss_domain_pending", false);
        DomainLifecycle.end(entity);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        var player = event.player;
        var data = player.getPersistentData();

        boolean cancelMode =
                data.getBoolean("abyss_cancel_domain_auth")
                        || data.getBoolean("abyss_domain_cancel_charging");

        if (!cancelMode) return;

        // Releasing the key interrupts the charge.
        // The domain remains active because cancellation did not complete.
        if (!data.getBoolean("PRESS_Z")) {
            data.putBoolean("abyss_domain_cancel_charging", false);
            data.putInt("abyss_domain_cancel_charge", 0);
            data.putBoolean("abyss_cancel_domain_auth", false);
            data.putDouble("skill", 0.0);
            return;
        }

        // Only charge while the player still owns an Abyss domain.
        if (DomainInstanceManager.getByOwner(player.getUUID()) == null) {
            data.putBoolean("abyss_domain_cancel_charging", false);
            data.putInt("abyss_domain_cancel_charge", 0);
            data.putBoolean("abyss_cancel_domain_auth", false);
            data.putDouble("skill", 0.0);
            return;
        }

        AbyssDomainCast.tickCancelDomain(player);
    }

}