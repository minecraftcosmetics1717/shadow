
package com.ghosts.abyss;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss")
public final class BlackFlashFocusCommands {

    private BlackFlashFocusCommands() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("blackflashfocus")

                        // /blackflashfocus
                        .executes(context -> {
                            ServerPlayer player =
                                    context.getSource().getPlayerOrException();

                            int focus = BlackFlashFocus.get(player);

                            player.sendSystemMessage(
                                    Component.literal(
                                            "§dBlack Flash Focus: §f"
                                                    + focus + "/100"
                                    )
                            );

                            return 1;
                        })

                        // /blackflashfocus set <amount>
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument(
                                                        "amount",
                                                        IntegerArgumentType.integer(0, 100)
                                                )
                                                .executes(context -> {
                                                    ServerPlayer player =
                                                            context.getSource()
                                                                    .getPlayerOrException();

                                                    int amount =
                                                            IntegerArgumentType.getInteger(
                                                                    context,
                                                                    "amount"
                                                            );

                                                    BlackFlashFocus.set(player, amount);

                                                    player.sendSystemMessage(
                                                            Component.literal(
                                                                    "§aBlack Flash Focus set to "
                                                                            + amount + "/100"
                                                            )
                                                    );

                                                    return 1;
                                                })
                                )
                        )

                        // /blackflashfocus reset
                        .then(Commands.literal("reset")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    BlackFlashFocus.set(player, 0);

                                    player.sendSystemMessage(
                                            Component.literal(
                                                    "§cBlack Flash Focus reset."
                                            )
                                    );

                                    return 1;
                                })
                        )
        );
    }
}
