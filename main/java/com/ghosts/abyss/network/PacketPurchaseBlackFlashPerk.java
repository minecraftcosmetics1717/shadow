package com.ghosts.abyss.network;

import com.ghosts.abyss.BlackFlashPoints;
import com.ghosts.abyss.AbyssGamerules;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketPurchaseBlackFlashPerk {
   private final Perk perk;

   public PacketPurchaseBlackFlashPerk(Perk perk) {
      this.perk = perk;
   }

   public static PacketPurchaseBlackFlashPerk decode(FriendlyByteBuf buf) {
      return new PacketPurchaseBlackFlashPerk(buf.readEnum(Perk.class));
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeEnum(this.perk);
   }

   public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
      NetworkEvent.Context ctx = ctxSupplier.get();
      ServerPlayer player = ctx.getSender();

      if (player == null) {
         ctx.setPacketHandled(true);
         return;
      }

      ctx.enqueueWork(() -> {
         if (!AbyssGamerules.isEnabled(player, AbyssGamerules.BLACK_FLASH)) {
            player.sendSystemMessage(Component.literal("§cBlack Flash features have been disabled by a server rule"));
            return;
         }

         boolean success = switch (this.perk) {
            case CHANCE -> BlackFlashPoints.purchaseTier(player, BlackFlashPoints.KEY_CHANCE_TIER, BlackFlashPoints.MAX_CHANCE_TIER);
            case FATIGUE -> BlackFlashPoints.purchaseTier(player, BlackFlashPoints.KEY_FATIGUE_TIER, BlackFlashPoints.MAX_FATIGUE_TIER);
            case ENERGY -> BlackFlashPoints.purchaseTier(player, BlackFlashPoints.KEY_ENERGY_TIER, BlackFlashPoints.MAX_ENERGY_TIER);
            case BRAIN -> BlackFlashPoints.purchaseTier(player, BlackFlashPoints.KEY_BRAIN_TIER, BlackFlashPoints.MAX_BRAIN_TIER);
            case AT_WILL -> {
               if (BlackFlashPoints.getChanceTier(player) < 5) {
                  player.sendSystemMessage(Component.literal("§cAt-Will Black Flash requires maxed Black Flash Chance tier"));
                  yield false;
               } else {
                  yield BlackFlashPoints.purchaseAtWill(player);
               }
            }
         };

         if (!success) {
            player.sendSystemMessage(Component.literal("§cNot enough Black Flash experience, or perk already maxed"));
         } else if (this.perk == Perk.AT_WILL) {
            player.sendSystemMessage(Component.literal("§6At-will Black Flash unlocked!"));
         }

         BlackFlashPoints.sync(player);
      });

      ctx.setPacketHandled(true);
   }

   public enum Perk {
      CHANCE,
      FATIGUE,
      ENERGY,
      BRAIN,
      AT_WILL
   }
}