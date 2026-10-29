package com.ghosts.abyss.client.sixeyes;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SixEyesMaskRenderHook {

    private static final Set<ResourceLocation> LOGGED_TYPES =
            new HashSet<>();

    private SixEyesMaskRenderHook() {}

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null
                || mc.level == null
                || !SixEyesVisionHandler.shouldRender(mc.player)) {
            LOGGED_TYPES.clear();
            return;
        }

        LivingEntity entity = event.getEntity();

        if (entity == mc.player) {
            return;
        }

        ResourceLocation id =
                ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        if (id == null) {
            return;
        }

        if (id.getNamespace().equals("jujutsucraft")) {
            String path = id.getPath();

            if (path.equals("entity_attack_slash")
                    || path.equals("entity_attack_strike")
                    || path.equals("entity_attack_sweep")) {
                return;
            }
        }

        if (LOGGED_TYPES.add(id)) {
            System.out.println(
                    "[Abyss Six Eyes Mask] Renderer detected: "
                            + id
                            + " | Classification: "
                            + SixEyesEntityColors.getType(entity)
            );
        }
    }
}
