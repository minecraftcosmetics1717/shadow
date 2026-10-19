package com.ghosts.abyss.rika;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RikaMenuTypes {
   public static final DeferredRegister<MenuType<?>> MENU_TYPES;
   public static final RegistryObject<MenuType<RikaInventoryMenu>> RIKA_INVENTORY;

   static {
      MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "abyss");
      RIKA_INVENTORY = MENU_TYPES.register("rika_inventory", () -> IForgeMenuType.create(RikaInventoryMenu::new));
   }
}
