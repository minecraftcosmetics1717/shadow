package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;
import net.mcreator.jujutsucraft.entity.StevensonScreenEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = StevensonScreenEntity.class, remap = false)
public abstract class MixinStevensonScreenEntity extends PathfinderMob {

   protected MixinStevensonScreenEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
      super(entityType, level);
   }

   @Override
   public boolean shouldDespawnInPeaceful() {
      return !this.level().getGameRules().getBoolean(Abyss.STEVENSON_NOT_DESPAWN);
   }

   @Override
   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      if (this.level().getGameRules().getBoolean(Abyss.STEVENSON_NOT_DESPAWN)) {
         return false;
      }
      return super.removeWhenFarAway(distanceToClosestPlayer);
   }
}