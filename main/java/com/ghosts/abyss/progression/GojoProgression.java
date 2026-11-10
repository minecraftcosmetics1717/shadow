
package com.ghosts.abyss.progression;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "abyss")
public final class GojoProgression {

    private static final String RCT_1 =
            "jujutsucraft:reverse_cursed_technique_1";
    private static final String RCT_2 =
            "jujutsucraft:reverse_cursed_technique_2";
    private static final String SIX_EYES =
            "jujutsucraft:advancement_six_eyes";
    private static final String INFINITY =
            "jujutsucraft:skill_infinity";
    private static final String RED =
            "jujutsucraft:skill_cursed_technique_reversal_red";
    private static final String PURPLE =
            "jujutsucraft:skill_hollow_technique_purple";

    private GojoProgression() {}

    public static boolean hasAdvancement(
            ServerPlayer player, String id) {

        Advancement advancement = player.server
                .getAdvancements()
                .getAdvancement(new ResourceLocation(id));

        return advancement != null
                && player.getAdvancements()
                .getOrStartProgress(advancement)
                .isDone();
    }

    public static boolean hasRCT(ServerPlayer player) {
        return hasAdvancement(player, RCT_1)
                || hasAdvancement(player, RCT_2);
    }

    public static boolean hasSixEyes(ServerPlayer player) {
        return hasAdvancement(player, SIX_EYES);
    }

    public static boolean isGojo(ServerPlayer player) {
        return player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY,
                (Direction) null
        ).map(vars ->
                vars.PlayerCurseTechnique == 2.0D
                        || vars.PlayerCurseTechnique2 == 2.0D
        ).orElse(false);
    }

    private static void grant(
            ServerPlayer player, String id) {

        Advancement advancement = player.server
                .getAdvancements()
                .getAdvancement(new ResourceLocation(id));

        if (advancement == null) return;

        AdvancementProgress progress = player
                .getAdvancements()
                .getOrStartProgress(advancement);

        if (progress.isDone()) return;

        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements()
                    .award(advancement, criterion);
        }
    }

    private static void revoke(
            ServerPlayer player, String id) {

        Advancement advancement = player.server
                .getAdvancements()
                .getAdvancement(new ResourceLocation(id));

        if (advancement == null) return;

        AdvancementProgress progress = player
                .getAdvancements()
                .getOrStartProgress(advancement);

        if (!progress.isDone()) return;

        for (String criterion : progress.getCompletedCriteria()) {
            player.getAdvancements()
                    .revoke(advancement, criterion);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // Check twice per second.
        if (player.tickCount % 10 != 0) return;

        if (!isGojo(player)) return;

        boolean rct = hasRCT(player);
        boolean sixEyes = hasSixEyes(player);

        // Red and Hollow Purple require only RCT.
        if (rct) {
            grant(player, RED);
            grant(player, PURPLE);
        }

        // Infinity requires both permanent unlocks.
        if (rct && sixEyes) {
            grant(player, INFINITY);
        } else {
            revoke(player, INFINITY);

            // Disable an already-active Infinity.
            player.getPersistentData()
                    .putBoolean("infinity", false);
        }
    }
}
