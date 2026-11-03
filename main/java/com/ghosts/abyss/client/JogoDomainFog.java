package com.ghosts.abyss.client;

import com.ghosts.abyss.block.ModBlocks;
import com.ghosts.abyss.domain.DomainDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class JogoDomainFog {

    // Fog color: #E88A30
    private static final float RED = 232.0F / 255.0F;
    private static final float GREEN = 120.0F / 255.0F;
    private static final float BLUE = 48.0F / 255.0F;

    // Visibility distances in blocks.
    private static final float FOG_START = 32.0F;
    private static final float FOG_END = 38.0F;

    private JogoDomainFog() {}

    private static boolean isInsideJogoDomain() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return false;
        }

        // Only inside Abyss's domain dimension.
        if (!mc.level.dimension().equals(DomainDimension.ID)) {
            return false;
        }

        // Domain arenas use a floor at CENTER_Y.
        // Check the floor beneath the player's X/Z position.
        BlockPos floorPos = new BlockPos(
                mc.player.getBlockX(),
                DomainDimension.CENTER_Y,
                mc.player.getBlockZ()
        );

        if (!mc.level.hasChunkAt(floorPos)) {
            return false;
        }

        BlockState state = mc.level.getBlockState(floorPos);
        Block block = state.getBlock();

        // Both floor materials identify Jogo's arena.
        return block == ModBlocks.DOMAIN_MOUNTAIN.get()
                || block == ModBlocks.DOMAIN_MOUNTAIN_MAGMA.get();
    }

    @SubscribeEvent
    public static void onFogDistance(ViewportEvent.RenderFog event) {
        if (!isInsideJogoDomain()) {
            return;
        }

        event.setNearPlaneDistance(FOG_START);
        event.setFarPlaneDistance(FOG_END);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isInsideJogoDomain()) {
            return;
        }

        event.setRed(RED);
        event.setGreen(GREEN);
        event.setBlue(BLUE);
    }
}
