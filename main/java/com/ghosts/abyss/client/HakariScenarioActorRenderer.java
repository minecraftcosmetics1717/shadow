package com.ghosts.abyss.client;

import com.ghosts.abyss.entity.HakariScenarioActor;
import com.ghosts.abyss.init.JujutsuSlopEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HakariScenarioActorRenderer extends EntityRenderer<HakariScenarioActor> {
    private static final ResourceLocation SKIN_1 = new ResourceLocation("abyss", "textures/entity/hakari_actor_1.png");
    private static final ResourceLocation SKIN_2 = new ResourceLocation("abyss", "textures/entity/hakari_actor_2.png");
    private final PlayerModel<LivingEntity> model;

    public HakariScenarioActorRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        // EntityModel defaults to young; these are adult-sized player models.
        model.young = false;
        shadowRadius = 0.35F;
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(JujutsuSlopEntities.HAKARI_SCENARIO_ACTOR.get(), HakariScenarioActorRenderer::new);
    }
    @Override public ResourceLocation getTextureLocation(HakariScenarioActor actor) {
        return actor.skin() == 1 ? SKIN_2 : SKIN_1;
    }
    @Override public void render(HakariScenarioActor actor, float yaw, float partialTick,
                                 PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        if (actor.sitting()) pose.translate(0.0D, -0.70D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        pose.scale(-1.0F, -1.0F, 1.0F);
        // Match the vanilla adult player renderer scale.
        pose.scale(0.9375F, 0.9375F, 0.9375F);
        pose.translate(0.0D, -1.501D, 0.0D);
        float swing = actor.walking() ? Mth.cos((actor.tickCount + partialTick) * 0.85F) * 1.05F : 0.0F;
        model.rightArm.xRot = swing;
        model.leftArm.xRot = -swing;
        model.rightLeg.xRot = -swing;
        model.leftLeg.xRot = swing;
        if (actor.sitting()) {
            model.rightLeg.xRot = -Mth.HALF_PI;
            model.leftLeg.xRot = -Mth.HALF_PI;
            model.rightArm.xRot = -0.22F;
            model.leftArm.xRot = -0.22F;
        }
        model.head.xRot = actor.sitting() ? 0.15F : actor.getXRot() * Mth.DEG_TO_RAD;
        model.head.yRot = 0.0F;
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
        model.renderToBuffer(pose, buffer.getBuffer(model.renderType(getTextureLocation(actor))),
                packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        pose.popPose();
        super.render(actor, yaw, partialTick, pose, buffer, packedLight);
    }
}
