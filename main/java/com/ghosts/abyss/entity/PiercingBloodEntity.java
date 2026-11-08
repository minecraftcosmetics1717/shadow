
package com.ghosts.abyss.entity;

import com.ghosts.abyss.blood.AbyssPiercingBlood;
import com.ghosts.abyss.init.JujutsuSlopEntities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class PiercingBloodEntity extends Entity {

    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(
                    PiercingBloodEntity.class,
                    EntityDataSerializers.INT
            );

    private UUID ownerUUID;

    public PiercingBloodEntity(
            EntityType<? extends PiercingBloodEntity> type,
            Level level
    ) {
        super(type, level);
        this.noPhysics = true;
    }

    public PiercingBloodEntity(Level level, ServerPlayer owner) {
        this(JujutsuSlopEntities.PIERCING_BLOOD.get(), level);
        setOwner(owner);
        followOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER_ID, -1);
    }

    public void setOwner(Player player) {
        ownerUUID = player.getUUID();
        entityData.set(OWNER_ID, player.getId());
    }

    public Player getOwnerPlayer() {
        Entity entity = level().getEntity(entityData.get(OWNER_ID));
        return entity instanceof Player player ? player : null;
    }

    public boolean belongsTo(Player player) {
        return ownerUUID != null
                && ownerUUID.equals(player.getUUID());
    }

    public void followOwner(Player player) {
        Vec3 eye = player.getEyePosition();

        setPos(
                eye.x,
                eye.y - 0.28D,
                eye.z
        );

        setYRot(player.getYRot());
        setXRot(player.getXRot());
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        Player owner = getOwnerPlayer();

        if (owner == null
                || !owner.isAlive()
                || !AbyssPiercingBlood.isReady(owner)) {
            discard();
            return;
        }

        followOwner(owner);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256.0D * 256.0D;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
