package com.ghosts.abyss.client.sixeyes;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SixEyesVisionHandler {

    private SixEyesVisionHandler() {}

    public static boolean shouldRender(Player player) {
        if (player == null || player.isSpectator()) {
            return false;
        }

        boolean hasSixEyes = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).map(data -> data.FlagSixEyes).orElse(false);

        if (!hasSixEyes) {
            return false;
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);

        if (!helmet.isEmpty()) {
            ResourceLocation id =
                    ForgeRegistries.ITEMS.getKey(helmet.getItem());

            if (id != null && "jujutsucraft".equals(id.getNamespace())) {
                switch (id.getPath()) {
                    case "sunglasses_helmet":
                    case "blindfold_bandage_helmet":
                    case "uniform_gojo_helmet":
                    case "mahoraga_wheel_helmet":
                        return false;
                }
            }
        }

        return true;
    }

    }