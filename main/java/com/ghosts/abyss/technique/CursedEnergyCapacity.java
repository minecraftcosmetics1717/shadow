package com.ghosts.abyss.technique;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/** Single source for the ordinary, Copy and awakened Copy CE capacities. */
public final class CursedEnergyCapacity {
    private static final ResourceLocation CURSE_LIFTED =
            new ResourceLocation("jujutsucraft", "skill_curseis_lifted");
    private static final double NORMAL_BASE = 200.0D;
    private static final double COPY_BASE = 400.0D;
    private static final double AWAKENED_COPY_BASE = 500.0D;

    private CursedEnergyCapacity() {}

    public static boolean hasCurseLifted(ServerPlayer player) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(CURSE_LIFTED);
        return advancement != null && player.getAdvancements()
                .getOrStartProgress(advancement).isDone();
    }

    /** Tick refresh: only Abyss's starter and Copy are managed continuously. */
    public static void refresh(ServerPlayer player) {
        refresh(player, false);
    }

    /** Explicit imprint exchange: recalculate the supported destination technique. */
    public static void refreshAfterTransfer(ServerPlayer player) {
        refresh(player, true);
    }

    private static void refresh(ServerPlayer player, boolean imprintTransfer) {
        var vars = player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY)
                .orElse(null);
        if (vars == null) return;

        // Do not give cursed energy to Heavenly Restriction or alter JC's
        // physical-gifted/non-sorcerer initialization and energy rules.
        if (vars.PlayerCurseTechnique <= 0.0D) return;

        double ct = vars.PlayerCurseTechnique;
        boolean managed = ct == TechniqueIds.NO_INNATE_TECHNIQUE || ct == 5.0D;
        boolean standardImprint = ct == 1.0D || ct == 2.0D || ct == 6.0D || ct == 15.0D;
        if (!managed && !(imprintTransfer && standardImprint)) return;

        double base = vars.PlayerCurseTechnique == 5.0D
                ? (hasCurseLifted(player) ? AWAKENED_COPY_BASE : COPY_BASE)
                : NORMAL_BASE;

        // Same rank multiplier as Jujutsucraft's WhenRespawnProcedure.
        double max = base * Math.round((2.0D + vars.PlayerLevel) / 1.1D);

        // Preserve JC's special vessel capacity floor instead of replacing it
        // with the ordinary sorcerer cap.
        MobEffectInstance sukuna = player.getEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get());
        if (sukuna != null) {
            int level = Math.min(sukuna.getAmplifier() + 1, 20);
            double vesselMax = level <= 9
                    ? 3000.0D + (level - 1) * 300.0D
                    : 8000.0D + (level - 10) * 1000.0D;
            max = Math.max(max, vesselMax);
        }

        // No free refill on technique changes. Retain existing CE when the
        // cap grows; clamp it when the cap falls.
        double current = Math.max(0.0D, Math.min(vars.PlayerCursePower, max));
        if (Double.compare(vars.PlayerCursePowerFormer, base) == 0
                && Double.compare(vars.PlayerCursePowerMAX, max) == 0
                && Double.compare(vars.PlayerCursePower, current) == 0) return;

        vars.PlayerCursePowerFormer = base;
        vars.PlayerCursePowerMAX = max;
        vars.PlayerCursePower = current;
        vars.syncPlayerVariables(player);
    }
}
