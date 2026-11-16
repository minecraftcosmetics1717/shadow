
package com.ghosts.abyss.phone;

import com.ghosts.abyss.client.phone.PhoneTeleportClient;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.TeleportRequestNotifyPacket;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abyss")
public final class PhoneTeleportRequests {

    public static final int DURATION_TICKS = 400;
    private static final int MAX_INCOMING = 5;

    // Recipient -> actionable requests.
    private static final Map<UUID, List<Request>> REQUESTS =
            new HashMap<>();

    // Sender -> recipient -> original expiry.
    // Used only to hide recipient death from the sender.
    private static final Map<UUID, Map<UUID, Long>> HIDDEN_OUTGOING =
            new HashMap<>();

    public record Request(
            UUID senderId,
            UUID recipientId,
            String senderName,
            long expiresAt
    ) {}

    private PhoneTeleportRequests() {}

    private static long now(MinecraftServer server) {
        return server.getTickCount();
    }

    private static boolean active(Request request, long tick) {
        return request.expiresAt() > tick;
    }

    private static boolean hasPending(
            UUID senderId, UUID recipientId, long tick
    ) {
        List<Request> requests = REQUESTS.get(recipientId);

        if (requests != null) {
            for (Request request : requests) {
                if (request.senderId().equals(senderId)
                        && active(request, tick)) {
                    return true;
                }
            }
        }

        return HIDDEN_OUTGOING
                .getOrDefault(senderId, Map.of())
                .getOrDefault(recipientId, 0L) > tick;
    }

    public static void create(ServerPlayer sender, ServerPlayer recipient) {
        if (sender == recipient
                || !sender.isAlive()
                || !recipient.isAlive()
                || PhoneSignal.blocked(sender)) {
            return;
        }

        MinecraftServer server = sender.server;
        long tick = now(server);

        // Clicking repeatedly cannot restart the countdown.
        if (hasPending(sender.getUUID(), recipient.getUUID(), tick)) {
            return;
        }

        List<Request> incoming = REQUESTS.computeIfAbsent(
                recipient.getUUID(), id -> new ArrayList<>()
        );

        incoming.removeIf(r -> !active(r, tick));

        if (incoming.size() >= MAX_INCOMING) return;

        incoming.add(new Request(
                sender.getUUID(),
                recipient.getUUID(),
                sender.getGameProfile().getName(),
                tick + DURATION_TICKS
        ));

        sync(sender);
        sync(recipient);
    }

    public static void respond(
            ServerPlayer recipient,
            UUID senderId,
            boolean accept
    ) {
        if (!recipient.isAlive() || PhoneSignal.blocked(recipient)) {
            return;
        }

        List<Request> requests = REQUESTS.get(recipient.getUUID());
        if (requests == null) return;

        long tick = now(recipient.server);
        Request selected = null;

        for (Request request : requests) {
            if (request.senderId().equals(senderId)
                    && active(request, tick)) {
                selected = request;
                break;
            }
        }

        if (selected == null) return;

        requests.remove(selected);
        if (requests.isEmpty()) {
            REQUESTS.remove(recipient.getUUID());
        }

        ServerPlayer sender = recipient.server
                .getPlayerList().getPlayer(senderId);

        sync(recipient);

        // Manual decline frees the sender to request again.
        if (!accept) {
            if (sender != null) sync(sender);
            return;
        }

        if (sender == null
                || !sender.isAlive()
                || PhoneSignal.blocked(sender)
                || !recipient.isAlive()
                || PhoneSignal.blocked(recipient)) {
            if (sender != null) sync(sender);
            return;
        }

        // Teleport requester TO accepting recipient.
        sender.teleportTo(
                recipient.serverLevel(),
                recipient.getX(),
                recipient.getY(),
                recipient.getZ(),
                recipient.getYRot(),
                recipient.getXRot()
        );

        sync(sender);
    }

    public static void sync(ServerPlayer player) {
        long tick = now(player.server);

        List<PhoneTeleportClient.Incoming> incoming = new ArrayList<>();
        List<PhoneTeleportClient.Outgoing> outgoing = new ArrayList<>();

        if (!PhoneSignal.blocked(player) && player.isAlive()) {
            for (Request request : REQUESTS.getOrDefault(
                    player.getUUID(), List.of())) {

                if (active(request, tick)) {
                    incoming.add(new PhoneTeleportClient.Incoming(
                            request.senderId(),
                            request.senderName(),
                            request.expiresAt()
                    ));
                }
            }
        }

        // Real outgoing requests.
        for (List<Request> requests : REQUESTS.values()) {
            for (Request request : requests) {
                if (request.senderId().equals(player.getUUID())
                        && active(request, tick)) {

                    outgoing.add(new PhoneTeleportClient.Outgoing(
                            request.recipientId(),
                            request.expiresAt()
                    ));
                }
            }
        }

        // Hidden countdowns after a recipient dies.
        HIDDEN_OUTGOING.getOrDefault(player.getUUID(), Map.of())
                .forEach((recipientId, expiresAt) -> {
                    if (expiresAt > tick) {
                        outgoing.add(new PhoneTeleportClient.Outgoing(
                                recipientId, expiresAt
                        ));
                    }
                });

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new TeleportRequestNotifyPacket(incoming, outgoing, tick)
        );
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        long tick = now(server);

        Set<UUID> changed = new HashSet<>();

        Iterator<Map.Entry<UUID, List<Request>>> iterator =
                REQUESTS.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();

            entry.getValue().removeIf(request -> {
                if (!active(request, tick)) {
                    changed.add(request.senderId());
                    changed.add(request.recipientId());
                    return true;
                }
                return false;
            });

            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }

        Iterator<Map.Entry<UUID, Map<UUID, Long>>> hiddenIterator =
                HIDDEN_OUTGOING.entrySet().iterator();

        while (hiddenIterator.hasNext()) {
            var entry = hiddenIterator.next();

            int before = entry.getValue().size();
            entry.getValue().values().removeIf(expiry -> expiry <= tick);

            if (entry.getValue().size() != before) {
                changed.add(entry.getKey());
            }

            if (entry.getValue().isEmpty()) {
                hiddenIterator.remove();
            }
        }

        for (UUID playerId : changed) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) sync(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID deadId = player.getUUID();
        MinecraftServer server = player.server;
        long tick = now(server);

        Set<UUID> changed = new HashSet<>();
        changed.add(deadId);

        // Recipient death: cancel actionable requests.
        // Keep the sender's original countdown as a decoy.
        List<Request> received = REQUESTS.remove(deadId);

        if (received != null) {
            for (Request request : received) {
                if (active(request, tick)) {
                    HIDDEN_OUTGOING.computeIfAbsent(
                            request.senderId(), id -> new HashMap<>()
                    ).put(deadId, request.expiresAt());
                }
            }
        }

        // Sender death: clear their requests from every recipient.
        for (List<Request> requests : REQUESTS.values()) {
            requests.removeIf(request -> {
                if (request.senderId().equals(deadId)) {
                    changed.add(request.recipientId());
                    return true;
                }
                return false;
            });
        }

        REQUESTS.values().removeIf(List::isEmpty);
        HIDDEN_OUTGOING.remove(deadId);

        for (UUID playerId : changed) {
            ServerPlayer other = server.getPlayerList().getPlayer(playerId);
            if (other != null) sync(other);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID playerId = player.getUUID();
        Set<UUID> changed = new HashSet<>();

        // Remove actionable requests addressed to the departing player.
        // Keep their senders' original countdowns private.
        List<Request> received = REQUESTS.remove(playerId);
        long tick = now(player.server);

        if (received != null) {
            for (Request request : received) {
                if (active(request, tick)) {
                    HIDDEN_OUTGOING.computeIfAbsent(
                            request.senderId(), id -> new HashMap<>()
                    ).put(playerId, request.expiresAt());
                }
            }
        }

        // A departing sender loses their outgoing requests.
        for (List<Request> requests : REQUESTS.values()) {
            requests.removeIf(request -> {
                if (request.senderId().equals(playerId)) {
                    changed.add(request.recipientId());
                    return true;
                }
                return false;
            });
        }

        REQUESTS.values().removeIf(List::isEmpty);
        HIDDEN_OUTGOING.remove(playerId);

        for (UUID id : changed) {
            ServerPlayer other = player.server
                    .getPlayerList().getPlayer(id);
            if (other != null) sync(other);
        }
    }
}
