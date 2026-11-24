package com.ghosts.abyss.network;

import com.ghosts.abyss.effect.ModEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-authoritative permission and pose for abyss:flight-powered gliding. */
@Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PacketFlightToggle {

    // Preserve the existing NBT keys so updating the mod is straightforward.
    private static final String ACTIVE = "abyss_powered_elytra_flight";
    private static final String LAST_INPUT = "abyss_powered_elytra_last_input";
    private static final String WANTS_POWER = "abyss_powered_elytra_requested";

    private static final int TIMEOUT_TICKS = 40;
    private static final double FLIGHT_SPEED = 0.65D;

    private final boolean enabled;

    public PacketFlightToggle(boolean enabled) {
        this.enabled = enabled;
    }

    public PacketFlightToggle(FriendlyByteBuf buf) {
        this.enabled = buf.readBoolean();
    }

    // Keep the existing registration signatures in ModNetwork.java.
    public static void encode(PacketFlightToggle message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.enabled);
    }

    public static void handle(PacketFlightToggle message,
                              Supplier<NetworkEvent.Context> supplier) {
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
        AABB standingBox = player.getDimensions(Pose.STANDING)
                .makeBoundingBox(player.position());
        return player.level().noCollision(player, standingBox);
    }

    /** Does not depend on Uro; any holder of abyss:flight can use the mode. */
    public static boolean isActive(ServerPlayer player) {
        return player != null && player.getPersistentData().getBoolean(ACTIVE);
    }

    private static void start(ServerPlayer player) {
        if (isActive(player)) return;
        player.getPersistentData().putBoolean(ACTIVE, true);

        // This should only change on entry, not on each heartbeat/tick.
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        if (!player.isFallFlying()) {
            player.startFallFlying();
        }
        player.fallDistance = 0.0F;
    }

    private static void stop(ServerPlayer player, boolean restoreFlight) {
        if (!isActive(player)) return;

        player.getPersistentData().remove(ACTIVE);
        player.getPersistentData().remove(WANTS_POWER);
        player.getPersistentData().remove(LAST_INPUT);

        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
        if (restoreFlight && eligible(player) && !player.onGround()
                && canStand(player)) {
            player.getAbilities().flying = true;
        }
        player.onUpdateAbilities();
        player.fallDistance = 0.0F;
    }

    public static void setMode(ServerPlayer player, boolean enabled) {
        if (enabled && !eligible(player)) {
            if (isActive(player)) stop(player, false);
            return;
        }

        var data = player.getPersistentData();
        data.putBoolean(WANTS_POWER, enabled);
        data.putLong(LAST_INPUT, player.level().getGameTime());

        if (!enabled) {
            if (isActive(player) && canStand(player)) {
                stop(player, true);
            }
            // Otherwise retain the low hitbox until there is headroom.
            return;
        }

        // The client enters while flying in air. Once active, allow brushing
        // a tunnel floor to keep the low hitbox as long as it cannot stand.
        if (player.onGround() && (!isActive(player) || canStand(player))) {
            if (isActive(player)) stop(player, false);
            return;
        }
        start(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || !isActive(player)) {
            return;
        }

        if (!eligible(player)) {
            stop(player, false);
            return;
        }

        boolean standingRoom = canStand(player);
        if (player.onGround() && standingRoom) {
            stop(player, false);
            return;
        }

        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        if (now - data.getLong(LAST_INPUT) > TIMEOUT_TICKS) {
            data.putBoolean(WANTS_POWER, false);
        }

        boolean requested = data.getBoolean(WANTS_POWER);
        if (!requested && standingRoom) {
            stop(player, true);
            return;
        }

        // Start only if a different system actually cleared the pose.
        if (!player.isFallFlying()) {
            player.startFallFlying();
        }

        // The server uses client movement packets for position. Don't move
        // or teleport the ServerPlayer a second time here! Track matching
        // expected velocity for vanilla movement validation instead.
        Vec3 expected = requested && player.isSprinting()
                ? player.getLookAngle().normalize().scale(FLIGHT_SPEED)
                : Vec3.ZERO;
        player.setDeltaMovement(expected);
        player.fallDistance = 0.0F;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Do not leave active mode stored across sessions.
            if (isActive(player)) stop(player, false);
            player.getPersistentData().remove(WANTS_POWER);
            player.getPersistentData().remove(LAST_INPUT);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isActive(player)) {
            stop(player, false);
        }
    }
}
