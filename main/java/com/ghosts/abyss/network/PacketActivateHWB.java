package com.ghosts.abyss.network;

import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.Abyss;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class PacketActivateHWB {
   private final boolean active;

   public PacketActivateHWB(boolean active) {
      this.active = active;
   }

   public PacketActivateHWB(FriendlyByteBuf buf) {
      this.active = buf.readBoolean();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.active);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            if (AbyssGamerules.isEnabled(player, AbyssGamerules.HOLLOW_WICKER_BASKET)) {
               if (!this.active || player.getPersistentData().getBoolean("unlocked_hwb")) {
                  if (!this.active || player.level().getGameTime() >= player.getPersistentData().getLong("jslop_hwb_cooldown_until")) {
                     if (this.active) {
                        boolean hasBrainDamage = player.getActiveEffects().stream().anyMatch((e) -> {
                           ResourceLocation loc = ForgeRegistries.MOB_EFFECTS.getKey(e.getEffect());
                           return loc != null && loc.toString().equals("jujutsucraft:brain_damage");
                        });
                        if (hasBrainDamage) {
                           return;
                        }
                     }

                     boolean hasPerfectBody = player.getPersistentData().getBoolean("perfect_body");
                     boolean newState;

                     if (hasPerfectBody) {
                        // Debounce check: Prevent rapid toggling if key is held or packet sent twice quickly
                        long currentTime = player.level().getGameTime();
                        long lastToggle = player.getPersistentData().getLong("jslop_hwb_last_toggle");

                        // Ignore key presses/packets sent within 5 ticks (0.25s) of the previous toggle
                        if (currentTime - lastToggle < 5L) {
                           return;
                        }

                        player.getPersistentData().putLong("jslop_hwb_last_toggle", currentTime);

                        // Toggle state regardless of whether packet sent true or false
                        newState = !player.getPersistentData().getBoolean("hwb_active");
                     } else {
                        // Regular behavior: Active only while holding the key
                        newState = this.active;
                     }

                     player.getPersistentData().putBoolean("hwb_active", newState);

// Original Jujutsucraft animation triggers
                     if (newState) {
                        setAnimation(player, -16.0D, 1.0D);
                        triggerAnimation(player);
                     } else {
                        setAnimation(player, 0.0D, 0.0D);
                     }
                  }
               }
            }
         }
      });
      ctx.setPacketHandled(true);
   }

   private static void setAnimation(ServerPlayer player, double anim1Val, double anim2Val) {
      try {
         Class<?> attrsClass = Class.forName("net.mcreator.jujutsucraft.init.JujutsucraftModAttributes");
         Attribute anim1 = getAttr(attrsClass, "ANIMATION_1");
         Attribute anim2 = getAttr(attrsClass, "ANIMATION_2");
         if (anim1 != null) {
            AttributeInstance inst = player.getAttribute(anim1);
            if (inst != null) {
               inst.setBaseValue(anim1Val);
            }
         }

         if (anim2 != null) {
            AttributeInstance inst = player.getAttribute(anim2);
            if (inst != null) {
               inst.setBaseValue(anim2Val);
            }
         }
      } catch (Exception e) {
         Abyss.LOGGER.warn("HWB: could not set animation attributes: {}", e.getMessage());
      }
   }

   private static void triggerAnimation(ServerPlayer player) {
      try {
         Registry<DamageType> registry = player.level().registryAccess().registry(Registries.DAMAGE_TYPE).orElse(null);
         if (registry == null) {
            return;
         }

         ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jujutsucraft", "start_animation"));
         if (!registry.containsKey(key)) {
            return;
         }

         Holder.Reference<DamageType> holder = registry.getHolderOrThrow(key);
         player.hurt(new DamageSource(holder), 1.0F);
      } catch (Exception e) {
         Abyss.LOGGER.warn("HWB: could not trigger animation: {}", e.getMessage());
      }
   }

   private static Attribute getAttr(Class<?> attrsClass, String fieldName) throws Exception {
      Field f = attrsClass.getDeclaredField(fieldName);
      f.setAccessible(true);
      Object supplier = f.get(null);
      return (Attribute) supplier.getClass().getMethod("get").invoke(supplier);
   }
}