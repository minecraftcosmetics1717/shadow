package com.ghosts.abyss.mixin;

import com.ghosts.abyss.csm.CsmRoster;
import com.ghosts.abyss.network.CsmSync;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.CursedSpiritBallFoodEatenProcedure"},
        remap = false
)
public class CsmAbsorbMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           cancellable = true,
           remap = false
   )
   private static void onExecute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack, CallbackInfo ci) {
      if (entity instanceof ServerPlayer player) {
         if (isCsmPlayer(player)) {
            // Read stored item tag data
            double yData = itemstack.getOrCreateTag().getDouble("y_data");
            String nameData = itemstack.getOrCreateTag().getString("name_data");

            if (player.getServer() != null) {
               ServerLevel csmDim = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation("jujutsucraft", "cursed_spirit_manipulation_dimension")));

               if (csmDim != null) {
                  Vec3 center = new Vec3(0.0D, yData, 0.0D);

                  // Fixed getEntitiesOfClass with safe entity predicate
                  List<Entity> found = csmDim.getEntitiesOfClass(
                          Entity.class,
                          new AABB(center, center).inflate(2.0D),
                          (e) -> e.isAlive() && nameData.equals(e.getDisplayName().getString())
                  );

                  if (!found.isEmpty()) {
                     Entity spiritEntity = found.get(0);
                     if (spiritEntity instanceof LivingEntity living) {
                        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(spiritEntity.getType());
                        String entityType = (rl != null) ? rl.toString() : "unknown";
                        String displayName = spiritEntity.getDisplayName().getString();

                        double maxHp = 20.0D;
                        double atk = 3.0D;

                        if (living.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                           maxHp = living.getAttributeValue(Attributes.MAX_HEALTH);
                        }

                        if (living.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                           atk = living.getAttributeValue(Attributes.ATTACK_DAMAGE);
                        }

                        int witherLevel = 0;
                        if (living.hasEffect(MobEffects.WITHER)) {
                           witherLevel = living.getEffect(MobEffects.WITHER).getAmplifier();
                        }

                        // Spawn poof particles at spirit location
                        csmDim.sendParticles(
                                ParticleTypes.POOF,
                                spiritEntity.getX(),
                                spiritEntity.getY() + spiritEntity.getBbHeight() * 0.5D,
                                spiritEntity.getZ(),
                                20, 0.4D, 0.4D, 0.4D, 0.0D
                        );

                        // Store in roster & remove spirit from CSM storage dimension
                        CsmRoster.captureSpirit(player, entityType, displayName, maxHp, atk, witherLevel);
                        spiritEntity.remove(RemovalReason.DISCARDED);
                        CsmSync.sendRoster(player);

                        itemstack.shrink(1);
                        ci.cancel(); // Cancel original MCreator procedure execution
                     }
                  }
               }
            }
         }
      }
   }

   @SuppressWarnings("unchecked")
   private static boolean isCsmPlayer(ServerPlayer player) {
      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         Capability<Object> cap = (Capability<Object>) capField.get(null);
         Object vars = player.getCapability(cap, (Direction) null).orElse(null);

         if (vars == null) {
            return false;
         } else {
            Field ct1 = vars.getClass().getDeclaredField("PlayerCurseTechnique");
            Field ct2 = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
            ct1.setAccessible(true);
            ct2.setAccessible(true);

            double technique1 = (Double) ct1.get(vars);
            double technique2 = (Double) ct2.get(vars);

            return technique1 == 18.0D || technique2 == 18.0D;
         }
      } catch (Exception e) {
         return false;
      }
   }
}