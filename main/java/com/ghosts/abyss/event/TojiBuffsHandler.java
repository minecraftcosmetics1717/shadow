package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss; //
import net.mcreator.jujutsucraft.init.JujutsucraftModItems; //
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables; //
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

@EventBusSubscriber(modid = "abyss")
public class TojiBuffsHandler {

   private static final UUID PLAYFUL_CLOUD_SPEED_UUID = UUID.fromString("7f38da64-6cfa-4d1d-91b4-123456789abc");
   private static final AttributeModifier PLAYFUL_CLOUD_SPEED_MODIFIER = new AttributeModifier(
           PLAYFUL_CLOUD_SPEED_UUID,
           "Playful Cloud Speed Boost",
           0.13D,
           AttributeModifier.Operation.MULTIPLY_TOTAL
   );

   @SubscribeEvent
   public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
      if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
         return;
      }

      AttributeInstance speedAttribute = event.player.getAttribute(Attributes.MOVEMENT_SPEED);

      event.player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent((ctData) -> {
         if (ctData.PlayerCurseTechnique == -1.0) {

            if (event.player.isShiftKeyDown()) {
               event.player.heal(0.2F);
               event.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 1, true, false));

               event.player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 2, 0, true, false));
            }

            if (event.player.getHealth() <= event.player.getMaxHealth() / 2.0F) {
               int strengthLevel = (int) (event.player.getMaxHealth() - event.player.getHealth());
               event.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, strengthLevel, true, false));
            }

            ItemStack mainHandItem = event.player.getMainHandItem();
            ResourceLocation itemRegistryName = ForgeRegistries.ITEMS.getKey(mainHandItem.getItem());

            boolean holdingPlayfulCloud = mainHandItem.is(JujutsucraftModItems.PLAYFUL_CLOUD.get()) ||
                    (itemRegistryName != null && itemRegistryName.toString().equals("jujutsucraftv:sharpened_playful_cloud")); //

            if (holdingPlayfulCloud) {
               event.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 10, true, false));

               if (speedAttribute != null && !speedAttribute.hasModifier(PLAYFUL_CLOUD_SPEED_MODIFIER)) {
                  speedAttribute.addTransientModifier(PLAYFUL_CLOUD_SPEED_MODIFIER);
               }
            } else {
               if (speedAttribute != null && speedAttribute.hasModifier(PLAYFUL_CLOUD_SPEED_MODIFIER)) {
                  speedAttribute.removeModifier(PLAYFUL_CLOUD_SPEED_UUID);
               }
            }
         } else {
            if (speedAttribute != null && speedAttribute.hasModifier(PLAYFUL_CLOUD_SPEED_MODIFIER)) {
               speedAttribute.removeModifier(PLAYFUL_CLOUD_SPEED_UUID);
            }
         }
      });
   }
}