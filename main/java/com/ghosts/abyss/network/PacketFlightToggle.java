
package com.ghosts.abyss.network;

import com.ghosts.abyss.effect.ModEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = "abyss")
public final class PacketFlightToggle {

    private static final String ACTIVE =
            "abyss_powered_elytra_flight";

    private static final String LAST_INPUT =
            "abyss_powered_elytra_last_input";

    private final boolean enabled;

    public PacketFlightToggle(boolean enabled) {
        this.enabled = enabled;
    }

    public PacketFlightToggle(FriendlyByteBuf buf) {
        this.enabled = buf.readBoolean();
    }

    public static void encode(
            PacketFlightToggle message,
            FriendlyByteBuf buf
    ) {
        buf.writeBoolean(message.enabled);
    }

    public static void handle(
            PacketFlightToggle message,
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null) {
                setMode(player, message.enabled);
            }
        });

        context.setPacketHandled(true);
    }

    private static boolean eligible(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && player.hasEffect(ModEffects.FLIGHT.get())
                && player.getAbilities().mayfly
                && !player.isPassenger()
                && !player.isInWaterOrBubble()
                && !player.isInLava();
    }

    private static boolean canStand(ServerPlayer player) {
        AABB box = player.getDimensions(Pose.STANDING)
                .makeBoundingBox(player.position());

        return player.level().noCollision(player, box);
    }

    public static void setMode(
            ServerPlayer player,
            boolean enabled
    ) {
        if (!enabled) {
            stop(player, false);
            return;
        }

        if (!eligible(player) || player.onGround()) {
            return;
        }

        player.getPersistentData().putBoolean(ACTIVE, true);
        player.getPersistentData().putLong(
                LAST_INPUT,
                player.level().getGameTime()
        );

        if (!player.isFallFlying()) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.startFallFlying();
        }

        player.fallDistance = 0.0F;
    }

    private static void stop(
            ServerPlayer player,
            boolean force
    ) {
        if (!player.getPersistentData().getBoolean(ACTIVE)) {
            return;
        }

        // Do not expand the player's hitbox into solid blocks.
        if (!force && !canStand(player)) {
            return;
        }

        player.getPersistentData().remove(ACTIVE);
        player.getPersistentData().remove(LAST_INPUT);

        if (player.isFallFlying()) {
            player.stopFallFlying();
        }

        if (eligible(player) && !player.onGround()) {
            player.getAbilities().flying = true;
        }

        player.onUpdateAbilities();
        player.fallDistance = 0.0F;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        if (!player.getPersistentData().getBoolean(ACTIVE)) {
            return;
        }

        if (!eligible(player)) {
            stop(player, true);
            return;
        }

        long now = player.level().getGameTime();
        long lastInput = player.getPersistentData()
                .getLong(LAST_INPUT);

        if (now - lastInput > 15) {
            stop(player, false);
            return;
        }

        if (player.onGround() && canStand(player)) {
            stop(player, true);
            return;
        }

        if (!player.isFallFlying()) {
            player.startFallFlying();
        }

        player.fallDistance = 0.0F;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getPersistentData().remove(ACTIVE);
            player.getPersistentData().remove(LAST_INPUT);
        }
    }
}
