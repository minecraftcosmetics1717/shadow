package com.ghosts.abyss.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "abyss")
public class DomainBlockEvents {

    private static boolean isDomainShell(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null || !"jujutsucraft".equals(id.getNamespace())) return false;
        String p = id.getPath();
        return p.equals("jujutsu_barrier") || p.startsWith("domain");
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() == null) return;
        if (!event.getPlayer().level().dimension().equals(DomainDimension.ID)) return;
        if (isDomainShell(event.getState().getBlock())) {
            event.setCanceled(true);
        }
    }
}