package com.ghosts.abyss.network;

import com.ghosts.abyss.csm.CsmRoster;

import java.util.List;
import java.util.function.Supplier;

import com.ghosts.abyss.csm.CsmSpiritData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class PacketCsmFeedFinger {
   private static final ResourceLocation FINGER_ID = new ResourceLocation("jujutsucraft", "sukuna_finger");
   private final int slotIdx;

   public PacketCsmFeedFinger(int slotIdx) {
      this.slotIdx = slotIdx;
   }

   public PacketCsmFeedFinger(FriendlyByteBuf buf) {
      this.slotIdx = buf.readInt();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.slotIdx);
   }

   public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
      NetworkEvent.Context ctx = ctxSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player == null) return;

         // --- ADD THIS BOUNDS CHECK ---
         List<CsmSpiritData> spirits = CsmRoster.getSpirits(player);
         if (this.slotIdx < 0 || this.slotIdx >= spirits.size()) {
            return; // Ignore malicious/invalid index sent by client
         }
         // -----------------------------

         // Existing code remains the same below:
         ItemStack held = player.getMainHandItem();
         Item fingerItem = ForgeRegistries.ITEMS.getValue(FINGER_ID);

         if (fingerItem != null && !held.isEmpty() && held.getItem() == fingerItem) {
            boolean success = CsmRoster.feedFinger(player, this.slotIdx);
            if (success) {
               held.shrink(1);
               player.sendSystemMessage(Component.literal("§6The spirit pulses with new power..."), true);
               CsmSync.sendRoster(player);
            } else {
               player.sendSystemMessage(Component.literal("§cThis spirit cannot receive any more fingers (or is not unique)"), true);
            }
         } else {
            player.sendSystemMessage(Component.literal("§cHold a Sukuna Finger to feed it to a spirit"), true);
         }
      });
      ctx.setPacketHandled(true);
   }}
