package com.ghosts.abyss.grab;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Emitted on the server after a successful grab's release finisher runs.
 * An interrupted grab deliberately doesn't fire this event.
 * Future custom grab moves can listen for their own skill IDs here.
 */
public final class GrabReleasedEvent extends Event {
    private final ServerPlayer grabber;
    private final LivingEntity target;
    private final GrabConfig config;

    public GrabReleasedEvent(ServerPlayer grabber, LivingEntity target, GrabConfig config) {
        this.grabber = grabber;
        this.target = target;
        this.config = config;
    }

    public ServerPlayer grabber() { return grabber; }
    public LivingEntity target() { return target; }
    public GrabConfig config() { return config; }
}
