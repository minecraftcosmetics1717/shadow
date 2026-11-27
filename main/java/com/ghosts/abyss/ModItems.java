
package com.ghosts.abyss;

import com.ghosts.abyss.item.BindingVowItem;
import com.ghosts.abyss.item.CursedFingerItem;
import com.ghosts.abyss.item.HeadwearItem;

import com.ghosts.abyss.item.TechniqueImprintItem;
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
   public static final RegistryObject<Item> IMPRINT_INFINITY =
           ITEMS.register("imprint_infinity",
                   () -> new TechniqueImprintItem(2));

   public static final RegistryObject<Item> IMPRINT_SHRINE =
           ITEMS.register("imprint_shrine",
                   () -> new TechniqueImprintItem(1));

   public static final RegistryObject<Item> IMPRINT_SHADOWS =
           ITEMS.register("imprint_shadows",
                   () -> new TechniqueImprintItem(6));

   public static final RegistryObject<Item> IMPRINT_SOUL =
           ITEMS.register("imprint_soul",
                   () -> new TechniqueImprintItem(15));

   public static final RegistryObject<Item> IMPRINT_COPY =
           ITEMS.register("imprint_copy",
                   () -> new TechniqueImprintItem(5));
}
