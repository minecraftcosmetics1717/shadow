
package com.ghosts.abyss.grade;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
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

}
