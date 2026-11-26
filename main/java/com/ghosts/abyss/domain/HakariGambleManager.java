package com.ghosts.abyss.domain;

import com.ghosts.abyss.Abyss;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abyss")
public final class HakariGambleManager {
    private HakariGambleManager() {}

    private static final int REQUIRED_INDICATORS = 2;
    private static final int OUTER_REVEAL_TICK = 20;
    private static final int CARD_SPIN_TICKS = 60;
    private static final int JACKPOT_BREAK_DELAY = 60;
    private static final int FAILURE_RESULT_DELAY = 40;

    private enum Phase { BUILDING, CARDS, SCENARIO, RESULT }
    private static final Map<UUID, GambleState> STATES = new HashMap<>();
    private static final class GambleState {
        final DomainInstance domain;
        Phase phase = Phase.BUILDING;
        int indicators;
        int elapsed;
        int first, second, third;
        boolean jackpot;
        boolean subway;
        HakariDomainCards.CardSet cards;
        GambleState(DomainInstance domain) { this.domain = domain; }
    }

    private static DomainInstance getHakariDomain(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || !level.dimension().equals(DomainDimension.ID)) return null;
        DomainInstance inst = DomainInstanceManager.getByOwner(entity.getUUID());
        if (inst == null || inst.skillDomain != 29.0D || !inst.ownerEntryConfirmed
                || inst.participants.size() != 1
                || entity.getPersistentData().getBoolean("abyss_domain_clashing")) return null;
        return inst;
    }

    public static boolean isHakariOwner(LivingEntity entity) {
        return entity != null && getHakariDomain(entity) != null;
    }

    public static boolean ownsCard(DomainInstance inst, UUID cardId) {
        GambleState state = STATES.get(inst.ownerId);
        return state != null && state.domain == inst && state.cards != null
                && state.cards.contains(cardId);
    }

    // Existing Reserve Ball and Shutter Doors hooks remain unchanged.
    public static void usedBall(LivingEntity entity) { registerMove(entity); }
    public static void usedDoors(LivingEntity entity) { registerMove(entity); }

    private static void registerMove(LivingEntity entity) {
        if (entity == null) return;
        DomainInstance inst = getHakariDomain(entity);
        if (inst == null) return;
        ServerLevel level = (ServerLevel) entity.level();
        GambleState state = STATES.get(entity.getUUID());
        if (state == null || state.domain != inst) {
            if (state != null) clearCards(level, state);
            state = new GambleState(inst);
            STATES.put(entity.getUUID(), state);
        }
        // Combat stays enabled during the cutscene, but it cannot start another roll.
        if (state.phase != Phase.BUILDING) return;
        if (!entity.getRandom().nextBoolean()) return;
        state.indicators++;
        if (entity instanceof ServerPlayer player) {
            player.displayClientMessage(Component.literal("Riichi " + state.indicators + "/2")
                    .withStyle(ChatFormatting.GREEN), true);
        }
        if (state.indicators >= REQUIRED_INDICATORS) startRoll(level, entity, state);
    }

    private static void startRoll(ServerLevel level, LivingEntity owner, GambleState state) {
        state.phase = Phase.CARDS;
        state.elapsed = 0;
        state.indicators = REQUIRED_INDICATORS;
        RandomSource random = owner.getRandom();
        // JC has only slot1 through slot7 animations. Keep the old 1/9 jackpot
        // odds and the old 5/9 odd vs 4/9 even mode distribution independently.
        int originalFirst = 1 + random.nextInt(9);
        int originalSecond = 1 + random.nextInt(9);
        state.jackpot = originalFirst == originalSecond;
        state.first = originalFirst <= 7 ? originalFirst
                : originalFirst == 8 ? 2 * (1 + random.nextInt(3)) : 1 + 2 * random.nextInt(4);
        state.third = state.first;
        if (state.jackpot) {
            state.second = state.first;
        } else {
            int different = 1 + random.nextInt(6);
            state.second = different >= state.first ? different + 1 : different;
        }
        HakariDomainCards.removeNativeCards(level, state.domain);
        state.cards = HakariDomainCards.spawn(level, state.domain);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel level = event.getServer().getLevel(DomainDimension.ID);
        if (level == null) return;
        for (UUID id : new ArrayList<>(STATES.keySet())) {
            GambleState state = STATES.get(id);
            if (state == null) continue;
            DomainInstance inst = DomainInstanceManager.getByOwner(id);
            LivingEntity owner = DomainArenaService.findLivingIn(level, id);
            if (inst != state.domain || inst.skillDomain != 29.0D || owner == null || !owner.isAlive()) {
                clearCards(level, state);
                // Existing lifecycle handles evacuation and station cleanup.
                STATES.remove(id, state);
                continue;
            }
            if (getHakariDomain(owner) == null) {
                if (state.subway) HakariSubwayStage.returnToBase(level, inst);
                reset(level, state);
                continue;
            }
            // A clash or lifecycle hook may have already returned everyone to
            // Stage 1 before this tick. Never resolve an interrupted scene as a win.
            if (state.subway && !HakariSubwayStage.isActive(inst)) {
                reset(level, state);
                continue;
            }
            switch (state.phase) {
                case BUILDING -> { }
                case CARDS -> {
                    state.elapsed++;
                    if (state.elapsed == OUTER_REVEAL_TICK && state.cards != null) {
                        state.cards.revealOuter(level, state.first);
                    }
                    if (state.elapsed >= CARD_SPIN_TICKS) {
                        // Outer cards have finished their 1.5s reveal animation.
                        // The middle card has never been told its final number.
                        state.subway = HakariSubwayStage.enter(owner, inst, state.jackpot);
                        if (state.subway) {
                            clearCards(level, state);
                            state.phase = Phase.SCENARIO;
                            state.elapsed = 0;
                        } else {
                            // Missing/incompatible station: resolve on the base cards.
                            beginResult(level, state);
                        }
                    }
                }
                case SCENARIO -> {
                    // The actor reaching the endpoint (or sitting down) reveals the result.
                    if (HakariSubwayStage.tickScenario(level, inst)) beginResult(level, state);
                }
                case RESULT -> {
                    int delay = state.jackpot ? JACKPOT_BREAK_DELAY : FAILURE_RESULT_DELAY;
                    if (++state.elapsed < delay) continue;
                    if (state.jackpot) {
                        // This is an intentional result-driven return, not a clash
                        // interruption. Preserve the win if lifecycle needs a retry.
                        state.subway = false;
                        if (awardJackpot(owner, inst, state.first)) {
                            clearCards(level, state);
                            STATES.remove(id, state);
                        } else {
                            state.elapsed = delay - 20;
                        }
                    } else {
                        if (state.subway) HakariSubwayStage.returnToBase(level, inst);
                        reset(level, state);
                    }
                }
            }
        }
    }

    private static void beginResult(ServerLevel level, GambleState state) {
        state.phase = Phase.RESULT;
        state.elapsed = 0;
        if (state.cards != null) state.cards.revealMiddle(level, state.second);
        // Temporary ending feedback until the actual successful/failed scenes exist.
        // Not a permanent HUD; only people in this arena see the result.
        Component result = Component.literal((state.jackpot ? "JACKPOT!  " : "Riichi failed  ")
                + state.first + " " + state.second + " " + state.third)
                .withStyle(state.jackpot ? ChatFormatting.GREEN : ChatFormatting.GRAY);
        for (ServerPlayer player : level.players()) {
            if (DomainClash.findInstanceContaining(player.position()) == state.domain) {
                player.displayClientMessage(result, true);
            }
        }
    }

    private static void clearCards(ServerLevel level, GambleState state) {
        if (state.cards != null) {
            state.cards.clear(level);
            state.cards = null;
        }
    }

    private static void reset(ServerLevel level, GambleState state) {
        clearCards(level, state);
        state.phase = Phase.BUILDING;
        state.indicators = 0;
        state.elapsed = 0;
        state.first = state.second = state.third = 0;
        state.jackpot = false;
        state.subway = false;
    }

    private static void playDomainBreakSounds(ServerLevel level, DomainInstance inst) {
        double cx = inst.overworldCenter.x;
        double cy = inst.overworldCenter.y;
        double cz = inst.overworldCenter.z;
        double[][] offsets = {
                {0,0,0}, {12,2,0}, {-12,2,0}, {0,2,12}, {0,2,-12},
                {10,8,10}, {-10,8,10}, {10,8,-10}, {-10,8,-10}, {0,14,0}
        };
        for (double[] offset : offsets) {
            float pitch = 0.75F + level.random.nextFloat() * 0.45F;
            level.playSound(null, cx + offset[0], cy + offset[1], cz + offset[2],
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, pitch);
        }
    }

    /** Preserve the previous jackpot effect, winning-number mode and break sound. */
    private static boolean awardJackpot(LivingEntity owner, DomainInstance inst, int winningNumber) {
        if (getHakariDomain(owner) != inst) return true;
        UUID id = owner.getUUID();
        var server = owner.level().getServer();
        if (server == null) return false;
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "jackpot"));
        if (effect == null) {
            Abyss.LOGGER.error("[Hakari] Jackpot effect not found!");
            HakariSubwayStage.returnToBase((ServerLevel) owner.level(), inst);
            return true;
        }
        // Return to the base sphere before evacuation. The existing lifecycle
        // hooks still clear the station on manual cancellation, expiry or death.
        HakariSubwayStage.returnToBase((ServerLevel) owner.level(), inst);
        DomainLifecycle.end(owner);
        if (DomainInstanceManager.getByOwner(id) == inst) return false;
        ServerLevel returnLevel = server.getLevel(inst.returnDim);
        if (returnLevel == null) returnLevel = server.overworld();
        playDomainBreakSounds(returnLevel, inst);
        LivingEntity returned = DomainArenaService.findLivingIn(returnLevel, id);
        if (returned != null && returned.isAlive()) {
            returned.getPersistentData().putDouble("mode_hakari", winningNumber % 2 != 0 ? 2.0D : 1.0D);
            returned.addEffect(new MobEffectInstance(effect, 5020, 0, true, true));
            Abyss.LOGGER.info("[Hakari] {} received Jackpot (winning number {})",
                    returned.getName().getString(), winningNumber);
        } else {
            Abyss.LOGGER.warn("[Hakari] Could not find owner after jackpot teleport: {}", id);
        }
        return true;
    }

    // Existing DomainHudSync still calls this method. No packets or HUD rendering needed.
    public static void syncToPlayer(ServerPlayer player) { }

    @SubscribeEvent public static void onStopping(ServerStoppingEvent event) {
        ServerLevel level = event.getServer().getLevel(DomainDimension.ID);
        if (level != null) for (GambleState state : STATES.values()) clearCards(level, state);
        STATES.clear();
    }
}
