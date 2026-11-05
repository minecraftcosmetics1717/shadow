
package com.ghosts.abyss.domain;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abyss")
public final class HakariGambleManager {

    private HakariGambleManager() {}

    private static final int ROLL_TICKS = 60;

    private static final Map<UUID, GambleState> STATES =
            new HashMap<>();

    private static class GambleState {
        DomainInstance domain;
        boolean ballUsed;
        boolean doorsUsed;
        int rollTicks;
        boolean rolling;
        int first;
        int second;
        int third;
    }

    private static DomainInstance getHakariDomain(
            LivingEntity entity
    ) {
        if (entity == null || entity.level().isClientSide) {
            return null;
        }

        if (!entity.level().dimension().equals(DomainDimension.ID)) {
            return null;
        }

        DomainInstance inst =
                DomainInstanceManager.getByOwner(entity.getUUID());

        if (inst == null || (int) inst.skillDomain != 29) {
            return null;
        }

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

    public static void usedBall(LivingEntity entity) {
        registerMove(entity, true);
    }

    public static void usedDoors(LivingEntity entity) {
        registerMove(entity, false);
    }

    private static void registerMove(
            LivingEntity entity,
            boolean ball
    ) {
        DomainInstance inst = getHakariDomain(entity);
        if (inst == null) return;

        GambleState state = STATES.computeIfAbsent(
                entity.getUUID(), id -> new GambleState()
        );

        if (state.domain != inst) {
            state = new GambleState();
            state.domain = inst;
            STATES.put(entity.getUUID(), state);
        }

        if (state.rolling) return;

        if (ball) {
            state.ballUsed = true;
        } else {
            state.doorsUsed = true;
        }

        if (state.ballUsed && state.doorsUsed) {
            startRoll(entity, state);
        } else {
            sendStatus(entity, ball
                    ? "Reserve Ball registered"
                    : "Shutter Doors registered");
        }
    }

    private static void startRoll(
            LivingEntity entity,
            GambleState state
    ) {
        state.ballUsed = false;
        state.doorsUsed = false;
        state.rolling = true;
        state.rollTicks = ROLL_TICKS;

        RandomSource random = entity.getRandom();

        state.first = 1 + random.nextInt(9);
        state.second = state.first;
        state.third = 1 + random.nextInt(9);

        sendStatus(entity, "Idle Death Gamble: Rolling...");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = net.minecraftforge.server.ServerLifecycleHooks
                .getCurrentServer();
        if (server == null) return;

        STATES.entrySet().removeIf(entry -> {
            var inst = DomainInstanceManager.getByOwner(
                    entry.getKey()
            );

            if (inst == null
                    || inst != entry.getValue().domain
                    || (int) inst.skillDomain != 29) {
                return true;
            }

            var level = server.getLevel(DomainDimension.ID);
            if (level == null) return true;

            var owner = level.getEntity(entry.getKey());

            if (!(owner instanceof LivingEntity living)
                    || !living.isAlive()) {
                return true;
            }

            GambleState state = entry.getValue();

            if (getHakariDomain(living) == null) {
                state.ballUsed = false;
                state.doorsUsed = false;
                state.rolling = false;
                return false;
            }

            if (!state.rolling) return false;

            state.rollTicks--;

            if (state.rollTicks <= 0) {
                state.rolling = false;

                boolean jackpot =
                        state.first == state.second
                                && state.second == state.third;

                sendStatus(living,
                        state.first + " | "
                                + state.second + " | "
                                + state.third
                                + (jackpot
                                ? " - JACKPOT!"
                                : " - No jackpot"));
            }

            return false;
        });
    }

    private static void sendStatus(
            LivingEntity entity,
            String message
    ) {
        if (entity instanceof ServerPlayer player) {
            player.displayClientMessage(
                    Component.literal(message),
                    true
            );
        }
    }
}
