package com.ghosts.abyss.domain;

import com.ghosts.abyss.RefinementPoints;
import com.ghosts.abyss.client.XaeroMinimapBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.function.Function;

public class DomainTeleport {

    private static final ResourceLocation DOMAIN_EFFECT_ID =
            new ResourceLocation("jujutsucraft", "domain_expansion");

    public static void sendToArena(LivingEntity entity, ServerLevel domainLevel, BlockPos arenaCenter) {
        sendToArenaAt(entity, domainLevel,
                arenaCenter.getX() + 0.5,
                arenaCenter.getY() + 1.1,
                arenaCenter.getZ() + 0.5);
    }

    public static void sendToArenaAt(LivingEntity entity, ServerLevel domainLevel,
                                     double x, double y, double z) {
        teleport(entity, domainLevel, x, y, z);
    }

    public static void sendBack(LivingEntity entity, ServerLevel returnLevel, Vec3 pos) {
        teleport(entity, returnLevel, pos.x, pos.y, pos.z);

        if (entity instanceof ServerPlayer player) {
            XaeroMinimapBridge.applyTo(player);
            player.server.execute(() -> {
                if (!player.isAlive()) return;
                XaeroMinimapBridge.applyTo(player);
                RefinementPoints.sync(player);
                boolean unlocked = player.getPersistentData().getBoolean("minimap_unlocked");
                boolean enabled  = player.getPersistentData().getBoolean("minimap_enabled");
                com.ghosts.abyss.network.ModNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new com.ghosts.abyss.network.PacketSyncMinimapState(unlocked, enabled)
                );
            });
        }
    }

    public static LivingEntity teleport(LivingEntity entity, ServerLevel dest, double x, double y, double z) {
        if (entity instanceof ServerPlayer player) {
            // Snapshot refinement before dimension move (safety)
            double refinement = player.getPersistentData().getDouble(RefinementPoints.KEY_SPENT);
            double available = player.getPersistentData().getDouble(RefinementPoints.KEY_AVAILABLE);

            player.teleportTo(dest, x, y, z, player.getYRot(), player.getXRot());

            player.getPersistentData().putDouble(RefinementPoints.KEY_SPENT, refinement);
            player.getPersistentData().putDouble(RefinementPoints.KEY_AVAILABLE, available);
            XaeroMinimapBridge.applyTo(player);
            RefinementPoints.sync(player);
            return player;
        }

        if (entity.level() == dest) {
            entity.moveTo(x, y, z, entity.getYRot(), entity.getXRot());
            return entity;
        }

        CompoundTag persistCopy = entity.getPersistentData().copy();
        float health = entity.getHealth();
        float yRot = entity.getYRot();
        float xRot = entity.getXRot();
        UUID id = entity.getUUID();

        // Only pause pathfinding briefly — do NOT permanently clear combat AI after land
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
        }

        entity.getPersistentData().putBoolean("abyss_domain_transferring", true);

        Entity moved = entity.changeDimension(dest, new ITeleporter() {
            @Override
            public Entity placeEntity(Entity e, ServerLevel current, ServerLevel destination,
                                      float yaw, Function<Boolean, Entity> repositionEntity) {
                Entity neu = repositionEntity.apply(false);
                if (neu != null) {
                    neu.moveTo(x, y, z, yaw, e.getXRot());
                }
                return neu;
            }

            @Override
            public boolean isVanilla() {
                return false;
            }
        });

        LivingEntity result = null;

        if (moved instanceof LivingEntity livingMoved) {
            result = livingMoved;
        } else if (entity.isAlive()) {
            Entity copy = entity.getType().create(dest);
            if (copy instanceof LivingEntity livingCopy) {
                entity.discard();

                copy.restoreFrom(entity);
                copy.setUUID(id);
                copy.moveTo(x, y, z, yRot, xRot);

                livingCopy.getPersistentData().merge(persistCopy);
                livingCopy.setHealth(health);

                dest.addFreshEntity(copy);
                result = livingCopy;
            }
        }

        if (result != null) {
            result.getPersistentData().merge(persistCopy);
            result.getPersistentData().putBoolean("abyss_domain_transferring", true);
            result.getPersistentData().putBoolean("abyss_domain_handled", true);
            // Clear JC clash-loss flags so they can act / sure-hit after transfer
            result.getPersistentData().putBoolean("Failed", false);
            result.getPersistentData().putBoolean("Cover", false);
            result.getPersistentData().putBoolean("DomainDefeated", false);
            reapplyDomainEffect(result);
            result.getPersistentData().putBoolean("abyss_domain_transferring", false);
            // Do not setTarget(null) here — let AI retarget
        } else if (entity.isAlive()) {
            entity.getPersistentData().putBoolean("abyss_domain_transferring", false);
        }

        return result;
    }

    private static void reapplyDomainEffect(LivingEntity entity) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(DOMAIN_EFFECT_ID);
        if (effect == null) return;

        MobEffectInstance cur = entity.getEffect(effect);
        int duration = cur != null
                ? Math.max(cur.getDuration(), DomainEvents.DOMAIN_DURATION_TICKS)
                : DomainEvents.DOMAIN_DURATION_TICKS;
        entity.addEffect(new MobEffectInstance(effect, duration, 1, false, false, true));
    }
}