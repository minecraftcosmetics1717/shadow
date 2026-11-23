package com.ghosts.abyss;

import com.ghosts.abyss.network.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
        modid = "abyss",
        bus = Bus.FORGE,
        value = {Dist.CLIENT}
)
public class ClientEventHandler {

   private static boolean wasRctOutputDown = false;
   private static boolean wasBlackFlashDown = false;
   private static boolean wasBurnoutDown = false;
   @SubscribeEvent
   public static void onKeyInput(InputEvent.Key event) {
      Minecraft mc = Minecraft.getInstance();

      if (mc.player != null && mc.screen == null) {
         if (KeyBindings.TOGGLE_OPEN_DOMAIN.consumeClick()) {
            if (!mc.player.getPersistentData().getBoolean("open_domain_unlocked")) {
               return;
            }
            ModNetwork.CHANNEL.sendToServer(new PacketToggleOpenDomain());
         }
      }
   }

   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase != TickEvent.Phase.END) return;

      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) {
         wasBlackFlashDown = KeyBindings.AT_WILL_BLACK_FLASH.isDown();

         // Release RCT Burnout when opening a screen
         // or disconnecting.
         if (wasBurnoutDown) {
            if (mc.player != null && mc.getConnection() != null) {
               ModNetwork.CHANNEL.sendToServer(
                       new PacketRctBurnoutHold(false)
               );
            }
            wasBurnoutDown = false;
         }

         if (wasRctOutputDown) {
            ModNetwork.CHANNEL.sendToServer(new PacketRctOutputHold(false));
            wasRctOutputDown = false;
         }
         return;
      }

      // Hold Reverse Cursed Technique Burnout.
      boolean burnoutDown = KeyBindings.RCT_BURNOUT.isDown();

      if (burnoutDown != wasBurnoutDown) {
         ModNetwork.CHANNEL.sendToServer(
                 new PacketRctBurnoutHold(burnoutDown)
         );

         wasBurnoutDown = burnoutDown;
      }

      boolean isDown = KeyBindings.RCT_OUTPUT.isDown();
      if (isDown != wasRctOutputDown) {
         ModNetwork.CHANNEL.sendToServer(new PacketRctOutputHold(isDown));
         wasRctOutputDown = isDown;
      }
// Black Flash timing key: only trigger on a fresh press
      boolean blackFlashDown = KeyBindings.AT_WILL_BLACK_FLASH.isDown();

      if (blackFlashDown && !wasBlackFlashDown) {
         ModNetwork.CHANNEL.sendToServer(new PacketBlackFlashInput());
      }

      wasBlackFlashDown = blackFlashDown;
   }
}