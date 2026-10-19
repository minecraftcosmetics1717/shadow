package com.ghosts.abyss.item;

import com.ghosts.abyss.NpcTechniqueMap;
import com.ghosts.abyss.network.PacketToggleAbsorbedTechnique;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class CursedFingerItem extends Item {
   public static final String TAG_OWNER = "finger_owner";
   public static final String TAG_OWNER_UUID = "finger_owner_uuid";
   private static final float HEALTH_THRESHOLD = 50.0F;

   public CursedFingerItem() {
      super(new Item.Properties().stacksTo(1));
   }

   @Override
   public Component getName(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      if (tag != null && tag.contains(TAG_OWNER)) {
         return Component.literal(tag.getString(TAG_OWNER) + "'s Finger");
      }
      return Component.literal("Sorcerer's Finger");
   }

   @Override
   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.literal("§7A vessel of condensed cursed energy."));
      tooltip.add(Component.literal("§7Use on a weakened sorcerer to assume their form and steal their technique."));
   }

   @Override
   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (player.level().isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (player instanceof ServerPlayer serverPlayer) {
         CompoundTag itemTag = stack.getTag();
         if (itemTag != null && itemTag.contains(TAG_OWNER_UUID)) {
            UUID ownerUuid = itemTag.getUUID(TAG_OWNER_UUID);
            if (!player.getUUID().equals(ownerUuid)) {
               player.sendSystemMessage(Component.literal("§cThis finger does not resonate with your cursed energy."));
               return InteractionResult.FAIL;
            }
         }

         double ct = NpcTechniqueMap.get(target.getClass());
         if (ct < 0.0D) {
            return InteractionResult.PASS;
         } else if (target.getHealth() >= HEALTH_THRESHOLD) {
            player.sendSystemMessage(Component.literal("§cTheir will to survive is too strong."));
            return InteractionResult.FAIL;
         } else {
            CompoundTag data = player.getPersistentData();
            if (data.getBoolean("jujutsuslop_absorbed_active")) {
               PacketToggleAbsorbedTechnique.setPlayerCurseTechnique(serverPlayer, ct);
            }

            data.putDouble("jujutsuslop_secondary_ct", ct);
            target.remove(RemovalReason.KILLED);

            if (!player.getAbilities().instabuild) {
               stack.shrink(1);
            }

            player.sendSystemMessage(Component.literal("§dA foreign technique takes root within you."));
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   public static ItemStack createFor(Player player) {
      Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("abyss", "cursed_finger"));
      ItemStack stack = new ItemStack(item != null ? item : new ItemStack(net.minecraft.world.item.Items.AIR).getItem());
      CompoundTag tag = stack.getOrCreateTag();
      tag.putString(TAG_OWNER, player.getGameProfile().getName());
      tag.putUUID(TAG_OWNER_UUID, player.getUUID());
      return stack;
   }
}