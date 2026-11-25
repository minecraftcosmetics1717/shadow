package com.ghosts.abyss.client;

/**
 * Client-side state for the currently held grab input.
 *
 * Idle: vanilla physical attack icon.
 * Holding without ever catching a target: custom open-hand icon.
 * After catching any target on this press: vanilla physical attack icon
 * until the button is released, even if the grab times out first.
 *
 * Actual successful contact is only confirmed by the server.
 */
public final class GrabHudState {

    private static boolean holding;
    private static boolean attached;
    private static boolean caughtDuringThisHold;

    private GrabHudState() {}

    public static void beginHold() {
        holding = true;
        attached = false;
        caughtDuringThisHold = false;
    }

    public static void endHold() {
        holding = false;
        attached = false;
        caughtDuringThisHold = false;
    }

    /** Called only from the server-to-client grab-state packet. */
    public static void setAttached(boolean value) {
        if (!holding) return;
        attached = value;
        if (value) caughtDuringThisHold = true;
    }

    /** True only while actively looking for an enemy to grab. */
    public static boolean showReadyIcon() {
        return holding && !attached && !caughtDuringThisHold;
    }
}
