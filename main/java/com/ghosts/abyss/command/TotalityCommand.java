
package com.ghosts.abyss.command;

import com.mojang.brigadier.CommandDispatcher;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TotalityCommand {

    public static final String TAG = "abyss_totality_enabled";

    private TotalityCommand() {}

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("totality")
                        .requires(source -> source.getEntity() instanceof ServerPlayer)
                        .then(Commands.literal("true")
                                .executes(ctx -> set(
                                        ctx.getSource().getPlayerOrException(),
                                        true
                                )))
                        .then(Commands.literal("false")
                                .executes(ctx -> set(
                                        ctx.getSource().getPlayerOrException(),
                                        false
                                )))
        );
    }

    public static boolean hasTenShadows(ServerPlayer player) {
        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);

        return vars != null && (
                vars.PlayerCurseTechnique == 6.0D
                        || vars.PlayerCurseTechnique2 == 6.0D
        );
    }

    private static int set(ServerPlayer player, boolean enabled) {
        if (!hasTenShadows(player)) {
            return 0;
        }

        player.getPersistentData().putBoolean(TAG, enabled);

        player.sendSystemMessage(Component.literal(
                enabled
                        ? "You can now damage your tamed shikigami"
                        : "Shikigami are now protected"
        ));

        return 1;
    }
}
