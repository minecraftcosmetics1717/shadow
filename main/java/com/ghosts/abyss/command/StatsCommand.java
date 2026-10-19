package com.ghosts.abyss.command;

import com.ghosts.abyss.Abyss;
import com.mojang.brigadier.CommandDispatcher;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "abyss")
public class StatsCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("stats")
            .executes(context -> {
                CommandSourceStack source = context.getSource();

                if (source.getEntity() instanceof Player player) {

                    player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(variables -> {
                        double fame = variables.PlayerFame;

                        player.sendSystemMessage(Component.literal("§eFame: §f" + (int) fame));
                    });
                    
                    return 1;
                } else {
                    return 0;
                }
            })
        );
    }
}