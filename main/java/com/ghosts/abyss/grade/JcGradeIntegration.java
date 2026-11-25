package com.ghosts.abyss.grade;

import net.mcreator.jujutsucraft.init.JujutsucraftModGameRules;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.CompletedAdvancementProcedure;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Jujutsucraft advancements are the ONLY persistent source of player grades.
 * Abyss quests can grant these advancements when their requirements are met.
 * Fame is disabled globally, and old Abyss grades are migrated once on login.
 */
@Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JcGradeIntegration {
    private static final String OLD_GRADE_KEY = "abyss_grade";

    // From lowest to highest. Ungraded means none of these advancements.
    private static final String[] GRADES = {
            "sorcerer_grade_4",
            "sorcerer_grade_3",
            "sorcerer_grade_2_semi",
            "sorcerer_grade_2",
            "sorcerer_grade_1_semi",
            "sorcerer_grade_1",
            "sorcerer_grade_special"
    };

    private JcGradeIntegration() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        disableFame(event.getServer());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        disableFame(player.server);
        migratePreviousGrade(player);
        clearFame(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) return;

        // If a command/mod turns the game rule back on, Abyss turns it off.
        // Checking once per tick is cheap; setting it only occurs on a change.
        disableFame(player.server);
        clearFame(player);

        if (player.tickCount % 20 == 0) {
            syncRikaControl(player);
        }
    }

    private static void disableFame(MinecraftServer server) {
        if (server == null) return;
        var rule = server.overworld().getGameRules()
                .getRule(JujutsucraftModGameRules.JUJUTSU_GAIN_FAME);
        if (rule.get()) {
            rule.set(false, server);
        }
    }

    private static void clearFame(ServerPlayer player) {
        player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY)
                .ifPresent(vars -> {
                    if (vars.PlayerFame != 0.0D) {
                        vars.PlayerFame = 0.0D;
                        vars.syncPlayerVariables(player);
                    }
                });
    }

    /**
     * A one-time conversion of the saved Abyss rank to Jujutsucraft's actual
     * advancements. Revoke any older, fame-earned grade advancements first so
     * that the saved Abyss rank (including Ungraded) is respected on migration.
     * After success, remove the legacy NBT key; it will never be used again.
     */
    private static void migratePreviousGrade(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(OLD_GRADE_KEY, Tag.TAG_STRING)) return;

        String oldGrade = data.getString(OLD_GRADE_KEY);
        int targetIndex = switch (oldGrade) {
            case "GRADE_4" -> 0;
            case "GRADE_3" -> 1;
            case "SEMI_GRADE_2" -> 2;
            case "GRADE_2" -> 3;
            case "SEMI_GRADE_1" -> 4;
            case "GRADE_1" -> 5;
            case "SPECIAL_GRADE" -> 6;
            default -> -1; // UNGRADED
        };

        List<Advancement> advancements = new ArrayList<>();
        for (String name : GRADES) {
            Advancement advancement = player.server.getAdvancements()
                    .getAdvancement(new ResourceLocation("jujutsucraft", name));
            if (advancement == null) {
                // Avoid deleting the previous rank if the JC advancement
                // registry is missing or not fully initialized.
                return;
            }
            advancements.add(advancement);
        }

        // Remove high-to-low, so previous fame promotions cannot override
        // the legacy Abyss rank during this one-time conversion.
        for (int i = advancements.size() - 1; i >= 0; i--) {
            Advancement advancement = advancements.get(i);
            AdvancementProgress progress = player.getAdvancements()
                    .getOrStartProgress(advancement);
            for (String criterion : progress.getCompletedCriteria()) {
                player.getAdvancements().revoke(advancement, criterion);
            }
        }

        if (targetIndex >= 0) {
            // Jujutsucraft's advancement listener also grants parents.
            Advancement target = advancements.get(targetIndex);
            AdvancementProgress progress = player.getAdvancements()
                    .getOrStartProgress(target);
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(target, criterion);
            }
        }

        // Recalculate JC's PlayerLevel and cursed-energy limit using its
        // existing advancement/respawn logic; no independent Abyss rank.
        CompletedAdvancementProcedure.execute(
                player.serverLevel(), player.getX(), player.getY(), player.getZ(), player
        );

        data.remove(OLD_GRADE_KEY);
    }

    private static boolean hasGradeAtLeastSemiOne(ServerPlayer player) {
        // These advancements normally grant their prerequisites, but check
        // each explicitly for worlds with nonstandard advancement progress.
        for (int i = 4; i < GRADES.length; i++) {
            Advancement adv = player.server.getAdvancements().getAdvancement(
                    new ResourceLocation("jujutsucraft", GRADES[i]));
            if (adv != null && player.getAdvancements()
                    .getOrStartProgress(adv).isDone()) {
                return true;
            }
        }
        return false;
    }

    /** Keep the existing Rika Control unlock, but use JC's rank instead. */
    private static void syncRikaControl(ServerPlayer player) {
        if (!hasGradeAtLeastSemiOne(player)) return;

        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
        if (vars == null) return;
        if (vars.PlayerCurseTechnique != 5.0D
                && vars.PlayerCurseTechnique2 != 5.0D) return;

        Advancement copy = player.server.getAdvancements().getAdvancement(
                new ResourceLocation("jujutsucraft", "skill_copy"));
        Advancement rika = player.server.getAdvancements().getAdvancement(
                new ResourceLocation("jujutsucraft", "skill_rika_control"));
        if (copy == null || rika == null) return;
        if (!player.getAdvancements().getOrStartProgress(copy).isDone()) return;

        AdvancementProgress progress = player.getAdvancements()
                .getOrStartProgress(rika);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(rika, criterion);
        }
    }
}
