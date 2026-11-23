package com.ghosts.abyss.technique;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncTechniqueCooldowns;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(
        modid = "abyss",
        bus = Mod.EventBusSubscriber.Bus.FORGE)

public final class TechniqueCooldownEvents {

    private TechniqueCooldownEvents() {}

    @SubscribeEvent
    public static void onEffectApplicable(
            MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        int skillId = (int) Math.round(
                player.getPersistentData().getDouble("skill"));

        if (!TechniqueCooldownManager.isManaged(skillId)) {
            return;
        }

        MobEffect effect = event.getEffectInstance().getEffect();

        boolean isMagicCooldown =
                effect == JujutsucraftModMobEffects.COOLDOWN_TIME.get();

        boolean isCombatCooldown =
                effect == JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get();

        // Only handle JC's two standard cooldown effects.
        if (!isMagicCooldown && !isCombatCooldown) {
            return;
        }

        if (skillId == 415 && isCombatCooldown) {
            event.setResult(Event.Result.DENY);
            return;
        }

        boolean usesCombat =
                TechniqueCooldownManager.usesCombatCooldown(skillId);

        if (usesCombat != isCombatCooldown) {
            return;
        }

        int duration = event.getEffectInstance().getDuration();

        // Gojo Teleport has its own 30-tick cooldown.
        // Suppress the original shared effect, but let
        // GojoTeleportExecuteMixin set its actual timer.
        if (skillId == 209) {
            event.setResult(Event.Result.DENY);
            return;
        }

        TechniqueCooldownManager.captureJcCooldown(
                player,
                skillId,
                duration);

        // Store it individually instead of applying
        // Jujutsucraft's shared cooldown effect.
        event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 5 != 0) {
            return;
        }


        Map<Integer, Integer> remaining = new HashMap<>();

        int selectedSkill =
                TechniqueCooldownManager.getSelectedSkill(player);

        boolean selectedPhysical =
                TechniqueCooldownManager.isSelectedPhysicalSkill(player);

        int burnout =
                TechniqueCooldownManager.getPostDomainRemainingTicks(player);

        for (int skillId :
                TechniqueCooldownManager.getManagedSkillIds()) {

            int ticks =
                    TechniqueCooldownManager.getRemainingTicks(
                            player, skillId
                    );

            // Show domain burnout for the currently selected
            // nonphysical skill without changing physical cooldowns.
            if (skillId == selectedSkill && !selectedPhysical) {
                ticks = Math.max(ticks, burnout);
            }

            remaining.put(skillId, ticks);
        }


        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncTechniqueCooldowns(remaining));
    }
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        TechniqueCooldownManager.copyPostDomainCooldown(
                event.getOriginal(),
                event.getEntity()
        );
    }
}
