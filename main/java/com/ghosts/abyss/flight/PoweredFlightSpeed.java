package com.ghosts.abyss.flight;

/** Shared client/server speed curve for abyss:flight prone flight. */
public final class PoweredFlightSpeed {
    /** Normal powered-flight speed in blocks per game tick. */
    public static final double NORMAL = 0.65D;
    /** Speed immediately after pressing Space while in powered flight. */
    public static final double BOOST = 1.10D;
    /** Keep full boost speed for two seconds. */
    public static final int BOOST_TICKS = 40;
    /** Ease back to normal speed over one additional second. */
    public static final int EASE_TICKS = 20;
    /** Four seconds between successful boost activations. */
    public static final int RECHARGE_TICKS = 80;

    private PoweredFlightSpeed() {}

    public static double atElapsedTicks(long elapsedTicks) {
        if (elapsedTicks < 0 || elapsedTicks >= BOOST_TICKS + EASE_TICKS) {
            return NORMAL;
        }
        if (elapsedTicks < BOOST_TICKS) {
            return BOOST;
        }
        double t = (elapsedTicks - BOOST_TICKS) / (double) EASE_TICKS;
        // Smoothstep avoids an abrupt speed change at either end of slowdown.
        double eased = t * t * (3.0D - 2.0D * t);
        return BOOST + (NORMAL - BOOST) * eased;
    }
}
