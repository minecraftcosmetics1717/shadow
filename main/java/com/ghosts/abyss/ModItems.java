
package com.ghosts.abyss;

import com.ghosts.abyss.item.BindingVowItem;
import com.ghosts.abyss.item.CursedFingerItem;
import com.ghosts.abyss.item.HeadwearItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

   public static final DeferredRegister<Item> ITEMS =
           DeferredRegister.create(
                   ForgeRegistries.ITEMS,
                   Abyss.MOD_ID);
   public static final RegistryObject<Item> BINDING_VOW =
           ITEMS.register(
                   "binding_vow",
                   BindingVowItem::new);
   public static final RegistryObject<Item> CURSED_FINGER =
           ITEMS.register(
                   "cursed_finger",
                   CursedFingerItem::new);
   public static final RegistryObject<Item> BLINDFOLD =
           ITEMS.register(
                   "blindfold",
                   HeadwearItem::new);
   public static final RegistryObject<Item> BLINDFOLD_BANDAGES =
           ITEMS.register(
                   "blindfold_bandages",
                   HeadwearItem::new);
   public static final RegistryObject<Item> SUNGLASSES =
           ITEMS.register(
                   "sunglasses",
                   HeadwearItem::new);
}
