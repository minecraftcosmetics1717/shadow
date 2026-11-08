
package com.ghosts.abyss.client;

import com.ghosts.abyss.network.PacketSyncPiercingBlood;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PiercingBloodBeamRenderer {

    private static final Map<UUID, BeamState> BEAMS =
            new HashMap<>();

    private static final int SIDES = 8;
    private static final long GROWTH_DURATION_NS =
            200_000_000L;
    private static final double MIN_GROWTH = 0.02D;
    private static final double FIRING_SENSITIVITY = 0.35D;
    private static Double originalSensitivity = null;
    private static final class BeamState {

        Vec3 start;
        Vec3 end;
        long lastUpdate;

        // When this beam first appeared.
        long startTime;

        BeamState(
                Vec3 start,
                Vec3 end,
                long lastUpdate
        ) {
            this.start = start;
            this.end = end;
            this.lastUpdate = lastUpdate;
            this.startTime = System.nanoTime();
        }
    }


    private PiercingBloodBeamRenderer() {}

    public static void update(PacketSyncPiercingBlood packet) {

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) {
            BEAMS.clear();
            return;
        }

        if (!packet.active) {
            BEAMS.remove(packet.playerId);
            return;
        }


        long now = mc.level.getGameTime();

        BeamState existing = BEAMS.get(packet.playerId);

        if (existing != null) {

            // Update endpoints without restarting growth.
            existing.start = packet.start;
            existing.end = packet.end;
            existing.lastUpdate = now;

        } else {

            // A newly created beam begins growing.
            BEAMS.put(
                    packet.playerId,
                    new BeamState(
                            packet.start,
                            packet.end,
                            now
                    )
            );
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {

        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            BEAMS.clear();
            return;
        }

        if (BEAMS.isEmpty()) {
            return;
        }

        long now = mc.level.getGameTime();

        // Remove beams that stopped receiving updates.
        Iterator<Map.Entry<UUID, BeamState>> iterator =
                BEAMS.entrySet().iterator();

        while (iterator.hasNext()) {
            BeamState state = iterator.next().getValue();

            if (now - state.lastUpdate > 5) {
                iterator.remove();
            }
        }

        if (BEAMS.isEmpty()) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        PoseStack pose = event.getPoseStack();
        pose.pushPose();

        pose.translate(
                -cameraPos.x,
                -cameraPos.y,
                -cameraPos.z
        );

        MultiBufferSource.BufferSource buffers =
                mc.renderBuffers().bufferSource();

        VertexConsumer vertices =
                buffers.getBuffer(RenderType.debugQuads());

        Matrix4f matrix = pose.last().pose();

        for (Map.Entry<UUID, BeamState> entry : BEAMS.entrySet()) {

            BeamState state = entry.getValue();

            Vec3 beamStart = state.start;
            Vec3 beamEnd = state.end;

            boolean localFirstPerson =
                    entry.getKey().equals(mc.player.getUUID())
                            && mc.options.getCameraType().isFirstPerson();


            if (localFirstPerson) {

                Vec3 forward = mc.player.getLookAngle().normalize();

                // Use the camera's vertical direction.
                Vec3 cameraUp = new Vec3(
                        0.0D,
                        1.0D,
                        0.0D
                );

                Vec3 right = forward.cross(cameraUp);

                if (right.lengthSqr() > 0.0001D) {
                    right = right.normalize();
                } else {
                    right = new Vec3(1.0D, 0.0D, 0.0D);
                }

                Vec3 down = right.cross(forward).normalize()
                        .scale(-1.0D);

                // Keep the visible beam lower on screen.
                beamStart = mc.player.getEyePosition()
                        .add(forward.scale(0.85D))
                        .add(down.scale(0.40D));

                // Keep the visual endpoint aligned with
                // the player's current aiming direction.
                double distance = Math.max(
                        0.1D,
                        state.end.distanceTo(state.start)
                );

                beamEnd = mc.player.getEyePosition()
                        .add(forward.scale(distance));
            }


            double elapsed = Math.max(
                    0.0D,
                    (System.nanoTime() - state.startTime)
                            / (double) GROWTH_DURATION_NS
            );

            double progress = Math.min(1.0D, elapsed);

// Ease out: starts fast and finishes smoothly.
            double growth = 1.0D -
                    Math.pow(1.0D - progress, 3.0D);

            growth = Math.max(MIN_GROWTH, growth);

// Extend the visible endpoint gradually.
            beamEnd = beamStart.add(
                    beamEnd.subtract(beamStart).scale(growth)
            );

            drawBeam(
                    vertices,
                    matrix,
                    beamStart,
                    beamEnd,
                    0.09F,
                    120, 0, 12, 255
            );}

        buffers.endBatch(RenderType.debugQuads());

        pose.popPose();
    }

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        boolean firing = false;

        if (mc.level != null && mc.player != null) {

            BeamState state = BEAMS.get(
                    mc.player.getUUID()
            );

            if (state != null) {

                long age = mc.level.getGameTime()
                        - state.lastUpdate;

                firing = age >= 0 && age <= 5;
            }
        }

        if (firing) {

            if (originalSensitivity == null) {

                originalSensitivity =
                        mc.options.sensitivity().get();

                mc.options.sensitivity().set(
                        originalSensitivity
                                * FIRING_SENSITIVITY
                );
            }

        } else if (originalSensitivity != null) {

            mc.options.sensitivity().set(
                    originalSensitivity
            );

            originalSensitivity = null;
        }
    }

    private static void drawBeam(
            VertexConsumer buffer,
            Matrix4f matrix,
            Vec3 start,
            Vec3 end,
            float radius,
            int red,
            int green,
            int blue,
            int alpha
    ) {

        Vec3 direction = end.subtract(start);

        if (direction.lengthSqr() < 0.0001D) {
            return;
        }

        Vec3 forward = direction.normalize();

        Vec3 reference = Math.abs(forward.y) > 0.95D
                ? new Vec3(1, 0, 0)
                : new Vec3(0, 1, 0);

        Vec3 right = forward.cross(reference).normalize();
        Vec3 up = forward.cross(right).normalize();

        for (int i = 0; i < SIDES; i++) {

            double angle1 = Math.PI * 2.0D * i / SIDES;
            double angle2 = Math.PI * 2.0D * (i + 1) / SIDES;

            Vec3 offset1 = right.scale(Math.cos(angle1) * radius)
                    .add(up.scale(Math.sin(angle1) * radius));

            Vec3 offset2 = right.scale(Math.cos(angle2) * radius)
                    .add(up.scale(Math.sin(angle2) * radius));

            Vec3 a = start.add(offset1);
            Vec3 b = start.add(offset2);
            Vec3 c = end.add(offset2);
            Vec3 d = end.add(offset1);

// RenderType.lightning() uses quadrilaterals.
// Each cylinder side is one four-vertex quad.
            vertex(buffer, matrix, a, red, green, blue, alpha);
            vertex(buffer, matrix, b, red, green, blue, alpha);
            vertex(buffer, matrix, c, red, green, blue, alpha);
            vertex(buffer, matrix, d, red, green, blue, alpha);
        }
    }

    private static void vertex(
            VertexConsumer buffer,
            Matrix4f matrix,
            Vec3 position,
            int red,
            int green,
            int blue,
            int alpha
    ) {

        buffer.vertex(
                matrix,
                (float) position.x,
                (float) position.y,
                (float) position.z
        ).color(red, green, blue, alpha).endVertex();
    }
}
