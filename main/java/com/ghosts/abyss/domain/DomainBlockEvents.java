package com.ghosts.abyss.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "abyss")
public class DomainBlockEvents {

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() == null) return;
        if (!event.getPlayer().level().dimension().equals(DomainDimension.ID)) return;

        BlockState state = event.getState();
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return;
        if ("jujutsucraft".equals(id.getNamespace())
                && (id.getPath().equals("jujutsu_barrier")
                || id.getPath().startsWith("domain"))) {
            event.setCanceled(true);
        }
    }
}