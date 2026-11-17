
package com.ghosts.abyss.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ServerEntity.class)
public abstract class DomainServerEntityMixin {

    private static final String ABYSS_RESET_KEY =
            "abyss_tracking_reset_pending";

    @Shadow
    @Final
    private Entity entity;

    @Shadow
    @Final
    private VecDeltaCodec positionCodec;

    @Shadow
    @Final
    private Consumer<Packet<?>> broadcast;

    @Inject(
            method = "sendChanges",
            at = @At("HEAD")
    )
    private void abyss$resetPlayerTracking(CallbackInfo ci) {

        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        var data = player.getPersistentData();

        if (!data.getBoolean(ABYSS_RESET_KEY)) {
            return;
        }

        // Reset the internal position used for
        // Minecraft's relative entity movement packets.
        positionCodec.setBase(player.position());

        // Send the actual position, body rotation,
        // and pitch to tracking clients.
        broadcast.accept(
                new ClientboundTeleportEntityPacket(player)
        );

        // Head rotation is synchronized separately.
        byte headYaw = (byte) (
                player.getYHeadRot() * 256.0F / 360.0F
        );

        broadcast.accept(
                new ClientboundRotateHeadPacket(
                        player,
                        headYaw
                )
        );

        // Only reset once per requested transition.
        data.remove(ABYSS_RESET_KEY);
    }
}
