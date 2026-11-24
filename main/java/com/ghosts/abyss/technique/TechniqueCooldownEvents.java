package com.ghosts.abyss.technique;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncTechniqueCooldowns;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

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
            MobEffectEvent.Applicable event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MobEffect effect = event.getEffectInstance().getEffect();

        boolean isMagicCooldown =
                effect == JujutsucraftModMobEffects.COOLDOWN_TIME.get();

        boolean isCombatCooldown =
                effect == JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get();

        if (!isMagicCooldown && !isCombatCooldown) {
            return;
        }

        // Prefer the skill actually being executed by JC.
        int skillId = (int) Math.round(
                player.getPersistentData().getDouble("skill")
        );

        // Some JC procedures clear or change "skill" early.
        // Fall back to the currently selected technique.
        if (!TechniqueCooldownManager.isManaged(skillId)) {
            skillId = TechniqueCooldownManager.getSelectedSkill(player);
        }

        if (!TechniqueCooldownManager.isManaged(skillId)) {
            return;
        }

        // Preserve Jogo's special combat cooldown suppression.
        if (skillId == 415 && isCombatCooldown) {
            event.setResult(Event.Result.DENY);
            return;
        }

        // Gojo Teleport already starts its own cooldown.
        if (skillId == 209) {
            event.setResult(Event.Result.DENY);
            return;
        }

        int duration = event.getEffectInstance().getDuration();

        if (duration <= 0) {
            return;
        }

        // Capture whichever JC cooldown effect actually fired,
        // rather than requiring a predetermined cooldown type.
        TechniqueCooldownManager.captureJcCooldown(
                player,
                skillId,
                duration
        );

        // Keep JC's shared cooldown from replacing the
        // individual cooldown stored by Abyss.
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

        // Yuta's slot values 100+ are indexes, not actual skill IDs.
        // A slot such as 105 must not be mistaken for Megumi skill 605.
        int selectedYutaCopyIndex = -1;
        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);

        if (vars != null) {
            double activeCT = vars.SecondTechnique
                    ? vars.PlayerCurseTechnique2
                    : vars.PlayerCurseTechnique;
            if (activeCT == 5.0D && vars.PlayerSelectCurseTechnique >= 100.0D) {
                selectedYutaCopyIndex =
                        (int) Math.round(vars.PlayerSelectCurseTechnique) - 100;
                selectedSkill = 0;
            }
        }

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

        // Reuse Yuta's existing original skill IDs. Do not create
        // synthetic cooldowns for the 600+ selection slots.
        ListTag registered = player.getPersistentData().getList(
                "jslop_yuta_registered", Tag.TAG_COMPOUND);

        for (int i = 0; i < registered.size(); i++) {
            int originalSkillId = (int) Math.round(
                    registered.getCompound(i).getDouble("skill"));
            if (originalSkillId <= 0) continue;

            int ticks = TechniqueCooldownManager.getRemainingTicks(
                    player, originalSkillId);

            // Preserve the existing post-domain cooldown display when
            // Yuta is currently viewing a registered copied ability.
            if (i == selectedYutaCopyIndex && !selectedPhysical) {
                ticks = Math.max(ticks, burnout);
            }

            remaining.merge(originalSkillId, ticks, Math::max);
        }

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncTechniqueCooldowns(remaining));
    }
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        TechniqueCooldownManager.copyPostDomainCooldown(
                event.getOriginal(),
                event.getEntity());
    }
}
