package com.ghosts.abyss.mixin;

import com.ghosts.abyss.RefinementPoints;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.registries.ForgeRegistries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.DomainExpansionEffectExpiresProcedure"},
        remap = false
)
public class DomainVictoryMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           remap = false
   )
   private static void onDomainExpire(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (!entity.getPersistentData().getBoolean("DomainDefeated")) {
         return;
      }

      if (world instanceof ServerLevel serverLevel) {
         MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse("jujutsucraft:domain_expansion"));

         if (domainEffect != null) {
            double range = 60.0D;
            Vec3 center = new Vec3(
                    entity.getPersistentData().getDouble("x_pos_doma"),
                    entity.getPersistentData().getDouble("y_pos_doma"),
                    entity.getPersistentData().getDouble("z_pos_doma")
            );

            AABB searchArea = new AABB(center, center).inflate(range);

            // Grant 1 refinement point to any opposing players within range who still have their domain active
            for (ServerPlayer winner : serverLevel.getEntitiesOfClass(ServerPlayer.class, searchArea, p -> p != entity && p.hasEffect(domainEffect))) {
               RefinementPoints.grantRefinementPoints(winner, 1);
            }
         }
      }
   }
}