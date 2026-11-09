
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
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abyss")
public final class BlackFlashTiming {

    private static final Map<UUID, Attempt> ATTEMPTS = new HashMap<>();

    // All timing values are in server ticks.
    private static final int BASE_WINDOW = 1;
    private static final int MAX_CHAIN_BONUS = 2;
    private static final int ATTACK_TIMEOUT = 15;
    private static final int CHAIN_TIMEOUT = 100;

    // Multiplier applied to the original punch damage.
    private static final float BLACK_FLASH_MULTIPLIER = 2.5F;

    private static final class Attempt {
        long attackTick;
        long pressTick = -1;
        boolean hitSeen = false;
        boolean finished = false;
        int chain = 0;
        long lastSuccessTick = -1;
    }

    private BlackFlashTiming() {}

    public static void beginAttack(Entity attacker) {
        if (!(attacker instanceof ServerPlayer player)) return;

        long now = player.level().getGameTime();
        Attempt old = ATTEMPTS.get(player.getUUID());

        // Jujutsucraft calls its punch procedure repeatedly.
        // Do not restart the attempt every tick.
        if (old != null
                && now - old.attackTick < ATTACK_TIMEOUT) {
            return;
        }

        Attempt next = new Attempt();
        next.attackTick = now;

        if (old != null
                && old.lastSuccessTick >= 0
                && now - old.lastSuccessTick <= CHAIN_TIMEOUT) {
            next.chain = old.chain;
            next.lastSuccessTick = old.lastSuccessTick;
        }

        ATTEMPTS.put(player.getUUID(), next);
    }

    public static void press(ServerPlayer player) {
        if (!AbyssGamerules.isEnabled(
                player, AbyssGamerules.BLACK_FLASH)) {
            return;
        }

        Attempt attempt = ATTEMPTS.get(player.getUUID());
        if (attempt == null || attempt.finished) return;

        long now = player.level().getGameTime();

        // One input per attempt.
        if (attempt.pressTick >= 0) return;

        // Pressing before the attack starts is invalid.
        if (now < attempt.attackTick
                || now - attempt.attackTick >= ATTACK_TIMEOUT) {
            attempt.finished = true;
            breakChain(attempt);
            return;
        }

        // A Black Flash cannot be activated after the hit.
        if (attempt.hitSeen) {
            attempt.finished = true;
            breakChain(attempt);
            return;
        }

        // Arm the current punch.
        attempt.pressTick = now;
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        Entity source = event.getSource().getEntity();

        if (!(source instanceof ServerPlayer player)) return;
        if (event.getAmount() <= 0.0F) return;

        Attempt attempt = ATTEMPTS.get(player.getUUID());
        if (attempt == null || attempt.finished) return;

        long now = player.level().getGameTime();

        if (now - attempt.attackTick >= ATTACK_TIMEOUT) return;

        // Only physical Jujutsucraft attacks.
        if (!player.getPersistentData().getBoolean("attack")) return;

        String damageId = event.getSource().getMsgId();

        if (!"damage_combat".equals(damageId)
                && !"damage_curse".equals(damageId)) {
            return;
        }

        LivingEntity target = event.getEntity();

        if (target == player || !target.isAlive()) return;

        // The first valid hit is the only hit eligible.
        attempt.hitSeen = true;
        attempt.finished = true;

        // No E input before impact: ordinary punch.
        if (attempt.pressTick < 0) {
            breakChain(attempt);
            return;
        }

        // Input must happen shortly BEFORE impact.
        long delay = now - attempt.pressTick;
        int window = BASE_WINDOW
                + Math.min(attempt.chain, MAX_CHAIN_BONUS);

        if (delay < 0 || delay > window) {
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

        // Increase the damage of the original punch.
        // Do NOT apply a second target.hurt() call.
        event.setAmount(
                event.getAmount() * BLACK_FLASH_MULTIPLIER
        );

        // Black Flash visuals and effects.
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
