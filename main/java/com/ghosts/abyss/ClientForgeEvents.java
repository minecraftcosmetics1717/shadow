package com.ghosts.abyss;

import com.ghosts.abyss.gui.KeybindScreen;
import com.ghosts.abyss.gui.YutaTab;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketActivateAbilitySlot;
import com.ghosts.abyss.network.PacketActivateAtWillBlackFlash;
import com.ghosts.abyss.network.PacketActivateHWB;
import com.ghosts.abyss.network.PacketGiveCursedTechniqueStarter;
import com.ghosts.abyss.network.PacketRegisterYutaTechnique;
import com.ghosts.abyss.network.PacketToggleAbsorbedTechnique;
import com.ghosts.abyss.network.PacketToggleOpenDomain;
import com.ghosts.abyss.network.PacketTransformToCursedObject;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(
        modid = "abyss",
        bus = Bus.FORGE,
        value = {Dist.CLIENT}
)
class ClientForgeEvents {
   private static boolean hwbWasHeld = false;
   private static boolean hwbToggled = false;
   private static final boolean[] slotWasDown = new boolean[8];

   @SubscribeEvent
   public static void onKeyInput(InputEvent.Key event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;

      // Advanced Menu
      if (KeyBindings.OPEN_ADVANCED_MENU.consumeClick()) {
         if (getClientCT() == -1.0) {
            mc.player.displayClientMessage(Component.literal("§cYou are marrow and bone, not skin and flesh"), true);
         } else {
            mc.setScreen(new AdvancedJujutsuScreen());
         }
      }

      // Keybind Menu
      if (KeyBindings.OPEN_KEYBIND_MENU.consumeClick()) {
         mc.setScreen(new KeybindScreen());
      }

      // Toggle Open Domain
      if (KeyBindings.TOGGLE_OPEN_DOMAIN.consumeClick()) {
         if (!mc.player.getPersistentData().getBoolean("open_domain_unlocked")) return;

         boolean hasDomainEffect = mc.player.getActiveEffects().stream().anyMatch(e -> {
            ResourceLocation loc = ForgeRegistries.MOB_EFFECTS.getKey(e.getEffect());
            return loc != null && loc.toString().equals("jujutsucraft:domain_expansion");
         });
         if (hasDomainEffect) return;

         ModNetwork.CHANNEL.sendToServer(new PacketToggleOpenDomain());
      }

      // Transform to Cursed Object
      if (KeyBindings.TRANSFORM_TO_CURSED_OBJECT.consumeClick()) {
         ModNetwork.CHANNEL.sendToServer(new PacketTransformToCursedObject());
      }

      // Toggle Absorbed Technique
      if (KeyBindings.TOGGLE_ABSORBED_TECHNIQUE.consumeClick()) {
         ModNetwork.CHANNEL.sendToServer(new PacketToggleAbsorbedTechnique());
      }

      // Give Technique Starter
      if (KeyBindings.GIVE_TECHNIQUE_STARTER.consumeClick()) {
         ModNetwork.CHANNEL.sendToServer(new PacketGiveCursedTechniqueStarter());
      }

      // At-Will Black Flash
      if (KeyBindings.AT_WILL_BLACK_FLASH.consumeClick() && BlackFlashPoints.isClientAtWillUnlocked()) {
         ModNetwork.CHANNEL.sendToServer(new PacketActivateAtWillBlackFlash());
      }

      // Register Yuta Copy
      if (KeyBindings.REGISTER_COPY.consumeClick()) {
         if (!YutaTab.isLocalPlayerYuta()) return;

         ItemStack held = mc.player.getMainHandItem();
         Item copiedCT = ForgeRegistries.ITEMS.getValue(new ResourceLocation("jujutsucraft", "copied_cursed_technique"));

         if (held.isEmpty() || copiedCT == null || held.getItem() != copiedCT || held.getOrCreateTag().getDouble("skill") <= 0.0) {
            mc.player.displayClientMessage(Component.literal("Hold a Copied Cursed Technique item to register it"), true);
            return;
         }

         if (ClientYutaTechniqueCache.getCount() >= 40) {
            mc.player.displayClientMessage(Component.literal("Your technique library is full"), true);
            return;
         }

         if (mc.player.experienceLevel < 1) {
            mc.player.displayClientMessage(Component.literal("You need at least 1 experience level to register"), true);
            return;
         }

         ModNetwork.CHANNEL.sendToServer(new PacketRegisterYutaTechnique(mc.player.getInventory().selected));
      }
   }

   private static void fireAbilityClientSide(Minecraft mc, int selectValue, int targetCT, boolean press) {
      if (mc.player == null) return;

      try {
         mc.player.getPersistentData().putBoolean("PRESS_Z", press);

         double skill;
         if (targetCT == 5 && selectValue >= 100) {
            int regIdx = selectValue - 100;
            List<ClientYutaTechniqueCache.Entry> entries = ClientYutaTechniqueCache.getAll();
            skill = regIdx < entries.size() ? entries.get(regIdx).skill() : 0.0;
         } else if (targetCT == 18 && selectValue >= 1000) {
            skill = 1812.0;
         } else {
            skill = (double) targetCT * 100.0 + (double) selectValue;
         }

         if (press) {
            try {
               Class<?> resetClass = Class.forName("net.mcreator.jujutsucraft.procedures.ResetCounterProcedure");
               resetClass.getMethod("execute", Entity.class).invoke(null, mc.player);
            } catch (Exception ignored) {}

            if (skill != 0.0) {
               mc.player.getPersistentData().putDouble("skill", skill);
            }
         }

         if (targetCT == -1) {
            boolean needsFwd = selectValue == 5 || selectValue == 7;
            if (needsFwd) {
               mc.player.getPersistentData().putBoolean("jslop_fwd_inject", true);
            }
         }
      } catch (Exception e) {
         Abyss.LOGGER.warn("[JSlop] fireAbilityClientSide exception: {}", e.getMessage());
      }
   }

   private static double getClientCT() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return -99.0;

      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         Capability<Object> cap = (Capability) capField.get(null);

         Object vars = mc.player.getCapability(cap, null).orElse(null);
         if (vars == null) return -99.0;

         Field ctField = vars.getClass().getDeclaredField("PlayerCurseTechnique");
         ctField.setAccessible(true);
         return (Double) ctField.get(vars);
      } catch (Exception e) {
         return -99.0;
      }
   }

   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase != Phase.END) return;

      Minecraft mc = Minecraft.getInstance();

      // SAFETY CHECK: When leaving the world, reset local states WITHOUT sending packets
      if (mc.player == null || mc.level == null || mc.getConnection() == null) {
         hwbWasHeld = false;
         hwbToggled = false;
         for (int i = 0; i < 8; i++) {
            slotWasDown[i] = false;
         }
         return;
      }

      boolean perfectBody = mc.player.getPersistentData().getBoolean("perfect_body");

      if (mc.screen != null) {
         if (hwbWasHeld) {
            // Only send packet if connection is active and player exists
            ModNetwork.CHANNEL.sendToServer(new PacketActivateHWB(false));
            hwbWasHeld = false;
         }
         return;
      }

      boolean unlocked = mc.player.getPersistentData().getBoolean("unlocked_hwb");
      if (!unlocked) {
         if (hwbWasHeld || hwbToggled) {
            ModNetwork.CHANNEL.sendToServer(new PacketActivateHWB(false));
            hwbWasHeld = false;
            hwbToggled = false;
         }
      } else if (perfectBody) {
         // Toggle mode for Perfect Body
         if (KeyBindings.HOLLOW_WICKER_BASKET.consumeClick()) {
            hwbToggled = !hwbToggled;
            ModNetwork.CHANNEL.sendToServer(new PacketActivateHWB(hwbToggled));
         }
         if (hwbWasHeld) {
            ModNetwork.CHANNEL.sendToServer(new PacketActivateHWB(false));
            hwbWasHeld = false;
         }
      } else {
         // Hold mode
         if (hwbToggled) {
            ModNetwork.CHANNEL.sendToServer(new PacketActivateHWB(false));
            hwbToggled = false;
         }

         boolean hwbIsHeld = KeyBindings.HOLLOW_WICKER_BASKET.isDown();
         if (hwbIsHeld && !hwbWasHeld) {
            ModNetwork.CHANNEL.sendToServer(new PacketActivateHWB(true));
         } else if (!hwbIsHeld && hwbWasHeld) {
            ModNetwork.CHANNEL.sendToServer(new PacketActivateHWB(false));
         }
         hwbWasHeld = hwbIsHeld;
      }

      handleCsmKeys(mc);

      // Ability slots
      for (int i = 0; i < 8; ++i) {
         boolean isDown = KeyBindings.ABILITY_SLOTS[i].isDown();
         if (isDown != slotWasDown[i]) {
            KeybindConfig.Ability ability = KeybindConfig.getSlot(i);
            if (!ability.isUnbound()) {
               fireAbilityClientSide(mc, ability.selectValue(), ability.ct(), isDown);
               ModNetwork.CHANNEL.sendToServer(new PacketActivateAbilitySlot(ability.selectValue(), ability.ct(), isDown));
            }
            slotWasDown[i] = isDown;
         }
      }
   }

   private static void handleCsmKeys(Minecraft mc) {
      if (mc.player == null) return;
      if (!ClientSetup.isLocalPlayerCsm(mc)) return;

      // Unused key listeners removed (PacketCsmActivateAbility / CsmAbilityScreen deleted)
   }

   private static String getClientCrosshairEntityUuid(Minecraft mc) {
      return mc.crosshairPickEntity != null ? mc.crosshairPickEntity.getStringUUID() : "";
   }
}