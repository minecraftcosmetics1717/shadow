package com.ghosts.abyss.network;

import com.ghosts.abyss.Abyss;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.network.NetworkEvent;

public class PacketToggleAbsorbedTechnique {
   public PacketToggleAbsorbedTechnique() {
   }

   public PacketToggleAbsorbedTechnique(FriendlyByteBuf buf) {
   }

   public static void encode(PacketToggleAbsorbedTechnique msg, FriendlyByteBuf buf) {
   }

   public static void handle(PacketToggleAbsorbedTechnique msg, Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            CompoundTag data = player.getPersistentData();
            if (!data.contains("jujutsuslop_secondary_ct")) {
            } else {
               double storedCt = data.getDouble("jujutsuslop_secondary_ct");
               if (storedCt == -1.0D) {
               } else {
                  boolean currentlyActive = data.getBoolean("jujutsuslop_absorbed_active");
                  if (!currentlyActive) {
                     double originalCt = getPlayerCurseTechnique(player);
                     if (originalCt < 0.0D) {
                        return;
                     }

                     data.putDouble("jujutsuslop_original_ct", originalCt);
                     data.putBoolean("jujutsuslop_absorbed_active", true);
                     setPlayerCurseTechnique(player, storedCt);
                     player.sendSystemMessage(Component.literal("§dVessel's technique activated"));
                  } else {
                     double originalCt = data.contains("jujutsuslop_original_ct") ? data.getDouble("jujutsuslop_original_ct") : -1.0D;
                     if (originalCt < 0.0D) {
                        player.sendSystemMessage(Component.literal("§cCould not restore original technique"));
                        return;
                     }

                     data.putBoolean("jujutsuslop_absorbed_active", false);
                     setPlayerCurseTechnique(player, originalCt);
                     player.sendSystemMessage(Component.literal("§7Original technique restored"));
                  }
               }
            }
         }
      });
      ctx.setPacketHandled(true);
   }

   public static void toggle(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (data.contains("jujutsuslop_secondary_ct")) {
         double storedCt = data.getDouble("jujutsuslop_secondary_ct");
         if (storedCt != -1.0D) {
            boolean currentlyActive = data.getBoolean("jujutsuslop_absorbed_active");
            if (!currentlyActive) {
               double originalCt = getPlayerCurseTechnique(player);
               if (originalCt < 0.0D) {
                  return;
               }

               data.putDouble("jujutsuslop_original_ct", originalCt);
               data.putBoolean("jujutsuslop_absorbed_active", true);
               setPlayerCurseTechnique(player, storedCt);
            } else {
               double originalCt = data.contains("jujutsuslop_original_ct") ? data.getDouble("jujutsuslop_original_ct") : -1.0D;
               if (originalCt < 0.0D) {
                  return;
               }

               data.putBoolean("jujutsuslop_absorbed_active", false);
               setPlayerCurseTechnique(player, originalCt);
            }
         }
      }
   }

   public static double getPlayerCurseTechnique(ServerPlayer player) {
      try {
         Object vars = getVars(player);
         if (vars == null) {
            return -1.0D;
         } else {
            Field f = vars.getClass().getDeclaredField("PlayerCurseTechnique");
            f.setAccessible(true);
            return (Double) f.get(vars);
         }
      } catch (Exception e) {
         Abyss.LOGGER.warn("getPlayerCurseTechnique error: {}", e.getMessage());
         return -1.0D;
      }
   }

   public static boolean setPlayerCurseTechnique(ServerPlayer player, double ct) {
      try {
         Object vars = getVars(player);
         if (vars == null) {
            return false;
         } else {
            Field f = vars.getClass().getDeclaredField("PlayerCurseTechnique");
            f.setAccessible(true);
            f.set(vars, ct);
            Method sync = vars.getClass().getDeclaredMethod("syncPlayerVariables", Entity.class);
            sync.invoke(vars, player);
            return true;
         }
      } catch (Exception e) {
         Abyss.LOGGER.warn("setPlayerCurseTechnique error: {}", e.getMessage());
         return false;
      }
   }

   static Object getVars(ServerPlayer player) throws Exception {
      Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
      Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
      capField.setAccessible(true);
      Capability<Object> cap = (Capability<Object>) capField.get(null);
      return player.getCapability(cap, (Direction) null).orElse(null);
   }
}