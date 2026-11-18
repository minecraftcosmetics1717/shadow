
package com.ghosts.abyss.domain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;

public final class DomainClashTimer {

    private static final String NBT_END_TICK =
            "abyss_domain_end_tick";

    // Refresh before JC's domain effect gets close
    // to expiring during a long clash.
    private static final int REFRESH_THRESHOLD = 1600;

    private DomainClashTimer() {}

    /**
     * Called every server tick while two or more
     * active domain casters are clashing.
     */
    public static void pauseDuringClash(
            ServerLevel level,
            DomainInstance inst,
            Map<UUID, LivingEntity> casters
    ) {
        if (level == null || inst == null) return;

        inst.clashTimerPaused = true;

        // Disable the owner's normal Abyss expiration.
        // This also pauses the regular active CE drain.
        LivingEntity owner = casters.get(inst.ownerId);

        if (owner != null) {
            owner.getPersistentData().putLong(
                    NBT_END_TICK,
                    0L
            );
        }

        MobEffect domainEffect =
                ForgeRegistries.MOB_EFFECTS.getValue(
                        DomainEvents.DOMAIN_EFFECT_ID
                );

        if (domainEffect == null) return;

        // Keep each active caster's domain effect alive
        // throughout the clash.
        for (LivingEntity caster : casters.values()) {

            if (caster == null || !caster.isAlive()) {
                continue;
            }

            MobEffectInstance current =
                    caster.getEffect(domainEffect);

            if (current == null) continue;

            if (current.getDuration() <= REFRESH_THRESHOLD) {

                caster.addEffect(new MobEffectInstance(
                        domainEffect,
                        DomainEvents.DOMAIN_DURATION_TICKS,
                        current.getAmplifier(),
                        false,
                        false,
                        true
                ));
            }
        }
    }

    /**
     * Give the surviving owner a fresh domain duration
     * after all other active casters have left the clash.
     */
    public static void resumeAfterClash(
            ServerLevel level,
            DomainInstance inst
    ) {
        if (level == null || inst == null) return;

        // Prevent repeated resets every server tick.
        if (!inst.clashTimerPaused) return;

        // Never restart a domain while multiple
        // domain participants remain registered.
        if (inst.participants.size() != 1) return;

        LivingEntity winner =
                DomainArenaService.findLivingIn(
                        level,
                        inst.ownerId
                );

        if (winner == null || !winner.isAlive()) {
            return;
        }

        MobEffect domainEffect =
                ForgeRegistries.MOB_EFFECTS.getValue(
                        DomainEvents.DOMAIN_EFFECT_ID
                );

        if (domainEffect == null) return;

        MobEffectInstance current =
                winner.getEffect(domainEffect);

        // Do not resurrect an already removed or
        // invalid domain effect.
        if (current == null) return;

        // Reset JC's domain duration to 2:40.
        winner.addEffect(new MobEffectInstance(
                domainEffect,
                DomainEvents.DOMAIN_DURATION_TICKS,
                current.getAmplifier(),
                false,
                false,
                true
        ));

        // Reset Abyss's independent expiration timer.
        long newEndTick =
                level.getServer().getTickCount()
                        + AbyssDomainCast.DURATION_TICKS;

        winner.getPersistentData().putLong(
                NBT_END_TICK,
                newEndTick
        );

        inst.clashTimerPaused = false;

        com.ghosts.abyss.Abyss.LOGGER.info(
                "[Abyss Domain] Clash ended. " +
                        "Winner={} Slot={} NewDuration={} ticks",
                winner.getUUID(),
                inst.slot,
                AbyssDomainCast.DURATION_TICKS
        );
    }
}
