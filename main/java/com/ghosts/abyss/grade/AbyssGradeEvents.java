
package com.ghosts.abyss.grade;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;

import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss",
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AbyssGradeEvents {

    private static final String GRADE_KEY = "abyss_grade";

    private AbyssGradeEvents() {}

    // Preserve Abyss grade when Minecraft replaces
    // the player's entity after death.
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        event.getOriginal().reviveCaps();

        try {
            if (event.getOriginal()
                    .getPersistentData()
                    .contains(GRADE_KEY)) {

                String grade = event.getOriginal()
                        .getPersistentData()
                        .getString(GRADE_KEY);

                event.getEntity()
                        .getPersistentData()
                        .putString(GRADE_KEY, grade);
            }
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    // Restore the grade after logging in.

    @SubscribeEvent
    public static void onLogin(
            PlayerEvent.PlayerLoggedInEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        // First login: assign Abyss's default grade.
        if (!AbyssGradeManager.hasGrade(player)) {
            AbyssGradeManager.setGrade(
                    player,
                    AbyssGrade.UNGRADED
            );
        } else {
            // Returning player: preserve their existing grade.
            AbyssGradeManager.syncJujutsucraft(player);
        }
    }

    // Restore the grade after respawning.
    @SubscribeEvent
    public static void onRespawn(
            PlayerEvent.PlayerRespawnEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        if (AbyssGradeManager.hasGrade(player)) {
            AbyssGradeManager.syncJujutsucraft(player);
        }
    }

    // Keep JC's PlayerLevel synchronized with Abyss.
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        if (!(event.player instanceof ServerPlayer player))
            return;

        if (!AbyssGradeManager.hasGrade(player)) return;

        // Check once per second instead of every tick.
        if (player.tickCount % 20 != 0) return;

        int expectedLevel =
                AbyssGradeManager.getGrade(player).getJcLevel();

        player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).ifPresent(vars -> {

            if (vars.PlayerLevel != expectedLevel) {
                AbyssGradeManager.syncJujutsucraft(player);
            }
        });
    }
}
