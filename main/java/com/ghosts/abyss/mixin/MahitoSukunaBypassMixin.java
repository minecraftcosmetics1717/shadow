package com.ghosts.abyss.mixin;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

import net.minecraftforge.registries.ForgeRegistries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.ChangeDamage1Procedure"},
        remap = false
)
public class MahitoSukunaBypassMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           cancellable = true,
           remap = false
   )
   private static void specialGradeBypass(LevelAccessor world, Entity entity, Entity entityiterator, CallbackInfoReturnable<Double> cir) {
      if (entity instanceof ServerPlayer player) {
         double skill = entity.getPersistentData().getDouble("skill");

         // Skill ID 1505 = Mahito's Idle Transfiguration / Touch
         if (skill == 1505.0D) {
            if (isSukunaType(entityiterator)) {
               Advancement adv = player.getServer().getAdvancements().getAdvancement(ResourceLocation.tryParse("jujutsucraft:sorcerer_grade_special"));

               // If player has Special Grade Sorcerer advancement, bypass typical soul protection and apply 1.25 multiplier
               if (adv != null && player.getAdvancements().getOrStartProgress(adv).isDone()) {
                  cir.setReturnValue(1.25D);
               }
            }
         }
      }
   }

   /**
    * Checks if the target entity is classified as Sukuna (via potion effect or possessed Sukuna Finger item).
    */
   private static boolean isSukunaType(Entity entity) {
      if (entity instanceof LivingEntity living) {
         MobEffect sukunaFx = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse("jujutsucraft:sukuna_effect"));

         if (sukunaFx != null && living.hasEffect(sukunaFx)) {
            return true;
         } else {
            try {
               Class<?> proc = Class.forName("net.mcreator.jujutsucraft.procedures.ReturnInsideItemProcedure");
               Object result = proc.getMethod("execute", Entity.class).invoke(null, entity);

               if (result instanceof ItemStack stack) {
                  Item sukunaFinger = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse("jujutsucraft:sukuna_finger"));
                  return sukunaFinger != null && stack.is(sukunaFinger);
               }
            } catch (Exception ignored) {
            }

            return false;
         }
      } else {
         return false;
      }
   }
}