package com.ghosts.abyss.entity;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.init.JujutsuSlopEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.Collections;

public class RockEntity extends LivingEntity implements ItemSupplier {

   private int lifeTicks = 60; // 2 seconds

   public RockEntity(EntityType<? extends LivingEntity> type, Level level) {
      super(type, level);
   }

   public RockEntity(Level level) {
      this(JujutsuSlopEntities.ROCK.get(), level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return LivingEntity.createLivingAttributes()
              .add(Attributes.MAX_HEALTH, 4.0D)
              .add(Attributes.MOVEMENT_SPEED, 0.0D)
              .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
              .add(Attributes.FOLLOW_RANGE, 0.0D);
   }

   // Required by ThrownItemRenderer
   @Override
   public ItemStack getItem() {
      return new ItemStack(Abyss.ROCK.get());
   }

   @Override
   public void tick() {
      super.tick();

      if (!this.level().isClientSide) {
         this.lifeTicks--;
         if (this.lifeTicks <= 0) {
            this.discard();
         }
      }
   }

   @Override
   public Iterable<ItemStack> getArmorSlots() {
      return Collections.emptyList();
   }

   @Override
   public ItemStack getItemBySlot(EquipmentSlot slot) {
      return ItemStack.EMPTY;
   }

   @Override
   public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}

   @Override
   public HumanoidArm getMainArm() {
      return HumanoidArm.RIGHT;
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return NetworkHooks.getEntitySpawningPacket(this);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("LifeTicks", this.lifeTicks);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("LifeTicks")) {
         this.lifeTicks = tag.getInt("LifeTicks");
      }
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      return super.hurt(source, amount);
   }
}