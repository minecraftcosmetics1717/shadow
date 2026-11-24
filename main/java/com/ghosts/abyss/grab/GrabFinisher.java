package com.ghosts.abyss.grab;

/** The effect applied when a completed grab releases its target. */
public enum GrabFinisher {
    NONE,
    THROW,
    DAMAGE,
    DAMAGE_AND_THROW;

    public boolean dealsDamage() {
        return this == DAMAGE || this == DAMAGE_AND_THROW;
    }

    public boolean throwsTarget() {
        return this == THROW || this == DAMAGE_AND_THROW;
    }
}
