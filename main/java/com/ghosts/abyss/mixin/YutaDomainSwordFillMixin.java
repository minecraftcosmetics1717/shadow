package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.YutaTechniqueRegistry;

import java.lang.reflect.Field;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

import net.minecraftforge.items.ItemHandlerHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.EntityItemRightClickedOnEntityProcedure"},
        remap = false
)
public class YutaDomainSwordFillMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           cancellable = true,
           remap = false
   )
   private static void jslop$yutaSwordFill(LevelAccessor world, Entity entity, Entity sourceentity, CallbackInfo ci) {
      if (!(world instanceof ServerLevel)) {
         return;
      }

      if (sourceentity instanceof ServerPlayer player) {
         // Check if player uses Yuta's Copy Technique
         if (YutaTechniqueRegistry.hasYutaCT(player)) {
            // Verify if target entity is a domain sword decoration
            if (jslop$isDomainDecoration(entity)) {
               ListTag list = YutaTechniqueRegistry.getList(player);
               int count = list.size();

               if (count != 0 && entity instanceof LivingEntity swordEntity) {
                  ItemStack sword = swordEntity.getItemBySlot(EquipmentSlot.HEAD).copy();

                  if (!sword.isEmpty()) {
                     // Pick a random copied technique from Yuta's stored techniques
                     int idx = (int) Math.floor(Math.random() * (double) count);
                     CompoundTag tech = list.getCompound(idx);

                     double skill = tech.getDouble("skill");
                     double cooldown = tech.getDouble("cooldown_ticks");
                     String name = tech.getString("name");

                     CompoundTag tag = sword.getOrCreateTag();
                     tag.putDouble("skill", skill);
                     tag.putDouble("COOLDOWN_TICKS", cooldown);

                     sword.setHoverName(Component.literal(name));
                     sword.setCount(1);

                     player.getCooldowns().addCooldown(sword.getItem(), 10);

                     // Equip to main hand or give to inventory
                     ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                     if (mainHand.isEmpty()) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
                        player.getInventory().setChanged();
                     } else {
                        ItemHandlerHelper.giveItemToPlayer(player, sword);
                     }

                     // Remove the domain sword decoration entity from world
                     entity.discard();
                     ci.cancel();
                  }
               }
            }
         }
      }
   }

   /**
    * Checks if entity is an instance of EntityItemEntity marked as domain_decoration via entity data.
    */
   private static boolean jslop$isDomainDecoration(Entity entity) {
      try {
         Class<?> cls = Class.forName("net.mcreator.jujutsucraft.entity.EntityItemEntity");
         if (!cls.isInstance(entity)) {
            return false;
         } else {
            Field f = cls.getDeclaredField("DATA_domain_decoration");
            f.setAccessible(true);

            @SuppressWarnings("unchecked")
            EntityDataAccessor<Boolean> accessor = (EntityDataAccessor<Boolean>) f.get(null);
            return entity.getEntityData().get(accessor);
         }
      } catch (Exception e) {
         Abyss.LOGGER.warn("[JSlop] SwordFill: domain_decoration reflection failed: {}", e.getMessage());
         return false;
      }
   }
}