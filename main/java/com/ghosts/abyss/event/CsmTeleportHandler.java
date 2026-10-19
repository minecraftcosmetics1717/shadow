package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class CsmTeleportHandler {

    private static final double MAX_DISTANCE_SQR = 32.0D * 32.0D; // 32 blocks squared (1024.0)

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        // 1. Run only on server side and for living entities
        if (entity.level().isClientSide() || !entity.isAlive()) {
            return;
        }

        // 2. Throttle check: Only execute once every 10 ticks (0.5s) per entity
        if (entity.tickCount % 10 != 0) {
            return;
        }

        // 3. Quick NBT filter
        CompoundTag persistentData = entity.getPersistentData();
        if (!persistentData.getBoolean("CursedSpirit")) {
            return;
        }

        String ownerUuidStr = persistentData.getString("OWNER_UUID");
        if (ownerUuidStr.isEmpty()) {
            return;
        }

        try {
            UUID ownerUuid = UUID.fromString(ownerUuidStr);
            ServerPlayer owner = ((ServerLevel) entity.level()).getServer().getPlayerList().getPlayer(ownerUuid);

            if (owner != null) {
                // Discard spirit if owner is dead
                if (!owner.isAlive()) {
                    entity.discard();
                    return;
                }

                // Cross-dimensional teleport
                if (entity.level() != owner.level()) {
                    entity.changeDimension((ServerLevel) owner.level());
                    entity.teleportTo(
                            owner.getX() + (Math.random() - 0.5D) * 2.0D,
                            owner.getY(),
                            owner.getZ() + (Math.random() - 0.5D) * 2.0D
                    );
                }
                // Teleport if farther than 32 blocks (32^2 = 1024)
                else if (entity.distanceToSqr(owner) > 1024.0D) {
                    entity.teleportTo(
                            owner.getX() + (Math.random() - 0.5D) * 2.0D,
                            owner.getY(),
                            owner.getZ() + (Math.random() - 0.5D) * 2.0D
                    );
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Safe check for invalid UUID strings
        }
    }
}