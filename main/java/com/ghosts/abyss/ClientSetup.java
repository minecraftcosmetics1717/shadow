package com.ghosts.abyss;

import java.lang.reflect.Field;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
        modid = "abyss",
        bus = Bus.MOD,
        value = {Dist.CLIENT}
)
public class ClientSetup {

   public static boolean isLocalPlayerCsm(Minecraft mc) {
      if (mc.player == null) {
         return false;
      }

      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         Capability<Object> cap = (Capability) capField.get(null);

         Object vars = mc.player.getCapability(cap, null).orElse(null);
         if (vars == null) {
            return false;
         }

         Field ct1 = vars.getClass().getDeclaredField("PlayerCurseTechnique");
         Field ct2 = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
         ct1.setAccessible(true);
         ct2.setAccessible(true);

         return (Double) ct1.get(vars) == 18.0 || (Double) ct2.get(vars) == 18.0;
      } catch (Exception e) {
         return false;
      }
   }

   @SubscribeEvent
   public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
      event.register(KeyBindings.OPEN_ADVANCED_MENU);
      event.register(KeyBindings.HOLLOW_WICKER_BASKET);
      event.register(KeyBindings.TOGGLE_OPEN_DOMAIN);
      event.register(KeyBindings.TRANSFORM_TO_CURSED_OBJECT);
      event.register(KeyBindings.TOGGLE_ABSORBED_TECHNIQUE);
      event.register(KeyBindings.GIVE_TECHNIQUE_STARTER);
      event.register(KeyBindings.OPEN_KEYBIND_MENU);
      event.register(KeyBindings.AT_WILL_BLACK_FLASH);
      event.register(KeyBindings.REGISTER_COPY);
      event.register(KeyBindings.RCT_OUTPUT);

      for (KeyMapping slot : KeyBindings.ABILITY_SLOTS) {
         event.register(slot);
      }
   }
}