package com.ghosts.abyss.item;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketOpenPactDialogue;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

public class BindingVowItem extends Item {

   public BindingVowItem() {
      super(new Item.Properties().stacksTo(1));
   }

   @Override
   public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
      lines.add(Component.translatable("tooltip.abyss.binding_vow"));
   }

   @Override
   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (player.level().isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (player instanceof ServerPlayer serverPlayer) {
         ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
         if (type != null && type.toString().equals("jujutsucraft:mahito")) {
            if (target.getHealth() > 49.0F) {
               serverPlayer.displayClientMessage(Component.translatable("message.abyss.binding_vow_too_healthy"), true);
               return InteractionResult.FAIL;
            } else if (serverPlayer.getPersistentData().getBoolean("perfect_body")) {
               serverPlayer.displayClientMessage(Component.translatable("message.abyss.binding_vow_already_bound"), true);
               return InteractionResult.FAIL;
            } else {
               if (target instanceof Mob mahitoMob) {
                  mahitoMob.setNoAi(true);
                  mahitoMob.setTarget(null);
               }

               serverPlayer.getPersistentData().putUUID("pact_target_uuid", target.getUUID());
               ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new PacketOpenPactDialogue());
               Abyss.LOGGER.debug("Opened pact dialogue for {} targeting Mahito {}", serverPlayer.getName().getString(), target.getUUID());

               return InteractionResult.SUCCESS;
            }
         } else {
            serverPlayer.displayClientMessage(Component.translatable("message.abyss.binding_vow_wrong_target"), true);
            return InteractionResult.FAIL;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      if (!level.isClientSide()) {
         player.displayClientMessage(Component.translatable("message.abyss.binding_vow_no_target"), true);
      }

      return InteractionResultHolder.pass(player.getItemInHand(hand));
   }
}