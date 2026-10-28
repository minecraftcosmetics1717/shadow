package com.ghosts.abyss.client.sixeyes;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SixEyesEntityRenderHook {

    private static final Map<UUID, SixEyesEntityColors.ThermalType>
            LAST_TYPES = new HashMap<>();

    private SixEyesEntityRenderHook() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null ||
                !SixEyesVisionHandler.shouldRender(mc.player)) {
            LAST_TYPES.clear();
            return;
        }

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;

            SixEyesEntityColors.ThermalType type =
                    SixEyesEntityColors.getType(living);

            SixEyesEntityColors.ThermalType previous =
                    LAST_TYPES.put(entity.getUUID(), type);

            if (previous != type) {
                System.out.println(
                        "[Abyss Six Eyes] "
                                + ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())
                                + " classified as "
                                + type
                );
            }
        }
    }
}