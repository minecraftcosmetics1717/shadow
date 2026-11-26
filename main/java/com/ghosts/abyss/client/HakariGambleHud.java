package com.ghosts.abyss.client;

import com.ghosts.abyss.network.PacketSyncHakariGamble;

/** Keep packet compatibility, but no longer render the top-left overlay. */
public final class HakariGambleHud {
    private HakariGambleHud() {}
    public static void update(PacketSyncHakariGamble packet) { }
}
