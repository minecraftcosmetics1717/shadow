package com.ghosts.abyss;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.rika.RikaMenuTypes;

// Abyss imports
import com.ghosts.abyss.effect.ModEffects;
import com.ghosts.abyss.entity.RockEntity;
import com.ghosts.abyss.event.ShadowDiveTickHandler;
import com.ghosts.abyss.init.JujutsuSlopEntities;
import com.ghosts.abyss.item.RockItem;
import com.ghosts.abyss.menu.ModMenus;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("abyss")
public class Abyss {
   public static final String MOD_ID = "abyss";
   public static final Logger LOGGER = LogManager.getLogger();

   // ===== From Abyss =====
   public static final DeferredRegister<Item> ABYSS_ITEMS =
           DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

   public static final RegistryObject<Item> ROCK = ABYSS_ITEMS.register("rock",
           () -> new RockItem(new Item.Properties().stacksTo(16)));

   public static final GameRules.Key<GameRules.BooleanValue> STEVENSON_NOT_DESPAWN =
           GameRules.register("stevensonNotDespawn", GameRules.Category.MISC,
                   GameRules.BooleanValue.create(false));

   public Abyss() {
      IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

      // ===== jslops =====
      AbyssGamerules.init();
      ModItems.ITEMS.register(modBus);
      RikaMenuTypes.MENU_TYPES.register(modBus);

      // ===== Abyss =====
      ABYSS_ITEMS.register(modBus);
      JujutsuSlopEntities.ENTITY_TYPES.register(modBus);
      ModMenus.MENUS.register(modBus);
      ModEffects.register(modBus);

      // Network (already merged – only call once)
      ModNetwork.register();

      // Event listeners
      modBus.addListener(this::addCreativeTabs);
      modBus.addListener(this::onEntityAttributeCreation);

      MinecraftForge.EVENT_BUS.register(ShadowDiveTickHandler.class);

      LOGGER.info("Abyss initialising...");
   }

   private void addCreativeTabs(BuildCreativeModeTabContentsEvent event) {
      if (event.getTabKey() == CreativeModeTabs.COMBAT) {
         event.accept(ROCK);
      }
   }

   private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
      event.put(JujutsuSlopEntities.ROCK.get(), RockEntity.createAttributes().build());
   }
}