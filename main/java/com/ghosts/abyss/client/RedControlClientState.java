package com.ghosts.abyss.client;

public final class RedControlClientState {
    private static boolean controlling;

    private RedControlClientState() {}

    public static boolean isControlling() {
        return controlling;
    }

    public static void setControlling(boolean value) {
        controlling = value;
    }
}
