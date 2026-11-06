
package com.ghosts.abyss.domain;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncHakariGamble;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abyss")
public final class HakariGambleManager {

    private HakariGambleManager() {}

    // Total roll duration: 3 seconds.
    private static final int ROLL_TICKS = 60;

    // Number of successful indicators needed.
    private static final int REQUIRED_INDICATORS = 2;

    // One state per Hakari domain owner.
    private static final Map<UUID, GambleState> STATES =
            new HashMap<>();

    private static class GambleState {

        DomainInstance domain;

        // Successful visual indicators (0, 1, or 2).
        int indicators;

        // Current roll.
        int rollTicks;
        boolean rolling;

        // Final slot numbers.
        int first;
        int second;
        int third;
    }

    // =========================================================
    // DOMAIN VALIDATION
    // =========================================================

    private static DomainInstance getHakariDomain(
            LivingEntity entity
    ) {
        if (entity == null || entity.level().isClientSide) {
            return null;
        }

        // Only allow gambling inside the Abyss domain dimension.
        if (!entity.level().dimension().equals(DomainDimension.ID)) {
            return null;
        }

        DomainInstance inst =
                DomainInstanceManager.getByOwner(entity.getUUID());

        // Hakari's domain ID is 29.
        if (inst == null || (int) inst.skillDomain != 29) {
            return null;
        }

        // Disable gambling during domain clashes.
        if (inst.participants.size() != 1
                || entity.getPersistentData()
                .getBoolean("abyss_domain_clashing")) {
            return null;
        }

        return inst;
    }

    public static boolean isHakariOwner(LivingEntity entity) {
        return getHakariDomain(entity) != null;
    }

    // =========================================================
    // TECHNIQUE REGISTRATION
    // =========================================================

    // Called when a Reserve Ball is thrown.
    public static void usedBall(LivingEntity entity) {
        registerMove(entity);
    }

    // Called when Shutter Doors is used.
    public static void usedDoors(LivingEntity entity) {
        registerMove(entity);
    }

    private static void registerMove(LivingEntity entity) {

        DomainInstance inst = getHakariDomain(entity);

        if (inst == null) {
            return;
        }

        GambleState state = STATES.computeIfAbsent(
                entity.getUUID(),
                id -> new GambleState()
        );

        // Reset state when a new domain session starts.
        if (state.domain != inst) {
            state = new GambleState();
            state.domain = inst;
            STATES.put(entity.getUUID(), state);
        }

        // Prevent registering moves while rolling.
        if (state.rolling) {
            return;
        }

        RandomSource random = entity.getRandom();

        // Each move has a 50% chance to earn one indicator.
        boolean success = random.nextBoolean();

        if (!success) {
            return;
        }

        state.indicators++;

        // Two indicators trigger a roll.
        if (state.indicators >= REQUIRED_INDICATORS) {
            startRoll(entity, state);
        }
    }

    // =========================================================
    // START ROLL
    // =========================================================

    private static void startRoll(
            LivingEntity entity,
            GambleState state
    ) {
        state.rolling = true;
        state.rollTicks = ROLL_TICKS;

        // Keep both indicators lit during the roll.
        state.indicators = REQUIRED_INDICATORS;

        RandomSource random = entity.getRandom();

        // Numbers range from 1 to 9.
        state.first = 1 + random.nextInt(9);

        // The first and third slots match.
        state.third = state.first;

        // The middle slot determines the jackpot.
        state.second = 1 + random.nextInt(9);
    }

    // =========================================================
    // SERVER TICK
    // =========================================================

    @SubscribeEvent
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var server =
                net.minecraftforge.server.ServerLifecycleHooks
                        .getCurrentServer();

        if (server == null) {
            return;
        }

        STATES.entrySet().removeIf(entry -> {

            UUID ownerId = entry.getKey();
            GambleState state = entry.getValue();

            DomainInstance inst =
                    DomainInstanceManager.getByOwner(ownerId);

            // Remove state if the domain has ended
            // or ownership/session has changed.
            if (inst == null
                    || inst != state.domain
                    || (int) inst.skillDomain != 29) {
                return true;
            }

            var level = server.getLevel(DomainDimension.ID);

            if (level == null) {
                return true;
            }

            var owner = level.getEntity(ownerId);

            if (!(owner instanceof LivingEntity living)
                    || !living.isAlive()) {
                return true;
            }

            // Reset progress during domain clashes.
            if (getHakariDomain(living) == null) {

                state.indicators = 0;
                state.rolling = false;
                state.rollTicks = 0;

                // Clear previous numbers.
                state.first = 0;
                state.second = 0;
                state.third = 0;

                return false;
            }

            // Nothing to update unless rolling.
            if (!state.rolling) {
                return false;
            }

            state.rollTicks--;

            // Roll has finished.
            if (state.rollTicks <= 0) {

                state.rolling = false;
                state.rollTicks = 0;

                // Reset indicators for the next attempt.
                state.indicators = 0;


// Jackpot calculation.
                boolean jackpot =
                        state.first == state.second
                                && state.second == state.third;

                if (jackpot) {

                    // Award Jackpot and end the domain.
                    awardJackpot(
                            living,
                            inst,
                            state.first
                    );

                    // Remove this gambling session.
                    return true;
                }

            }

            return false;
        });
    }

    private static void awardJackpot(
            LivingEntity owner,
            DomainInstance expectedDomain,
            int winningNumber
    ) {
        // Confirm this is still Hakari's active,
        // uncontested domain.
        if (getHakariDomain(owner) != expectedDomain) {
            return;
        }

        UUID ownerId = owner.getUUID();

        MobEffect jackpotEffect =
                ForgeRegistries.MOB_EFFECTS.getValue(
                        new ResourceLocation(
                                "jujutsucraft",
                                "jackpot"
                        )
                );

        if (jackpotEffect == null) {
            com.ghosts.abyss.Abyss.LOGGER.error(
                    "[Hakari] Jackpot effect not found!"
            );
            return;
        }

        // End the domain through Abyss's existing lifecycle.
        // This returns players, cleans the arena,
        // removes the domain effect, and applies cooldown.
        DomainLifecycle.end(owner);

        // Do not award if the domain wasn't successfully ended.
        if (DomainInstanceManager.getByOwner(ownerId)
                == expectedDomain) {
            return;
        }

        var server = owner.level().getServer();
        if (server == null) return;

        ServerLevel returnLevel =
                server.getLevel(expectedDomain.returnDim);

        if (returnLevel == null) {
            returnLevel = server.overworld();
        }

        // Resolve the owner after teleportation.
        // Teleporting a non-player entity can replace
        // its original Java entity instance.
        LivingEntity returnedOwner =
                DomainArenaService.findLivingIn(
                        returnLevel,
                        ownerId
                );

        if (returnedOwner == null) {
            com.ghosts.abyss.Abyss.LOGGER.warn(
                    "[Hakari] Could not find owner after jackpot teleport: {}",
                    ownerId
            );
            return;
        }

        // Preserve JC's winning-number mode behavior.
        if (winningNumber % 2 != 0) {
            returnedOwner.getPersistentData()
                    .putDouble("mode_hakari", 2.0D);
        } else {
            returnedOwner.getPersistentData()
                    .putDouble("mode_hakari", 1.0D);
        }

        // Apply the original 90-second Jackpot effect.
        returnedOwner.addEffect(
                new MobEffectInstance(
                        jackpotEffect,
                        5020,
                        0,
                        true,
                        true
                )
        );
// Play glass breaking at Hakari's returned position.
        returnLevel.playSound(
                null,
                returnedOwner.getX(),
                returnedOwner.getY(),
                returnedOwner.getZ(),
                SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS,
                1.5F,
                0.85F
        );
        com.ghosts.abyss.Abyss.LOGGER.info(
                "[Hakari] {} received Jackpot (winning number {})",
                returnedOwner.getName().getString(),
                winningNumber
        );
    }

    // =========================================================
    // MULTIPLAYER HUD SYNCHRONIZATION
    // =========================================================

    public static void syncToPlayer(ServerPlayer player) {

        DomainInstance inst = null;

        // Find the domain containing this player.
        if (player.level().dimension().equals(DomainDimension.ID)) {
            inst = DomainClash.findInstanceContaining(
                    player.position()
            );
        }

        // Only display HUD for an uncontested Hakari domain.
        boolean active =
                inst != null
                        && (int) inst.skillDomain == 29
                        && inst.participants.size() == 1;

        if (!active) {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new PacketSyncHakariGamble(
                            false,
                            false,
                            0,
                            0,
                            0,
                            0,
                            0
                    )
            );
            return;
        }

        GambleState state = STATES.get(inst.ownerId);

        // Ensure state belongs to this domain session.
        if (state != null && state.domain != inst) {
            state = null;
        }

        boolean rolling =
                state != null && state.rolling;

        int ticks =
                rolling ? state.rollTicks : 0;

        int first =
                state != null ? state.first : 0;

        int second =
                state != null ? state.second : 0;

        int third =
                state != null ? state.third : 0;

        int indicators =
                state != null ? state.indicators : 0;

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncHakariGamble(
                        true,
                        rolling,
                        ticks,
                        first,
                        second,
                        third,
                        indicators
                )
        );
    }
}
