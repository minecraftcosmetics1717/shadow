package com.ghosts.abyss.network;

import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.OpenDomain;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.network.NetworkEvent;

public class PacketRequestUnlockOpenDomain {
   public PacketRequestUnlockOpenDomain() {
   }

   public PacketRequestUnlockOpenDomain(FriendlyByteBuf buf) {
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = (NetworkEvent.Context)contextSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            if (AbyssGamerules.isEnabled(player, AbyssGamerules.OPEN_DOMAIN)) {
               if (!isRestrictedCT(player)) {
                  OpenDomain.tryUnlock(player);
               }
            }
         }
      });
      ctx.setPacketHandled(true);
   }

   public static boolean isRestrictedCT(ServerPlayer player) {
      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         Capability<Object> cap = (Capability)capField.get((Object)null);
         Object vars = player.getCapability(cap, (Direction)null).orElse((Object)null);
         if (vars == null) {
            return false;
         } else {
            Field ctField = vars.getClass().getDeclaredField("PlayerCurseTechnique");
            ctField.setAccessible(true);
            double ct = (Double)ctField.get(vars);
            if (ct != (double)1.0F && ct != (double)18.0F && ct != (double)41.0F) {
               Field ct2Field = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
               ct2Field.setAccessible(true);
               double ct2 = (Double)ct2Field.get(vars);
               Field secondField = vars.getClass().getDeclaredField("SecondTechnique");
               secondField.setAccessible(true);
               boolean secondActive = (Boolean)secondField.get(vars);
               return secondActive && (ct2 == (double)1.0F || ct2 == (double)18.0F || ct2 == (double)41.0F);
            } else {
               return true;
            }
         }
      } catch (Exception var13) {
         return false;
      }
   }
}
