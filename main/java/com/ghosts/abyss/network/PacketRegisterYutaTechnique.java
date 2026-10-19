package com.ghosts.abyss.network;

import com.ghosts.abyss.AbilityScanHandler;
import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.YutaTechniqueRegistry;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class PacketRegisterYutaTechnique {
   private final int slot;

   public PacketRegisterYutaTechnique(int slot) {
      this.slot = slot;
   }

   public PacketRegisterYutaTechnique(FriendlyByteBuf buf) {
      this.slot = buf.readInt();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.slot);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player == null) {
            return;
         }

         // Check if the player has Yuta's Cursed Technique
         if (!YutaTechniqueRegistry.hasYutaCT(player)) {
            player.sendSystemMessage(Component.translatable("message.abyss.yuta_ct_required"), true);
            return;
         }

         // Check if player reached registration capacity
         if (!YutaTechniqueRegistry.canRegister(player)) {
            player.sendSystemMessage(Component.translatable("message.abyss.yuta_register_cap", 40), true);
            return;
         }

         Item copiedCT = ForgeRegistries.ITEMS.getValue(new ResourceLocation("jujutsucraft", "copied_cursed_technique"));
         if (copiedCT == null) {
            Abyss.LOGGER.warn("[JSlop] PacketRegisterYutaTechnique: copied_cursed_technique not found");
            return;
         }

         Inventory inventory = player.getInventory();
         if (this.slot >= 0 && this.slot < inventory.getContainerSize()) {
            ItemStack stack = inventory.getItem(this.slot);

            if (!stack.isEmpty() && stack.getItem() == copiedCT) {
               CompoundTag tag = stack.getOrCreateTag();
               double skill = tag.getDouble("skill");

               if (skill <= 0.0D) {
                  player.sendSystemMessage(Component.translatable("message.abyss.yuta_invalid_item"), true);
               } else if (player.experienceLevel < 1) {
                  player.sendSystemMessage(Component.translatable("message.abyss.not_enough_levels"), true);
               } else {
                  double cooldownTicks = tag.getDouble("COOLDOWN_TICKS");
                  String name = stack.getHoverName().getString();
                  String shikigamiName = tag.getString("SHIKIGAMI_NAME");
                  double shikigamiHp = tag.getDouble("SHIKIGAMI_HP");

                  double ceCost = AbilityScanHandler.getCostForSkill(player, skill);
                  if (ceCost <= 0.0D) {
                     ceCost = Math.min(1000.0D, Math.max(50.0D, cooldownTicks / 4.0D));
                  }

                  // Deduct 1 EXP level and register the technique
                  player.giveExperienceLevels(-1);
                  YutaTechniqueRegistry.register(player, skill, cooldownTicks, name, shikigamiName, shikigamiHp, ceCost);

                  stack.shrink(1);
                  PacketSyncYutaTechniques.sync(player);

                  player.sendSystemMessage(Component.translatable("message.abyss.yuta_registered", name), true);
                  Abyss.LOGGER.debug(
                          "[JSlop] {} registered Yuta technique: {} (skill={}, cooldown={})",
                          player.getScoreboardName(),
                          name,
                          skill,
                          cooldownTicks
                  );
               }
            } else {
               player.sendSystemMessage(Component.translatable("message.abyss.yuta_invalid_item"), true);
            }
         }
      });
      ctx.setPacketHandled(true);
   }
}