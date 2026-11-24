package com.ghosts.abyss.rct;

import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.technique.TechniqueCooldownManager;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = "abyss",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RctBurnout {

    // JujutsuFIN's default values.
    private static final int EXTRA_TICKS = 20;
    private static final double CE_COST_PER_TICK = 30.0D;

    private static final Set<UUID> HOLDING = new HashSet<>();

    private RctBurnout() {}

    public static void setHolding(
            ServerPlayer player,
            boolean holding) {
        UUID id = player.getUUID();

        if (!holding) {
            HOLDING.remove(id);
            return;
        }

        // Unlock and gamerule are server-authoritative.
        if (!AbyssGamerules.isEnabled(
                player,
                AbyssGamerules.REVERSE_CURSED_TECHNIQUE)) {
            HOLDING.remove(id);
            return;
        }

        if (!RctPoints.isBurnoutUnlocked(player)) {
            HOLDING.remove(id);

            player.displayClientMessage(
                    Component.literal(
                            ""),
                    true);
            return;
        }

        HOLDING.add(id);
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        UUID id = player.getUUID();

        if (!HOLDING.contains(id)) return;

        if (!player.isAlive()
                || !RctPoints.isBurnoutUnlocked(player)
                || !AbyssGamerules.isEnabled(
                player,
                AbyssGamerules.REVERSE_CURSED_TECHNIQUE)) {
            HOLDING.remove(id);
            return;
        }

        player.getCapability(
                JujutsucraftModVariables
                        .PLAYER_VARIABLES_CAPABILITY).ifPresent(vars -> {

            double availableCE =
                    vars.PlayerCursePower + vars.PlayerCursePowerChange;
            if (availableCE < CE_COST_PER_TICK) {
                return;
            }

            boolean accelerated =
                    TechniqueCooldownManager
                            .accelerateNonPhysicalCooldowns(
                                    player,
                                    EXTRA_TICKS);

            // FIN also accelerates Unstable.
            // Abyss uses this effect after domains.
            MobEffect unstable =
                    JujutsucraftModMobEffects.UNSTABLE.get();

            MobEffectInstance current =
                    player.getEffect(unstable);

            if (current != null
                    && current.getDuration() > 0) {

                // Continue RCT acceleration through the final second.
                // Remove the effect when its remaining time reaches zero.
                int newDuration = Math.max(0,
                        current.getDuration() - EXTRA_TICKS);

                int amplifier = current.getAmplifier();
                boolean ambient = current.isAmbient();
                boolean visible = current.isVisible();
                boolean icon = current.showIcon();

                player.removeEffect(unstable);

                if (newDuration > 0) {
                    player.addEffect(new MobEffectInstance(
                            unstable, newDuration, amplifier,
                            ambient, visible, icon));
                }

                accelerated = true;
            }

            // Charge once per game tick, not once
            // for each affected cooldown.
            if (accelerated) {
                vars.PlayerCursePowerChange -=
                        CE_COST_PER_TICK;

                vars.syncPlayerVariables(player);
            }
        });
    }

    @SubscribeEvent
    public static void onLogout(
            PlayerEvent.PlayerLoggedOutEvent event) {
        HOLDING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        // Preserve the purchased upgrade after death.
        boolean unlocked = event.getOriginal()
                .getPersistentData()
                .getBoolean(RctPoints.KEY_BURNOUT_UNLOCKED);

        if (unlocked) {
            event.getEntity()
                    .getPersistentData()
                    .putBoolean(
                            RctPoints.KEY_BURNOUT_UNLOCKED,
                            true);
        }

        HOLDING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HOLDING.remove(player.getUUID());
            RctPoints.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HOLDING.remove(player.getUUID());
            RctPoints.sync(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(
            PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RctPoints.sync(player);
        }
    }
}
