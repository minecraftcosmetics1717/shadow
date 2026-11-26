package com.ghosts.abyss.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** A cinematic character, not a living combat target. */
public final class HakariScenarioActor extends Entity {
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(HakariScenarioActor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> WALKING =
            SynchedEntityData.defineId(HakariScenarioActor.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> SITTING =
            SynchedEntityData.defineId(HakariScenarioActor.class, EntityDataSerializers.BOOLEAN);

    public HakariScenarioActor(EntityType<? extends HakariScenarioActor> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override protected void defineSynchedData() {
        entityData.define(SKIN, 0);
        entityData.define(WALKING, false);
        entityData.define(SITTING, false);
    }
    public int skin() { return entityData.get(SKIN); }
    public void setSkin(int skin) { entityData.set(SKIN, skin == 1 ? 1 : 0); }
    public boolean walking() { return entityData.get(WALKING); }
    public void setWalking(boolean walking) { entityData.set(WALKING, walking); }

    public boolean sitting() { return entityData.get(SITTING); }
    public void setSitting(boolean sitting) {
        entityData.set(SITTING, sitting);
        if (sitting) setWalking(false);
    }

    // Future scenario scripts can call this with a waypoint in world coordinates.
    // The script is responsible for choosing unobstructed paths.
    public boolean walkToward(Vec3 destination, double blocksPerTick) {
        if (level().isClientSide) return false;
        setSitting(false);
        Vec3 delta = destination.subtract(position());
        double distance = delta.length();
        if (distance <= blocksPerTick && blocksPerTick > 0) {
            if (delta.horizontalDistanceSqr() > 1.0E-6) setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
            setPos(destination.x, destination.y, destination.z);
            setWalking(false);
            return true;
        }
        if (blocksPerTick <= 0) return false;
        Vec3 step = delta.scale(Math.min(1.0, blocksPerTick / distance));
        if (step.horizontalDistanceSqr() > 1.0E-6) {
            setYRot((float) Math.toDegrees(Math.atan2(-step.x, step.z)));
        }
        setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
        setWalking(true);
        return false;
    }

    @Override public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        // This entity is moved only by the scenario script.
    }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public void push(Entity entity) { }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        setSkin(tag.getInt("Skin"));
        setWalking(false);
        setSitting(false);
        setNoGravity(true);
        setInvulnerable(true);
        noPhysics = true;
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Skin", skin());
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
