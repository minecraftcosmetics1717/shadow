package com.ghosts.abyss.network;

import com.ghosts.abyss.BlackFlashPoints;
import com.ghosts.abyss.AbyssGamerules;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketActivateAtWillBlackFlash {
   public static PacketActivateAtWillBlackFlash decode(FriendlyByteBuf buf) {
      return new PacketActivateAtWillBlackFlash();
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ServerPlayer player = ctx.getSender();
      if (player == null) {
         ctx.setPacketHandled(true);
      } else {
         ctx.enqueueWork(() -> {
            if (AbyssGamerules.isEnabled(player, AbyssGamerules.BLACK_FLASH)) {
               if (!BlackFlashPoints.isAtWillUnlocked(player)) {
                  player.sendSystemMessage(Component.literal("§cAt-will Black Flash not unlocked"));
               } else if (BlackFlashPoints.isAtWillCharged(player)) {
                  player.sendSystemMessage(Component.literal("§eAt-will is already charged"));
               } else if (!BlackFlashPoints.canActivateAtWill(player)) {
                  long remaining = BlackFlashPoints.getAtWillCooldownEnd(player) - player.level().getGameTime();
                  player.sendSystemMessage(Component.literal("§cAt-will Black Flash on cooldown. (" + Math.max(0L, remaining / 20L) + "s remaining)"));
               } else {
                  BlackFlashPoints.activateAtWill(player);
                  player.sendSystemMessage(Component.literal("§6At-will Black Flash — charged"));
                  BlackFlashPoints.sync(player);
               }
            }
         });
         ctx.setPacketHandled(true);
      }
   }
}