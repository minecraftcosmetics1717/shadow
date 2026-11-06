
package com.ghosts.abyss.client;

import com.ghosts.abyss.block.ModBlocks;
import com.ghosts.abyss.domain.DomainDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
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
public final class YutaDomainFog {

    // Heavy dusty-pink fog: #D88BAF
    private static final float RED = 216.0F / 255.0F;
    private static final float GREEN = 139.0F / 255.0F;
    private static final float BLUE = 175.0F / 255.0F;

    // Heavy fog visibility distances.
    private static final float FOG_START = 18.0F;
    private static final float FOG_END = 28.0F;

    private YutaDomainFog() {}

    private static boolean isInsideYutaDomain() {

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return false;
        }

        if (!mc.level.dimension().equals(DomainDimension.ID)) {
            return false;
        }

        // Identify Yuta's domain using its gravel floor.
        BlockPos floorPos = new BlockPos(
                mc.player.getBlockX(),
                DomainDimension.CENTER_Y,
                mc.player.getBlockZ()
        );

        if (!mc.level.hasChunkAt(floorPos)) {
            return false;
        }

        BlockState state = mc.level.getBlockState(floorPos);

        return state.is(ModBlocks.DOMAIN_GRAVEL.get());
    }

    @SubscribeEvent
    public static void onFogDistance(
            ViewportEvent.RenderFog event
    ) {
        if (!isInsideYutaDomain()) return;

        event.setNearPlaneDistance(FOG_START);
        event.setFarPlaneDistance(FOG_END);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(
            ViewportEvent.ComputeFogColor event
    ) {
        if (!isInsideYutaDomain()) return;

        event.setRed(RED);
        event.setGreen(GREEN);
        event.setBlue(BLUE);
    }
}
