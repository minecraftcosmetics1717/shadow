
package com.ghosts.abyss.client;

public final class BlueControlClientState {

    private static boolean controlling = false;

    private BlueControlClientState() {}

    public static boolean isControlling() {
        return controlling;
    }

    public static void setControlling(boolean value) {
        controlling = value;
    }
}
