package com.ghosts.abyss;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;

public class KeyBindings {
   public static final KeyMapping OPEN_ADVANCED_MENU;
   public static final KeyMapping HOLLOW_WICKER_BASKET;
   public static final KeyMapping TOGGLE_OPEN_DOMAIN;
   public static final KeyMapping TRANSFORM_TO_CURSED_OBJECT;
   public static final KeyMapping TOGGLE_ABSORBED_TECHNIQUE;
   public static final KeyMapping GIVE_TECHNIQUE_STARTER;
   public static final KeyMapping OPEN_KEYBIND_MENU;
   public static final KeyMapping AT_WILL_BLACK_FLASH;
   public static final KeyMapping REGISTER_COPY;
   public static final KeyMapping RCT_OUTPUT;          // ← new
   public static final KeyMapping[] ABILITY_SLOTS;

   static {
      OPEN_ADVANCED_MENU = new KeyMapping("key.abyss.open_menu", KeyConflictContext.IN_GAME, Type.KEYSYM, 74, "key.categories.abyss");
      HOLLOW_WICKER_BASKET = new KeyMapping("key.abyss.hollow_wicker_basket", KeyConflictContext.IN_GAME, Type.KEYSYM, 86, "key.categories.abyss");
      TOGGLE_OPEN_DOMAIN = new KeyMapping("key.abyss.toggle_open_domain", KeyConflictContext.IN_GAME, Type.KEYSYM, -1, "key.categories.abyss");
      TRANSFORM_TO_CURSED_OBJECT = new KeyMapping("key.abyss.transform_to_cursed_object", KeyConflictContext.IN_GAME, Type.KEYSYM, -1, "key.categories.abyss");
      TOGGLE_ABSORBED_TECHNIQUE = new KeyMapping("key.abyss.toggle_absorbed_technique", KeyConflictContext.IN_GAME, Type.KEYSYM, -1, "key.categories.abyss");
      GIVE_TECHNIQUE_STARTER = new KeyMapping("key.abyss.give_technique_starter", KeyConflictContext.IN_GAME, Type.KEYSYM, -1, "key.categories.abyss");
      OPEN_KEYBIND_MENU = new KeyMapping("key.abyss.open_keybind_menu", KeyConflictContext.IN_GAME, Type.KEYSYM, -1, "key.categories.abyss");
      AT_WILL_BLACK_FLASH = new KeyMapping("key.abyss.at_will_black_flash", KeyConflictContext.IN_GAME, Type.KEYSYM, -1, "key.categories.abyss");
      REGISTER_COPY = new KeyMapping("key.abyss.register_copy", KeyConflictContext.IN_GAME, Type.KEYSYM, -1, "key.categories.abyss");
      RCT_OUTPUT = new KeyMapping("key.abyss.rct_output", KeyConflictContext.IN_GAME, Type.KEYSYM, -1, "key.categories.abyss");

      ABILITY_SLOTS = new KeyMapping[8];

      for (int i = 0; i < 8; ++i) {
         ABILITY_SLOTS[i] = new KeyMapping("key.abyss.ability_slot_" + (i + 1), KeyConflictContext.IN_GAME, Type.KEYSYM, -1, "key.categories.abyss");
      }
   }
}