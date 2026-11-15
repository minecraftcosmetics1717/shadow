
package com.ghosts.abyss.grade;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class AbyssGradeManager {

    private static final String GRADE_KEY = "abyss_grade";

    private AbyssGradeManager() {}

    public static AbyssGrade getGrade(ServerPlayer player) {
        return AbyssGrade.fromName(
                player.getPersistentData().getString(GRADE_KEY)
        );
    }

    public static boolean hasGrade(ServerPlayer player) {
        return player.getPersistentData().contains(GRADE_KEY);
    }

    public static void setGrade(
            ServerPlayer player,
            AbyssGrade grade
    ) {
        player.getPersistentData().putString(
                GRADE_KEY,
                grade.name()
        );

        syncJujutsucraft(player);
    }

    public static void syncJujutsucraft(ServerPlayer player) {
        AbyssGrade grade = getGrade(player);

        player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).ifPresent(vars -> {

            vars.PlayerLevel = grade.getJcLevel();
            vars.syncPlayerVariables(player);
        });

        GradeCursedEnergy.update(player);
    }

    public static void syncRikaControl(ServerPlayer player) {
        AbyssGrade grade = getGrade(player);

        // Require Abyss Semi-Grade 1 or higher.
        if (grade.ordinal() < AbyssGrade.SEMI_GRADE_1.ordinal()) {
            return;
        }

        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);

        if (vars == null) return;

        // Yuta's cursed technique ID is 5.
        boolean isYuta =
                vars.PlayerCurseTechnique == 5.0D
                        || vars.PlayerCurseTechnique2 == 5.0D;

        if (!isYuta) return;

        Advancement copy = player.server
                .getAdvancements()
                .getAdvancement(
                        new ResourceLocation(
                                "jujutsucraft",
                                "skill_copy"
                        )
                );

        Advancement rikaControl = player.server
                .getAdvancements()
                .getAdvancement(
                        new ResourceLocation(
                                "jujutsucraft",
                                "skill_rika_control"
                        )
                );

        if (copy == null || rikaControl == null) return;

        // Preserve the Copy prerequisite.
        if (!player.getAdvancements()
                .getOrStartProgress(copy).isDone()) {
            return;
        }

        AdvancementProgress progress = player
                .getAdvancements()
                .getOrStartProgress(rikaControl);

        if (progress.isDone()) return;

        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(
                    rikaControl,
                    criterion
            );
        }
    }

}
