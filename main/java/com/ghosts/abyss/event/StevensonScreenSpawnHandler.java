package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import net.mcreator.jujutsucraft.entity.StevensonScreenEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss")
public class StevensonScreenSpawnHandler {

    private static final int MIN_DAYTIME_SKY_LIGHT = 8;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof StevensonScreenEntity) {

            if (!event.loadedFromDisk()) {
                Level level = event.getLevel();
                
                if (!level.isClientSide()) {
                    BlockPos pos = event.getEntity().blockPosition();

                    boolean isDayTime = level.isDay();
                    int skyLight = level.getBrightness(LightLayer.SKY, pos);

                    if (!isDayTime || skyLight < MIN_DAYTIME_SKY_LIGHT) {
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
}