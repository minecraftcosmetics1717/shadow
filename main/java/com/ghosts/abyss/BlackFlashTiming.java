
package com.ghosts.abyss;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abyss")
public final class BlackFlashTiming {

    private static final Map<UUID, Attempt> ATTEMPTS =
            new HashMap<>();

    private static final Map<UUID, Long> EARLY_INPUTS =
            new HashMap<>();

    // Gojo Blue Punch: hold state and pre-release E input.
    private static final Map<UUID, Long> BLUE_HOLD_TICKS = new HashMap<>();
    private static final Map<UUID, Long> BLUE_INPUTS = new HashMap<>();
    private static final int BLUE_HOLD_GRACE = 3;

    // Server ticks (20 ticks = 1 second).
    private static final int FIRST_WINDOW = 1;
    private static final int CHAIN_WINDOW = 2;
    private static final int ATTACK_TIMEOUT = 15;
    private static final int CHAIN_TIMEOUT = 100;

    private static final int EARLY_BUFFER = 3;
    private static final int LATE_RECOVERY = 3;

    private static final float BLACK_FLASH_MULTIPLIER = 2.5F;

    private static final class Attempt {
        long attackTick;
        long pressTick = -1;
        long hitTick = -1;

        boolean hitSeen = false;
        boolean finished = false;
        boolean eligible = false;
        boolean blueFinisher = false;

        int chain = 0;
        long lastSuccessTick = -1;
    }

    private BlackFlashTiming() {}

    private static boolean hasActiveChain(Attempt a, long now) {
        return a != null
                && a.chain > 0
                && a.lastSuccessTick >= 0
                && now >= a.lastSuccessTick
                && now - a.lastSuccessTick <= CHAIN_TIMEOUT;
    }
    private static boolean isYujiItadori(ServerPlayer player) {
        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);

        return vars != null && vars.PlayerCurseTechnique == 21.0D;
    }
    private static boolean hasCursedEnergy(ServerPlayer player) {
        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);

        // Temporary check until Heavenly Restriction
        // eligibility is integrated.
        return vars != null
                && vars.PlayerCursePowerFormer > 0.0D;
    }

    private static boolean canAttempt(
            ServerPlayer player,
            Attempt attempt,
            long now
    ) {
        if (!AbyssGamerules.isEnabled(
                player, AbyssGamerules.BLACK_FLASH)) {
            return false;
        }

        if (!hasCursedEnergy(player)) return false;

        return hasActiveChain(attempt, now)
                || BlackFlashFocus.isReady(player);
    }

    /** Called by the Blue Punch procedure while its grabbing phase runs. */
    public static void blueHolding(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        BLUE_HOLD_TICKS.put(player.getUUID(), player.level().getGameTime());
    }

    /** Called when Blue Punch changes into its skill 101 finishing path. */
    public static void blueFinisher(ServerPlayer player) {
        long now = player.level().getGameTime();
        UUID id = player.getUUID();
        Long pressed = BLUE_INPUTS.remove(id);
        BLUE_HOLD_TICKS.remove(id);

        if (pressed == null) return; // No E input: normal Blue Punch.

        Attempt previous = ATTEMPTS.get(id);
        boolean chaining = hasActiveChain(previous, now);
        int window = chaining ? CHAIN_WINDOW : FIRST_WINDOW;
        if (isYujiItadori(player)) window++;

        Attempt next = new Attempt();
        next.attackTick = now;
        next.pressTick = pressed;
        next.eligible = true;
        next.blueFinisher = true;
        if (chaining) {
            next.chain = previous.chain;
            next.lastSuccessTick = previous.lastSuccessTick;
        }
        ATTEMPTS.put(id, next);

        // Timing is measured at the RELEASE/transition, not impact.
        long delay = now - pressed;
        if (delay < 0 || delay > window || !canAttempt(player, next, now)) {
            failAttempt(player, next);
        }
    }

    public static void beginAttack(Entity attacker) {
        if (!(attacker instanceof ServerPlayer player)) return;

        long now = player.level().getGameTime();
        UUID id = player.getUUID();

        Attempt old = ATTEMPTS.get(id);
        // A follow-up attack procedure must not replace Blue's finisher.
        if (old != null && old.blueFinisher && !old.finished
                && now >= old.attackTick
                && now - old.attackTick < ATTACK_TIMEOUT) return;

        // Jujutsucraft may call attack procedures repeatedly.
        // Do not reset the same attack every tick.
        if (old != null
                && now >= old.attackTick
                && now - old.attackTick < ATTACK_TIMEOUT) {
            return;
        }

        Attempt next = new Attempt();
        next.attackTick = now;

        if (hasActiveChain(old, now)) {
            next.chain = old.chain;
            next.lastSuccessTick = old.lastSuccessTick;
        }

        ATTEMPTS.put(id, next);

        Long earlyTick = EARLY_INPUTS.remove(id);

        if (earlyTick != null
                && now >= earlyTick
                && now - earlyTick <= EARLY_BUFFER) {

            // E was pressed before this attack began.
            // Eligibility was checked when E was pressed.
            next.eligible = true;
            next.finished = true;

            failAttempt(player, next);
        }
    }

    public static void press(ServerPlayer player) {
        long now = player.level().getGameTime();
        UUID id = player.getUUID();

        Attempt attempt = ATTEMPTS.get(id);

        // Blue Punch has a separate E-before-release timing rule.
        Long blueTick = BLUE_HOLD_TICKS.get(id);
        if (blueTick != null && now >= blueTick
                && now - blueTick <= BLUE_HOLD_GRACE) {
            if (!BLUE_INPUTS.containsKey(id) && canAttempt(player, attempt, now)) {
                BLUE_INPUTS.put(id, now);
            }
            return;
        }

        // A late press following a connected attack.
        if (attempt != null
                && attempt.finished
                && attempt.hitSeen
                && attempt.hitTick >= 0
                && now >= attempt.hitTick
                && now - attempt.hitTick <= LATE_RECOVERY) {

            if (attempt.pressTick < 0
                    && canAttempt(player, attempt, now)) {

                attempt.eligible = true;
                attempt.pressTick = now;
                failAttempt(player, attempt);
            }

            return;
        }

        // An active attack that has not yet connected.
        if (attempt != null
                && !attempt.finished
                && now >= attempt.attackTick
                && now - attempt.attackTick < ATTACK_TIMEOUT) {

            if (attempt.pressTick >= 0) return;

            if (!canAttempt(player, attempt, now)) return;

            attempt.pressTick = now;
            attempt.eligible = true;
            return;
        }

        // Outside an attack: buffer E briefly.
        // No penalty unless an attack actually begins.
        if (!AbyssGamerules.isEnabled(
                player, AbyssGamerules.BLACK_FLASH)) {
            return;
        }

        if (!hasCursedEnergy(player)) return;

        boolean chaining = hasActiveChain(attempt, now);

        if (!chaining && !BlackFlashFocus.isReady(player)) {
            return;
        }

        EARLY_INPUTS.put(id, now);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        Entity source = event.getSource().getEntity();

        if (!(source instanceof ServerPlayer player)) return;
        if (event.getAmount() <= 0.0F) return;

        Attempt attempt = ATTEMPTS.get(player.getUUID());

        if (attempt == null || attempt.finished) return;

        long now = player.level().getGameTime();

        if (now < attempt.attackTick
                || now - attempt.attackTick >= ATTACK_TIMEOUT) {
            return;
        }

        if (!attempt.blueFinisher && !player.getPersistentData().getBoolean("attack")) {
            return;
        }

        String damageId = event.getSource().getMsgId();

        if (!"damage_combat".equals(damageId)
                && !"damage_curse".equals(damageId)) {
            return;
        }

        LivingEntity target = event.getEntity();

        if (target == player || !target.isAlive()) return;

        attempt.hitSeen = true;
        attempt.hitTick = now;
        attempt.finished = true;

        // No E press yet: ordinary hit.
        // A late E can still fail during recovery.
        if (attempt.pressTick < 0) return;

        if (!attempt.eligible) return;

        boolean chaining = hasActiveChain(attempt, now);

        int window = chaining
                ? CHAIN_WINDOW
                : FIRST_WINDOW;

        if (isYujiItadori(player)) {
            window += 1;
        }

        long delay = now - attempt.pressTick;

        if (!attempt.blueFinisher && (delay < 0 || delay > window)) {
            failAttempt(player, attempt);
            return;
        }

        if (!hasCursedEnergy(player)) {
            failAttempt(player, attempt);
            return;
        }

        if (!chaining && !BlackFlashFocus.isReady(player)) {
            failAttempt(player, attempt);
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // Black Flash modifies the original damage event.
        event.setAmount(
                event.getAmount() * BLACK_FLASH_MULTIPLIER
        );

        JujutsucraftModEntities.ENTITY_BLACK_FLASH.get().spawn(
                level,
                BlockPos.containing(target.position()),
                MobSpawnType.MOB_SUMMONED
        );

        player.addEffect(new MobEffectInstance(
                JujutsucraftModMobEffects.ZONE.get(),
                6000, 0, true, true
        ));

        BlackFlashPoints.onBlackFlash(player);
        BlackFlashPoints.applyFatigueBrainBonus(player);
        BlackFlashPoints.sync(player);

        BlackFlashFocus.consumeFocus(player);

        attempt.chain = chaining ? attempt.chain + 1 : 1;
        attempt.lastSuccessTick = now;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        long now = player.level().getGameTime();
        UUID id = player.getUUID();

        Long blueHold = BLUE_HOLD_TICKS.get(id);
        if (blueHold != null && (now < blueHold || now - blueHold > BLUE_HOLD_GRACE)) {
            BLUE_HOLD_TICKS.remove(id);
            // E without a release transition is discarded, not penalized.
            BLUE_INPUTS.remove(id);
        }

        Long earlyTick = EARLY_INPUTS.get(id);

        if (earlyTick != null
                && (now < earlyTick
                || now - earlyTick > EARLY_BUFFER)) {
            EARLY_INPUTS.remove(id);
        }

        Attempt attempt = ATTEMPTS.get(id);

        if (attempt == null) return;

        // Expire the chain after 5 seconds.
        if (attempt.chain > 0
                && !hasActiveChain(attempt, now)) {
            breakChain(attempt);
        }

        // An armed attack that never connected also fails.
        if (!attempt.finished
                && now >= attempt.attackTick
                && now - attempt.attackTick >= ATTACK_TIMEOUT) {

            attempt.finished = true;

            if (attempt.eligible && attempt.pressTick >= 0) {
                failAttempt(player, attempt);
            }
        }
    }

    private static void failAttempt(
            ServerPlayer player,
            Attempt attempt
    ) {
        if (!attempt.eligible) return;

        boolean chaining = hasActiveChain(
                attempt,
                player.level().getGameTime()
        );

        // First failed attempt costs 50 Focus.
        // A failed chain only breaks the chain.
        if (!chaining) {
            BlackFlashFocus.failedAttempt(player);
        }

        attempt.eligible = false;
        attempt.finished = true;
        breakChain(attempt);
    }

    private static void breakChain(Attempt attempt) {
        attempt.chain = 0;
        attempt.lastSuccessTick = -1;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        ATTEMPTS.remove(id);
        EARLY_INPUTS.remove(id);
        BLUE_HOLD_TICKS.remove(id);
        BLUE_INPUTS.remove(id);
    }
}
