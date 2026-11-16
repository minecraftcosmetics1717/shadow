
package com.ghosts.abyss.phone;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abyss")
public final class PhoneTeleportRequests {

    public static final int DURATION_TICKS = 400;
    private static final int MAX_INCOMING = 5;

    // Recipient UUID -> requests addressed to that player.
    private static final Map<UUID, List<Request>> REQUESTS = new HashMap<>();

    public record Request(
            UUID senderId,
            UUID recipientId,
            String senderName,
            int expiresAt
    ) {}

    private PhoneTeleportRequests() {}

    public static void create(ServerPlayer sender, ServerPlayer recipient) {
        if (sender == recipient || PhoneSignal.blocked(sender)
                || !sender.isAlive() || !recipient.isAlive()) {
            return;
        }

        MinecraftServer server = sender.server;
        int now = server.getTickCount();

        List<Request> incoming = REQUESTS.computeIfAbsent(
                recipient.getUUID(), id -> new ArrayList<>()
        );

        // Remove expired requests and replace previous requests
        // from the same sender.
        incoming.removeIf(request ->
                request.expiresAt() <= now
                        || request.senderId().equals(sender.getUUID())
        );

        if (incoming.size() >= MAX_INCOMING) {
            // Keep the oldest existing requests rather than silently
            // displacing another player's request.
            return;
        }

        incoming.add(new Request(
                sender.getUUID(),
                recipient.getUUID(),
                sender.getGameProfile().getName(),
                now + DURATION_TICKS
        ));

        // Do not reveal the recipient's connection status.
        // The GUI synchronization will be added next.
    }

    public static List<Request> incoming(ServerPlayer recipient) {
        if (PhoneSignal.blocked(recipient) || !recipient.isAlive()) {
            return List.of();
        }

        List<Request> list = REQUESTS.get(recipient.getUUID());
        if (list == null) return List.of();

        int now = recipient.server.getTickCount();

        return list.stream()
                .filter(request -> request.expiresAt() > now)
                .toList();
    }

    public static void respond(
            ServerPlayer recipient,
            UUID senderId,
            boolean accept
    ) {
        // A disconnected or dead recipient cannot respond.
        if (PhoneSignal.blocked(recipient) || !recipient.isAlive()) {
            return;
        }

        List<Request> list = REQUESTS.get(recipient.getUUID());
        if (list == null) return;

        int now = recipient.server.getTickCount();
        Request selected = null;

        for (Request request : list) {
            if (request.senderId().equals(senderId)
                    && request.expiresAt() > now) {
                selected = request;
                break;
            }
        }

        if (selected == null) return;

        // The recipient's response consumes the request.
        list.remove(selected);

        if (list.isEmpty()) {
            REQUESTS.remove(recipient.getUUID());
        }

        if (!accept) {
            // No notification to the sender.
            return;
        }

        ServerPlayer sender = recipient.server
                .getPlayerList()
                .getPlayer(senderId);

        if (sender == null || !sender.isAlive()
                || PhoneSignal.blocked(sender)
                || PhoneSignal.blocked(recipient)
                || !recipient.isAlive()) {
            return;
        }

        // Sender teleports TO the accepting recipient.
        sender.teleportTo(
                recipient.serverLevel(),
                recipient.getX(),
                recipient.getY(),
                recipient.getZ(),
                recipient.getYRot(),
                recipient.getXRot()
        );
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        int now = server.getTickCount();

        Iterator<Map.Entry<UUID, List<Request>>> iterator =
                REQUESTS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, List<Request>> entry = iterator.next();

            entry.getValue().removeIf(request ->
                    request.expiresAt() <= now
            );

            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // The deceased recipient permanently loses incoming requests.
        // Nothing is sent to their senders.
        REQUESTS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Offline recipients cannot retain actionable requests.
        REQUESTS.remove(player.getUUID());

        // Requests originating from a departing player are invalid.
        // This cleanup does not notify any recipient.
        for (List<Request> list : REQUESTS.values()) {
            list.removeIf(request ->
                    request.senderId().equals(player.getUUID())
            );
        }

        REQUESTS.values().removeIf(List::isEmpty);
    }
}
