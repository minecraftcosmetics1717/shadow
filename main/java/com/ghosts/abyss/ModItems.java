package com.ghosts.abyss;

import com.ghosts.abyss.item.BindingVowItem;
import com.ghosts.abyss.item.CursedFingerItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
   public static final DeferredRegister<Item> ITEMS;
   public static final RegistryObject<Item> BINDING_VOW;
   public static final RegistryObject<Item> CURSED_FINGER;

   static {
      ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "abyss");
      BINDING_VOW = ITEMS.register("binding_vow", BindingVowItem::new);
      CURSED_FINGER = ITEMS.register("cursed_finger", CursedFingerItem::new);
   }
}
