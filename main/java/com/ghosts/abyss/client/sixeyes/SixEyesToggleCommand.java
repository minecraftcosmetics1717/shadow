
package com.ghosts.abyss.client.sixeyes;

import com.mojang.brigadier.Command;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraft.commands.Commands.literal;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SixEyesToggleCommand {

    private SixEyesToggleCommand() {}

    @SubscribeEvent
    public static void registerCommands(
            RegisterClientCommandsEvent event
    ) {
        event.getDispatcher().register(
                literal("togglesixeyes")
                        .executes(context -> {
                            Minecraft mc = Minecraft.getInstance();

                            SixEyesVisionHandler.toggleWheelVision(
                                    mc.player
                            );

                            // No chat message or feedback.
                            return Command.SINGLE_SUCCESS;
                        })
        );
    }
}
