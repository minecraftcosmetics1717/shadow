
package com.ghosts.abyss.client.firstperson;

import com.ghosts.abyss.client.firstperson.poses.PiercingBloodPose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class FirstPersonHandRenderer {

    private FirstPersonHandRenderer() {}

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null
                || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        float progress =
                FirstPersonAnimationManager.getProgress(
                        event.getPartialTick()
                );

        if (progress <= 0.0F) {
            return;
        }

        // Replace the vanilla hand rendering.
        event.setCanceled(true);

        // Render both arms only once.
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        AbstractClientPlayer player = mc.player;

        var entityRenderer =
                mc.getEntityRenderDispatcher().getRenderer(player);

        if (!(entityRenderer instanceof PlayerRenderer renderer)) {
            return;
        }

        FirstPersonAnimationType animation =
                FirstPersonAnimationManager.getAnimation();

        // Keep the outgoing Piercing Blood pose visible
        // while its transition finishes.
        if (animation == FirstPersonAnimationType.PIERCING_BLOOD
                || progress > 0.0F) {

            PiercingBloodPose.render(
                    event.getPoseStack(),
                    event.getMultiBufferSource(),
                    event.getPackedLight(),
                    player,
                    renderer,
                    progress
            );
        }
    }
}
