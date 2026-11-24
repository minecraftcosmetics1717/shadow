package com.ghosts.abyss.grab;

/**
 * Settings supplied by an individual grab move. Durations are game ticks;
 * velocities and damage are Minecraft units.
 *
 * The move decides when contact occurs. GrabManager handles everything
 * after a successful grab begins.
 */
public record GrabConfig(
        int durationTicks,
        float contactDamage,
        float releaseDamage,
        double throwPower,
        int skillId,
        int cooldownTicks,
        GrabFinisher finisher
) {
    public GrabConfig {
        if (durationTicks <= 0 || contactDamage < 0 || releaseDamage < 0
                || throwPower < 0 || cooldownTicks < 0 || finisher == null) {
            throw new IllegalArgumentException("Invalid grab settings");
        }
    }
}
