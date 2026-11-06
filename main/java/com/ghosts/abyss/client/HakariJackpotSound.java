
package com.ghosts.abyss.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

public class HakariJackpotSound
        extends AbstractTickableSoundInstance {

    public static final double MAX_DISTANCE = 80.0;

    private final UUID ownerId;
    private boolean finished;

    public HakariJackpotSound(
            LivingEntity owner,
            SoundEvent sound
    ) {
        super(
                sound,
                SoundSource.PLAYERS,
                RandomSource.create()
        );

        this.ownerId = owner.getUUID();

        // Repeat the sound while Jackpot is active.
        this.looping = true;
        this.delay = 0;

        // Disable Minecraft's default distance fade.
        // We calculate our own 80-block fade.
        this.attenuation = Attenuation.NONE;

        this.relative = false;
        this.pitch = 1.0F;

        this.x = owner.getX();
        this.y = owner.getY() + owner.getBbHeight() * 0.5;
        this.z = owner.getZ();

// Initialize at the correct volume immediately.
// This avoids starting the sound at zero volume.
        Minecraft mc = Minecraft.getInstance();

        double distance = mc.player != null
                ? mc.player.distanceTo(owner)
                : 80.0;

        this.volume = (float) Math.max(
                0.0,
                1.0 - distance / MAX_DISTANCE
        );
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();

        if (finished || mc.level == null || mc.player == null) {
            finish();
            return;
        }

        LivingEntity owner = null;

        for (LivingEntity entity :
                mc.level.getEntitiesOfClass(
                        LivingEntity.class,
                        mc.player.getBoundingBox().inflate(82.0)
                )) {

            if (entity.getUUID().equals(ownerId)) {
                owner = entity;
                break;
            }
        }

        if (owner == null || !owner.isAlive()) {
            finish();
            return;
        }

        if (!HakariJackpotMusic.hasJackpot(owner)) {
            finish();
            return;
        }

        // Follow the Jackpot user.
        this.x = owner.getX();
        this.y = owner.getY() + owner.getBbHeight() * 0.5;
        this.z = owner.getZ();

        // Distance from the listener.
        double distance = mc.player.distanceTo(owner);

        // Linear fade from 100% to 0% over 80 blocks.
        double fade = Math.max(
                0.0,
                1.0 - distance / MAX_DISTANCE
        );

        this.volume = (float) fade;

        // Stop outside the hearing radius.
        if (distance >= MAX_DISTANCE) {
            finish();
        }
    }

    public void finish() {
        if (!finished) {
            finished = true;
            stop();

            Minecraft.getInstance()
                    .getSoundManager()
                    .stop(this);
        }
    }

    public boolean isFinished() {
        return finished;
    }
}
