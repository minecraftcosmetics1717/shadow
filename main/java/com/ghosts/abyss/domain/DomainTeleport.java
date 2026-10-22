package com.ghosts.abyss.domain;

import com.ghosts.abyss.client.XaeroMinimapBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.UUID;
import java.util.function.Function;

public class DomainTeleport {

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
                boolean unlocked = player.getPersistentData().getBoolean("minimap_unlocked");
                boolean enabled  = player.getPersistentData().getBoolean("minimap_enabled");
                com.ghosts.abyss.network.ModNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new com.ghosts.abyss.network.PacketSyncMinimapState(unlocked, enabled)
                );
            });
        }
    }

    private static void teleport(LivingEntity entity, ServerLevel dest, double x, double y, double z) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(dest, x, y, z, player.getYRot(), player.getXRot());
            XaeroMinimapBridge.applyTo(player);
            return;
        }

        // Already in destination dimension
        if (entity.level() == dest) {
            entity.moveTo(x, y, z, entity.getYRot(), entity.getXRot());
            return;
        }

        // Try Forge changeDimension first
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

        if (moved != null) return;

        // Fallback: clone into dest, discard original (keeps same UUID for owner tracking)
        if (!entity.isAlive()) return;

        Entity copy = entity.getType().create(dest);
        if (copy == null) return;

        UUID id = entity.getUUID();
        entity.discard();

        copy.restoreFrom(entity);
        copy.setUUID(id);
        copy.moveTo(x, y, z, entity.getYRot(), entity.getXRot());
        if (copy instanceof LivingEntity livingCopy) {
            livingCopy.setHealth(entity.getHealth());
        }
        dest.addFreshEntity(copy);
    }
}