package com.ghosts.abyss.mixin;

import com.ghosts.abyss.BlackFlashPoints;
import com.ghosts.abyss.Abyss;
import java.lang.reflect.Field;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.capabilities.Capability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.RangeAttackProcedure"},
        remap = false
)
public class BlackFlashMixin {
   private static final ThreadLocal<Boolean> AT_WILL_ACTIVE = ThreadLocal.withInitial(() -> false);
   private static final ThreadLocal<Integer> BONUS_ROLLS = ThreadLocal.withInitial(() -> 0);
   private static final ThreadLocal<Boolean> BF_FIRED = ThreadLocal.withInitial(() -> false);
   private static final ThreadLocal<Entity> BF_ENTITY = ThreadLocal.withInitial(() -> null);

   @Inject(
           method = "execute",
           at = @At("HEAD"),
           remap = false
   )
   private static void onHead(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      AT_WILL_ACTIVE.set(false);
      BF_FIRED.set(false);
      BF_ENTITY.set(null);

      if (entity instanceof ServerPlayer player) {
         BONUS_ROLLS.set(BlackFlashPoints.getBonusRolls(player));
         AT_WILL_ACTIVE.set(BlackFlashPoints.isAtWillCharged(player));
      } else {
         BONUS_ROLLS.set(0);
      }
   }

   @Redirect(
           method = "execute",
           at = @At(value = "INVOKE", target = "Ljava/lang/Math;round(D)J"),
           remap = false
   )
   private static long onRollCount(double value) {
      return Math.round(value + (double) BONUS_ROLLS.get());
   }

   @Redirect(
           method = "execute",
           at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"),
           remap = false
   )
   private static double onRandom() {
      return AT_WILL_ACTIVE.get() ? 1.0D : Math.random();
   }

   @Inject(
           method = "execute",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/mcreator/jujutsucraft/network/JujutsucraftModVariables$PlayerVariables;syncPlayerVariables(Lnet/minecraft/world/entity/Entity;)V"
           ),
           remap = false
   )
   private static void onBlackFlashConfirmed(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (entity instanceof ServerPlayer player) {
         BlackFlashPoints.onBlackFlash(player);
         BF_FIRED.set(true);
         BF_ENTITY.set(entity);

         int bonusEnergy = BlackFlashPoints.getBonusEnergy(player);
         if (bonusEnergy > 0) {
            try {
               Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
               Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
               capField.setAccessible(true);

               @SuppressWarnings("unchecked")
               Capability<Object> cap = (Capability<Object>) capField.get(null);
               Object pVars = entity.getCapability(cap, (Direction) null).orElse(null);

               if (pVars != null) {
                  Field expField = pVars.getClass().getDeclaredField("PlayerExperience");
                  expField.setAccessible(true);
                  expField.set(pVars, (Double) expField.get(pVars) + (double) bonusEnergy);
               }
            } catch (Exception e) {
               Abyss.LOGGER.warn("[JSlop] BF energy bonus failed: {}", e.getMessage());
            }
         }

         if (AT_WILL_ACTIVE.get()) {
            BlackFlashPoints.consumeAtWillCharge(player);
         }
      }
   }

   @Inject(
           method = "execute",
           at = @At("TAIL"),
           remap = false
   )
   private static void onTail(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      try {
         if (BF_FIRED.get() && BF_ENTITY.get() instanceof ServerPlayer player) {
            // Schedule reduction for the next server tick to let Jujutsucraft finish applying fatigue first
            if (player.getServer() != null) {
               player.getServer().execute(() -> {
                  BlackFlashPoints.applyFatigueBrainBonus(player);
                  BlackFlashPoints.sync(player);
               });
            } else {
               BlackFlashPoints.applyFatigueBrainBonus(player);
               BlackFlashPoints.sync(player);
            }
         }
      } finally {
         AT_WILL_ACTIVE.set(false);
         BONUS_ROLLS.set(0);
         BF_FIRED.set(false);
         BF_ENTITY.set(null);
      }
   }
}