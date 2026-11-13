
package com.ghosts.abyss.chat;

import net.minecraft.server.level.ServerPlayer;

public final class ProximityMessages {

    public static final double RANGE = 64.0;
    public static final double RANGE_SQUARED = RANGE * RANGE;

    private ProximityMessages() {}

    public static boolean canHear(
            ServerPlayer sender,
            ServerPlayer receiver
    ) {
        if (sender == receiver) {
            return true;
        }

        if (sender.level() != receiver.level()) {
            return false;
        }

        return sender.distanceToSqr(receiver) <= RANGE_SQUARED;
    }
}
