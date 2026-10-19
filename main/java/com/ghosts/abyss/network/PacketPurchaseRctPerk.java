package com.ghosts.abyss.network;

import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.RctPoints;
import java.util.function.Supplier;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketPurchaseRctPerk {
   private final Perk perk;

   public PacketPurchaseRctPerk(Perk perk) {
      this.perk = perk;
   }

   public static PacketPurchaseRctPerk decode(FriendlyByteBuf buf) {
      return new PacketPurchaseRctPerk(buf.readEnum(Perk.class));
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
         if (!AbyssGamerules.isEnabled(player, AbyssGamerules.REVERSE_CURSED_TECHNIQUE)) {
            player.sendSystemMessage(Component.literal("§cReverse Cursed Technique is disabled by a server rule"));
            return;
         }

         if (player.getServer() == null) return;

         Advancement grade3Advancement = player.getServer().getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft", "sorcerer_grade_3"));
         boolean isGrade3OrHigher = grade3Advancement != null && player.getAdvancements().getOrStartProgress(grade3Advancement).isDone();

         if (isGrade3OrHigher) {
            boolean success = switch (this.perk) {
               case EFFICIENCY -> RctPoints.purchaseEfficiencyTier(player);
               case PROFICIENCY -> RctPoints.purchaseProficiencyTier(player);
               case OUTPUT -> RctPoints.purchaseOutputUnlock(player);
            };

            if (!success) {
               player.sendSystemMessage(Component.literal("§cNot enough RCT experience, or perk already unlocked/maxed"));
            }

            RctPoints.sync(player);
         } else {
            player.sendSystemMessage(Component.literal("§cYou must be at least Grade 3 to develop RCT"));
         }
      });

      ctx.setPacketHandled(true);
   }

   public enum Perk {
      EFFICIENCY,
      PROFICIENCY,
      OUTPUT
   }
}