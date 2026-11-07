package com.ghosts.abyss.blood;

import net.minecraft.world.entity.player.Player;

public final class DeathWombPainting {

    private static final String KEY =
            "abyss_death_womb_painting";

    private DeathWombPainting() {}

    public static boolean is(Player player) {
        return player.getPersistentData().getBoolean(KEY);
    }

    public static void set(Player player, boolean value) {
        if (!player.level().isClientSide) {
            player.getPersistentData().putBoolean(KEY, value);
        }
    }
}