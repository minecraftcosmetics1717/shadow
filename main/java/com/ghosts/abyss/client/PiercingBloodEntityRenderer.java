
package com.ghosts.abyss.client;

import com.ghosts.abyss.entity.PiercingBloodEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

public class PiercingBloodEntityRenderer
        extends EntityRenderer<PiercingBloodEntity> {

    private static final double RANGE = 48.0D;
    private static final double HALF_WIDTH = 0.075D;

    private static final double FIRST_PERSON_FORWARD = 0.85D;
    private static final double FIRST_PERSON_DOWN = 0.65D;

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("abyss", "textures/entity/piercing_blood.png");

    public PiercingBloodEntityRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(
            PiercingBloodEntity beam,
            float entityYaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight
    ) {

        Player owner = beam.getOwnerPlayer();

        if (owner == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        boolean localFirstPerson =
                owner == mc.player
                        && mc.options.getCameraType().isFirstPerson();

        Vec3 direction = owner.getViewVector(partialTick).normalize();

        Vec3 origin;

        if (localFirstPerson) {

            // Minecraft already positions this PoseStack
            // relative to the beam entity.
            // Convert the camera position into that space.

            Vec3 camera = mc.gameRenderer
                    .getMainCamera()
                    .getPosition();

            double entityX = Mth.lerp(
                    partialTick, beam.xOld, beam.getX()
            );
            double entityY = Mth.lerp(
                    partialTick, beam.yOld, beam.getY()
            );
            double entityZ = Mth.lerp(
                    partialTick, beam.zOld, beam.getZ()
            );

            Vec3 renderEntityPosition = new Vec3(
                    entityX, entityY, entityZ
            );

            Vec3 worldUp = new Vec3(0, 1, 0);
            Vec3 right = direction.cross(worldUp);

            if (right.lengthSqr() < 0.0001D) {
                right = new Vec3(1, 0, 0);
            } else {
                right = right.normalize();
            }

            Vec3 down = direction.cross(right)
                    .normalize()
                    .scale(FIRST_PERSON_DOWN);

            origin = camera
                    .add(direction.scale(FIRST_PERSON_FORWARD))
                    .add(down)
                    .subtract(renderEntityPosition);

        } else {

            // The entity is already positioned near
            // its owner's eyes on the server.
            origin = Vec3.ZERO;
        }

// Use the length calculated by the server.
        double beamLength = beam.getBeamLength();

        Vec3 end;

        if (localFirstPerson) {

            // The crosshair follows the camera's look direction.
            // Keep the beam's existing low starting position,
            // but aim its endpoint toward the crosshair.

            Vec3 camera = mc.gameRenderer
                    .getMainCamera()
                    .getPosition();

            org.joml.Vector3f look = mc.gameRenderer
                    .getMainCamera()
                    .getLookVector();

            Vec3 cameraForward = new Vec3(
                    look.x(),
                    look.y(),
                    look.z()
            );

            Vec3 targetWorld = camera.add(
                    cameraForward.scale(beamLength)
            );

            // Convert the crosshair target into the same
            // entity-relative coordinates as the beam origin.
            Vec3 entityPosition = new Vec3(
                    Mth.lerp(partialTick, beam.xOld, beam.getX()),
                    Mth.lerp(partialTick, beam.yOld, beam.getY()),
                    Mth.lerp(partialTick, beam.zOld, beam.getZ())
            );

            end = targetWorld.subtract(entityPosition);

        } else {

            // Keep third-person rendering exactly as before.
            end = origin.add(
                    direction.scale(beamLength)
            );
        }

        Matrix4f matrix = pose.last().pose();

        VertexConsumer vertices =
                buffers.getBuffer(RenderType.debugQuads());

        drawBeam(vertices, matrix, origin, end);

        super.render(
                beam,
                entityYaw,
                partialTick,
                pose,
                buffers,
                packedLight
        );
    }

    private static void drawBeam(
            VertexConsumer buffer,
            Matrix4f matrix,
            Vec3 start,
            Vec3 end
    ) {

        Vec3 forward = end.subtract(start).normalize();

        Vec3 reference = new Vec3(0, 1, 0);

        if (Math.abs(forward.dot(reference)) > 0.98D) {
            reference = new Vec3(1, 0, 0);
        }

        Vec3 right = forward.cross(reference).normalize();
        Vec3 up = right.cross(forward).normalize();

// Rotate the beam around its own length.
// 360 degrees every 20 ticks (1 second).
        double angle = Math.toRadians(
                (System.currentTimeMillis() % 1000L) * 0.36D
        );

        Vec3 rotatedRight = right.scale(Math.cos(angle))
                .add(up.scale(Math.sin(angle)));

        Vec3 rotatedUp = up.scale(Math.cos(angle))
                .subtract(right.scale(Math.sin(angle)));

        right = rotatedRight;
        up = rotatedUp;

        // Two perpendicular rectangles form a visible
        // cross-shaped beam from most viewing angles.
        drawQuad(
                buffer,
                matrix,
                start,
                end,
                right.scale(HALF_WIDTH)
        );

        drawQuad(
                buffer,
                matrix,
                start,
                end,
                up.scale(HALF_WIDTH)
        );
    }

    private static void drawQuad(
            VertexConsumer buffer,
            Matrix4f matrix,
            Vec3 start,
            Vec3 end,
            Vec3 offset
    ) {

        Vec3 a = start.subtract(offset);
        Vec3 b = start.add(offset);
        Vec3 c = end.add(offset);
        Vec3 d = end.subtract(offset);

        vertex(buffer, matrix, a);
        vertex(buffer, matrix, b);
        vertex(buffer, matrix, c);
        vertex(buffer, matrix, d);

        // Reverse side.
        vertex(buffer, matrix, d);
        vertex(buffer, matrix, c);
        vertex(buffer, matrix, b);
        vertex(buffer, matrix, a);
    }

    private static void vertex(
            VertexConsumer buffer,
            Matrix4f matrix,
            Vec3 point
    ) {
        buffer.vertex(
                        matrix,
                        (float) point.x,
                        (float) point.y,
                        (float) point.z
                )
                .color(127, 0, 0, 255)
                .endVertex();
    }
    @Override
    public boolean shouldRender(
            PiercingBloodEntity entity,
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ
    ) {
        return true;
    }
    @Override
    public ResourceLocation getTextureLocation(
            PiercingBloodEntity entity

    ) {
        return TEXTURE;
    }
}
