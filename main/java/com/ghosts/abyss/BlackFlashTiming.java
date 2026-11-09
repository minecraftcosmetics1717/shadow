
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
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abyss")
public final class BlackFlashTiming {

    private static final Map<UUID, Attempt> ATTEMPTS =
            new HashMap<>();

    // Initial balancing values, in server ticks.
    private static final int MIN_DELAY = 1;
    private static final int BASE_MAX_DELAY = 3;
    private static final int MAX_CHAIN_BONUS = 2;
    private static final int ATTACK_TIMEOUT = 15;
    private static final int CHAIN_TIMEOUT = 100;

    private static boolean applyingBonus = false;

    private static final class Attempt {
        long attackTick;
        long hitTick = -1;
        UUID targetId;
        boolean pressed = false;
        int chain = 0;
        long lastSuccessTick = -1;
    }

    private BlackFlashTiming() {}

    public static void beginAttack(Entity attacker) {
        if (!(attacker instanceof ServerPlayer player)) return;

        long now = player.level().getGameTime();

        Attempt old = ATTEMPTS.get(player.getUUID());

        if (old != null && now - old.attackTick < ATTACK_TIMEOUT) {
            return;
        }

        Attempt next = new Attempt();

        if (old != null
                && old.lastSuccessTick >= 0
                && now - old.lastSuccessTick <= CHAIN_TIMEOUT) {
            next.chain = old.chain;
            next.lastSuccessTick = old.lastSuccessTick;
        }

        next.attackTick = now;
        ATTEMPTS.put(player.getUUID(), next);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (applyingBonus) return;

        Entity source = event.getSource().getEntity();

        if (!(source instanceof ServerPlayer player)) return;
        if (event.getAmount() <= 0) return;

// TEMPORARY DEBUG: A player dealt damage
        if ("damage_curse".equals(event.getSource().getMsgId())) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§e[BF DEBUG] Jujutsucraft damage detected! Attack flag: "
                                    + player.getPersistentData().getBoolean("attack")
                    )
            );
        }

        Attempt attempt = ATTEMPTS.get(player.getUUID());
        if (attempt == null || attempt.pressed) return;

        long now = player.level().getGameTime();

        if (now - attempt.attackTick > ATTACK_TIMEOUT) return;
        if (attempt.targetId != null) return;

        // Only Jujutsucraft's physical attack damage.
        if (!player.getPersistentData().getBoolean("attack")) return;
        if (!"damage_curse".equals(event.getSource().getMsgId())) return;

        LivingEntity target = event.getEntity();

        if (target == player || !target.isAlive()) return;

        attempt.hitTick = now;
        attempt.targetId = target.getUUID();
        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§a[BF DEBUG] Valid punch registered!"
                )
        );
    }

    public static void press(ServerPlayer player) {
        if (!AbyssGamerules.isEnabled(
                player, AbyssGamerules.BLACK_FLASH)) {
            return;
        }

        Attempt attempt = ATTEMPTS.get(player.getUUID());
        if (attempt == null) return;

        // The first press permanently consumes this attempt.
        if (attempt.pressed) return;
        attempt.pressed = true;

        long now = player.level().getGameTime();

        // Simultaneous activation and Black Flash is invalid.
        if (now <= attempt.attackTick) {
            breakChain(attempt);
            return;
        }

        // Pressing before a confirmed hit also fails.
        if (attempt.hitTick < 0 || attempt.targetId == null) {
            breakChain(attempt);
            return;
        }

        int chainBonus = Math.min(attempt.chain, MAX_CHAIN_BONUS);
        int maxDelay = BASE_MAX_DELAY + chainBonus;

        long delay = now - attempt.hitTick;

        if (delay < MIN_DELAY || delay > maxDelay) {
            breakChain(attempt);
            return;
        }

        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);

        if (vars == null || vars.PlayerCursePowerFormer <= 150.0D) {
            breakChain(attempt);
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) return;

        Entity found = level.getEntity(attempt.targetId);

        if (!(found instanceof LivingEntity target)
                || !target.isAlive()) {
            breakChain(attempt);
            return;
        }

        // Prevent delayed bonuses against distant targets.
        if (player.distanceToSqr(target) > 100.0D) {
            breakChain(attempt);
            return;
        }

        successfulBlackFlash(player, target, attempt, now);
    }

    private static void successfulBlackFlash(
            ServerPlayer player,
            LivingEntity target,
            Attempt attempt,
            long now
    ) {
        ServerLevel level = (ServerLevel) player.level();

        // Prototype bonus damage. This does not modify the
        // timing or duration of the original punch.
        applyingBonus = true;
        try {
            target.hurt(level.damageSources().playerAttack(player), 12.0F);
        } finally {
            applyingBonus = false;
        }

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

        attempt.chain++;
        attempt.lastSuccessTick = now;
    }

    private static void breakChain(Attempt attempt) {
        attempt.chain = 0;
        attempt.lastSuccessTick = -1;
    }
}
