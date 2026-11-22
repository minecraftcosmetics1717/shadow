
package com.ghosts.abyss.domain;

import net.minecraft.server.level.ServerPlayer;

public final class OpenDomainStateEvents {

    private OpenDomainStateEvents() {}

    public static void onPlayerTick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }

        var data = player.getPersistentData();

        boolean unlocked =
                data.getBoolean(OpenDomain.KEY_UNLOCKED);

        boolean selected =
                data.getBoolean(OpenDomain.KEY_TOGGLED);

        // Locked players must always have Closed selected.
        if (!unlocked) {
            if (selected) {
                data.putBoolean(OpenDomain.KEY_TOGGLED, false);
            }

            if (data.getBoolean(OpenDomain.KEY_ACTIVE)) {
                data.putBoolean(OpenDomain.KEY_ACTIVE, false);
            }

            return;
        }

        // The saved preference is the authoritative choice.
        // Do not change it based on cursed technique ID
        // or whether the player is crouching.
        boolean wanted = selected;

        if (data.getBoolean(OpenDomain.KEY_ACTIVE) != wanted) {
            data.putBoolean(OpenDomain.KEY_ACTIVE, wanted);
            OpenDomain.sync(player);
        }
    }
}
