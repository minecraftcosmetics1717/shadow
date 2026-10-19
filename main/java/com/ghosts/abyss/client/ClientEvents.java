package com.ghosts.abyss.client;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.client.phone.PhoneScreen;
import com.ghosts.abyss.menu.ModMenus;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.OpenLifeCharmsPacket;
import com.ghosts.abyss.network.OpenShadowInventoryPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public class ClientEvents {

    public static final KeyMapping OPEN_SHADOW_INV = new KeyMapping(
            "key.abyss.shadow_inventory",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.abyss"
    );

    public static final KeyMapping OPEN_LIFE_CHARMS = new KeyMapping(
            "key.abyss.life_charms",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.abyss"
    );

    // I = Phone
    public static final KeyMapping OPEN_PHONE = new KeyMapping(
            "key.abyss.phone",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            "key.categories.abyss"
    );

    @Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(ModMenus.SHADOW_INVENTORY.get(), ShadowInventoryScreen::new);
                MenuScreens.register(ModMenus.LIFE_CHARMS.get(), LifeCharmsScreen::new);
                MenuScreens.register(ModMenus.INNERSELF_INVENTORY.get(), InnerselfInventoryScreen::new);
            });
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_SHADOW_INV);
            event.register(OPEN_LIFE_CHARMS);
            event.register(OPEN_PHONE);
        }
    }

    @Mod.EventBusSubscriber(modid = "abyss", value = Dist.CLIENT)
    public static class ForgeBusEvents {

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            if (OPEN_SHADOW_INV.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new OpenShadowInventoryPacket());
            }

            if (OPEN_LIFE_CHARMS.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new OpenLifeCharmsPacket());
            }

            if (OPEN_PHONE.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.screen == null) {
                    mc.setScreen(new PhoneScreen());
                }
            }
        }
    }
}