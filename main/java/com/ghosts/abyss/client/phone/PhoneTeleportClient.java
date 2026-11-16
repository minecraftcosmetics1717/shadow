
package com.ghosts.abyss.client.phone;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PhoneTeleportClient {

    public record Incoming(
            UUID senderId,
            String senderName,
            long expiresAt
    ) {}

    public record Outgoing(
            UUID recipientId,
            long expiresAt
    ) {}

    private static final List<Incoming> INCOMING = new ArrayList<>();
    private static final List<Outgoing> OUTGOING = new ArrayList<>();

    private static long serverTickAtSync;
    private static long clientTickAtSync;

    private PhoneTeleportClient() {}

    public static void update(
            List<Incoming> incoming,
            List<Outgoing> outgoing,
            long serverTick,
            long clientTick
    ) {
        INCOMING.clear();
        INCOMING.addAll(incoming);

        OUTGOING.clear();
        OUTGOING.addAll(outgoing);

        serverTickAtSync = serverTick;
        clientTickAtSync = clientTick;
    }

    public static void clear() {
        INCOMING.clear();
        OUTGOING.clear();
    }

    public static int secondsLeft(long expiresAt, long clientTick) {
        long elapsed = Math.max(0, clientTick - clientTickAtSync);
        long remaining = expiresAt - (serverTickAtSync + elapsed);

        return (int) Math.max(0, (remaining + 19) / 20);
    }

    public static List<Incoming> incoming(long clientTick) {
        return INCOMING.stream()
                .filter(r -> secondsLeft(r.expiresAt(), clientTick) > 0)
                .toList();
    }

    public static boolean hasOutgoing(UUID recipientId, long clientTick) {
        return OUTGOING.stream().anyMatch(r ->
                r.recipientId().equals(recipientId)
                        && secondsLeft(r.expiresAt(), clientTick) > 0
        );
    }

    public static int outgoingSeconds(UUID recipientId, long clientTick) {
        for (Outgoing r : OUTGOING) {
            if (r.recipientId().equals(recipientId)) {
                return secondsLeft(r.expiresAt(), clientTick);
            }
        }
        return 0;
    }
}
