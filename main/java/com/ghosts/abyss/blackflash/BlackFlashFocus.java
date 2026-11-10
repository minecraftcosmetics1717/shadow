
package com.ghosts.abyss.blackflash;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abyss")
public final class BlackFlashFocus {

    public static final int MAX_FOCUS = 100;
    public static final int REQUIRED_FOCUS = 100;
    public static final int FAILED_ATTEMPT_PENALTY = 50;

    // Initial balancing values.
    private static final int FOCUS_PER_HIT = 4;
    private static final int FOCUS_PER_DAMAGE_TAKEN = 6;

    // 10 seconds before Focus starts decaying.
    private static final int DECAY_DELAY = 200;

    // Lose 1 Focus every 10 ticks (0.5 seconds).
    private static final int DECAY_INTERVAL = 10;

    // Prevent one multi-hit attack from generating
    // Focus repeatedly in the same few ticks.
    private static final int HIT_REWARD_COOLDOWN = 4;
    private static final int HURT_REWARD_COOLDOWN = 4;

    private static final String FOCUS_KEY =
            "abyss_black_flash_focus";

    private static final Map<UUID, CombatState> STATES =
            new HashMap<>();

    private static final class CombatState {
        int idleTicks = 0;
        int decayTicks = 0;
        long lastHitReward = Long.MIN_VALUE;
        long lastHurtReward = Long.MIN_VALUE;
    }

    private BlackFlashFocus() {}

    private static CombatState state(ServerPlayer player) {
        return STATES.computeIfAbsent(
                player.getUUID(),
                id -> new CombatState()
        );
    }

    private static CompoundTag savedData(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();

        if (!persistent.contains(Player.PERSISTED_NBT_TAG, 10)) {
            persistent.put(
                    Player.PERSISTED_NBT_TAG,
                    new CompoundTag()
            );
        }

        return persistent.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static int get(ServerPlayer player) {
        return Math.max(0, Math.min(
                MAX_FOCUS,
                savedData(player).getInt(FOCUS_KEY)
        ));
    }

    public static void set(ServerPlayer player, int amount) {
        int clamped = Math.max(
                0,
                Math.min(MAX_FOCUS, amount)
        );

        savedData(player).putInt(FOCUS_KEY, clamped);
    }

    public static void add(ServerPlayer player, int amount) {
        if (amount <= 0) return;

        set(player, get(player) + amount);
        markCombat(player);
    }

    public static boolean isReady(ServerPlayer player) {
        return get(player) >= REQUIRED_FOCUS;
    }

    public static void failedAttempt(ServerPlayer player) {
        set(
                player,
                get(player) - FAILED_ATTEMPT_PENALTY
        );
    }

    public static void consumeFocus(ServerPlayer player) {
        set(player, 0);
    }

    private static void markCombat(ServerPlayer player) {
        CombatState state = state(player);
        state.idleTicks = 0;
        state.decayTicks = 0;
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getAmount() <= 0.0F) return;

        Entity attacker = event.getSource().getEntity();

        // Reward the attacker for dealing damage.
        if (attacker instanceof ServerPlayer player
                && attacker != event.getEntity()) {

            CombatState state = state(player);
            long now = player.level().getGameTime();

            markCombat(player);

            if (state.lastHitReward == Long.MIN_VALUE
                    || now - state.lastHitReward
                    >= HIT_REWARD_COOLDOWN) {

                state.lastHitReward = now;
                add(player, FOCUS_PER_HIT);
            }
        }

        // Reward the player for receiving damage.
        if (event.getEntity() instanceof ServerPlayer player) {
            CombatState state = state(player);
            long now = player.level().getGameTime();

            markCombat(player);

            if (state.lastHurtReward == Long.MIN_VALUE
                    || now - state.lastHurtReward
                    >= HURT_REWARD_COOLDOWN) {

                state.lastHurtReward = now;
                add(player, FOCUS_PER_DAMAGE_TAKEN);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        CombatState state = state(player);

        if (get(player) <= 0) {
            state.decayTicks = 0;
            return;
        }

        state.idleTicks++;

        if (state.idleTicks < DECAY_DELAY) return;

        state.decayTicks++;

        if (state.decayTicks >= DECAY_INTERVAL) {
            state.decayTicks = 0;
            set(player, get(player) - 1);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        CompoundTag originalData =
                event.getOriginal().getPersistentData();

        if (originalData.contains(Player.PERSISTED_NBT_TAG, 10)) {
            event.getEntity().getPersistentData().put(
                    Player.PERSISTED_NBT_TAG,
                    originalData.getCompound(
                            Player.PERSISTED_NBT_TAG
                    ).copy()
            );
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }
}
