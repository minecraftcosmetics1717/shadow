package com.ghosts.abyss.network;

import com.ghosts.abyss.effect.ModEffects;
import com.ghosts.abyss.flight.PoweredFlightSpeed;
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

/** Server-side permission/pose and boost timing for any abyss:flight holder. */
@Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PacketFlightToggle {
    private static final String ACTIVE = "abyss_powered_elytra_flight";
    private static final String LAST_INPUT = "abyss_powered_elytra_last_input";
    private static final String WANTS_POWER = "abyss_powered_elytra_requested";
    private static final String BOOST_STARTED = "abyss_powered_elytra_boost_started";
    private static final String NEXT_BOOST = "abyss_powered_elytra_next_boost";

    private static final int TIMEOUT_TICKS = 40;

    private final boolean enabled;
    private final boolean boostPress;

    /** Retain compatibility with existing Java callers. */
    public PacketFlightToggle(boolean enabled) {
        this(enabled, false);
    }

    public PacketFlightToggle(boolean enabled, boolean boostPress) {
        this.enabled = enabled;
        this.boostPress = boostPress;
    }

    public PacketFlightToggle(FriendlyByteBuf buf) {
        this.enabled = buf.readBoolean();
        this.boostPress = buf.readBoolean();
    }

    // No ModNetwork registration changes are required.
    public static void encode(PacketFlightToggle message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.enabled);
        buf.writeBoolean(message.boostPress);
    }

    public static void handle(PacketFlightToggle message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                setMode(player, message.enabled, message.boostPress);
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

    public static boolean isActive(ServerPlayer player) {
        return player != null && player.getPersistentData().getBoolean(ACTIVE);
    }

    private static void start(ServerPlayer player) {
        if (isActive(player)) return;
        player.getPersistentData().putBoolean(ACTIVE, true);
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
        player.getPersistentData().remove(BOOST_STARTED);
        // Preserve NEXT_BOOST across quick exit/re-entry to avoid bypasses.

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
        setMode(player, enabled, false);
    }

    public static void setMode(ServerPlayer player, boolean enabled,
                               boolean boostPress) {
        if (enabled && !eligible(player)) {
            if (isActive(player)) stop(player, false);
            return;
        }

        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        data.putBoolean(WANTS_POWER, enabled);
        data.putLong(LAST_INPUT, now);

        if (!enabled) {
            if (isActive(player) && canStand(player)) {
                stop(player, true);
            }
            return; // Stay low if the standing hitbox would intersect blocks.
        }

        if (player.onGround() && (!isActive(player) || canStand(player))) {
            if (isActive(player)) stop(player, false);
            return;
        }
        start(player);

        // A one-shot Space press, validated by the server. Heartbeats never
        // restart the speed curve, and rapid presses cannot extend the boost.
        if (boostPress && isActive(player)
                && (!data.contains(NEXT_BOOST) || now >= data.getLong(NEXT_BOOST))) {
            data.putLong(BOOST_STARTED, now);
            data.putLong(NEXT_BOOST,
                    now + PoweredFlightSpeed.RECHARGE_TICKS);
        }
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

        if (!player.isFallFlying()) {
            player.startFallFlying();
        }

        double speed = PoweredFlightSpeed.NORMAL;
        if (data.contains(BOOST_STARTED)) {
            speed = PoweredFlightSpeed.atElapsedTicks(
                    now - data.getLong(BOOST_STARTED));
        }

        // Movement itself remains client-predicted and server-validated;
        // do not double-teleport the ServerPlayer on each tick.
        Vec3 expected = requested && player.isSprinting()
                ? player.getLookAngle().normalize().scale(speed)
                : Vec3.ZERO;
        player.setDeltaMovement(expected);
        player.fallDistance = 0.0F;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (isActive(player)) stop(player, false);
            player.getPersistentData().remove(WANTS_POWER);
            player.getPersistentData().remove(LAST_INPUT);
            player.getPersistentData().remove(BOOST_STARTED);
            player.getPersistentData().remove(NEXT_BOOST);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isActive(player)) {
            stop(player, false);
        }
    }
}
