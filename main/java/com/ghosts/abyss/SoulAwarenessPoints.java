
package com.ghosts.abyss;

import com.ghosts.abyss.event.SoulAwarenessEvents;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class SoulAwarenessPoints {

    public static final String KEY_AVAILABLE =
            "abyss_soul_points_available";

    private SoulAwarenessPoints() {}

    public static int getAvailable(ServerPlayer player) {
        return Math.max(
                0,
                player.getPersistentData().getInt(KEY_AVAILABLE)
        );
    }

    public static void grant(
            ServerPlayer player,
            int amount
    ) {
        if (amount <= 0) {
            return;
        }

        CompoundTag data = player.getPersistentData();

        long total = (long) getAvailable(player) + amount;

        data.putInt(
                KEY_AVAILABLE,
                (int) Math.min(Integer.MAX_VALUE, total)
        );

        SoulAwarenessEvents.sync(player);
    }

    public static boolean tryUpgrade(ServerPlayer player) {

        int available = getAvailable(player);

        if (available <= 0) {
            return false;
        }

        int current = SoulAwareness.get(player);

        if (current >= SoulAwareness.MAX) {
            return false;
        }

        player.getPersistentData().putInt(
                KEY_AVAILABLE,
                available - 1
        );

        SoulAwareness.set(player, current + 1);

        SoulAwarenessEvents.sync(player);

        return true;
    }
}
