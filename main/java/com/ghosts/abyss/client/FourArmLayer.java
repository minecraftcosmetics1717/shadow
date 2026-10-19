package com.ghosts.abyss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ghosts.abyss.PerfectBodySync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class FourArmLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final double ARM_Y_OFFSET = 0.0D;
   private static final float SPREAD_Z = (float) Math.toRadians(40.0D);
   private static final float FORWARD_TILT = (float) Math.toRadians(25.0D);

   public FourArmLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   @Override
   public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
      if (hasPerfectBody(player)) {
         PlayerModel<AbstractClientPlayer> model = this.getParentModel();
         ResourceLocation skin = this.getTextureLocation(player);

         // Save original rotations
         float lxr = model.leftArm.xRot;
         float lyr = model.leftArm.yRot;
         float lzr = model.leftArm.zRot;

         float rxr = model.rightArm.xRot;
         float ryr = model.rightArm.yRot;
         float rzr = model.rightArm.zRot;

         float slxr = model.leftSleeve.xRot;
         float slyr = model.leftSleeve.yRot;
         float slzr = model.leftSleeve.zRot;

         float srxr = model.rightSleeve.xRot;
         float sryr = model.rightSleeve.yRot;
         float srzr = model.rightSleeve.zRot;

         // Apply offsets for extra arms
         model.leftArm.zRot -= SPREAD_Z;
         model.leftArm.xRot -= FORWARD_TILT;

         model.rightArm.zRot += SPREAD_Z;
         model.rightArm.xRot -= FORWARD_TILT;

         model.leftSleeve.zRot -= SPREAD_Z;
         model.leftSleeve.xRot -= FORWARD_TILT;

         model.rightSleeve.zRot += SPREAD_Z;
         model.rightSleeve.xRot -= FORWARD_TILT;

         poseStack.pushPose();
         poseStack.translate(0.0D, 0.0D, 0.0D);

         // Render extra pair of arms
         VertexConsumer armConsumer = buffers.getBuffer(RenderType.entitySolid(skin));
         model.leftArm.render(poseStack, armConsumer, packedLight, OverlayTexture.NO_OVERLAY);
         model.rightArm.render(poseStack, armConsumer, packedLight, OverlayTexture.NO_OVERLAY);

         // Render extra pair of sleeves
         VertexConsumer sleeveConsumer = buffers.getBuffer(RenderType.entityTranslucent(skin));
         model.leftSleeve.render(poseStack, sleeveConsumer, packedLight, OverlayTexture.NO_OVERLAY);
         model.rightSleeve.render(poseStack, sleeveConsumer, packedLight, OverlayTexture.NO_OVERLAY);

         poseStack.popPose();

         // Restore original arm/sleeve rotations
         model.leftArm.xRot = lxr;
         model.leftArm.yRot = lyr;
         model.leftArm.zRot = lzr;

         model.rightArm.xRot = rxr;
         model.rightArm.yRot = ryr;
         model.rightArm.zRot = rzr;

         model.leftSleeve.xRot = slxr;
         model.leftSleeve.yRot = slyr;
         model.leftSleeve.zRot = slzr;

         model.rightSleeve.xRot = srxr;
         model.rightSleeve.yRot = sryr;
         model.rightSleeve.zRot = srzr;
      }
   }

   private static boolean hasPerfectBody(AbstractClientPlayer player) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && player.getUUID().equals(mc.player.getUUID())) {
         Boolean synced = PerfectBodySync.MAP.get(player.getUUID());
         return synced != null ? synced : player.getPersistentData().getBoolean("perfect_body");
      } else {
         return Boolean.TRUE.equals(PerfectBodySync.MAP.get(player.getUUID()));
      }
   }
}