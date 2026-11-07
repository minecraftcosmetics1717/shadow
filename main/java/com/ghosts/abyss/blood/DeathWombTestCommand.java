
package com.ghosts.abyss.blood;

import com.ghosts.abyss.Abyss;
import com.mojang.brigadier.arguments.BoolArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Abyss.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DeathWombTestCommand {

    private DeathWombTestCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("deathwomb")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.argument(
                                        "enabled",
                                        BoolArgumentType.bool()
                                ).executes(context -> {

                                    ServerPlayer player =
                                            context.getSource().getPlayerOrException();

                                    boolean enabled =
                                            BoolArgumentType.getBool(
                                                    context,
                                                    "enabled"
                                            );

                                    DeathWombPainting.set(
                                            player,
                                            enabled
                                    );

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Death Womb Painting: "
                                                            + enabled
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )
        );
    }
}
