
package com.ghosts.abyss.command;

import com.ghosts.abyss.grade.AbyssGrade;
import com.ghosts.abyss.grade.AbyssGradeManager;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "abyss")
public final class AbyssGradeCommand {

    private AbyssGradeCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher =
                event.getDispatcher();

        var root = Commands.literal("abyss");

        var grade = Commands.literal("grade");

        grade.then(
                Commands.literal("get")
                        .executes(context -> {
                            ServerPlayer player =
                                    context.getSource().getPlayerOrException();

                            AbyssGrade current =
                                    AbyssGradeManager.getGrade(player);

                            context.getSource().sendSuccess(
                                    () -> Component.literal(
                                            "Your Abyss grade: "
                                                    + current.getDisplayName()
                                    ),
                                    false
                            );

                            return 1;
                        })
        );

        grade.then(
                Commands.literal("list")
                        .executes(context -> {
                            for (AbyssGrade value : AbyssGrade.values()) {
                                context.getSource().sendSuccess(
                                        () -> Component.literal(
                                                value.name().toLowerCase()
                                                        + " - "
                                                        + value.getDisplayName()
                                        ),
                                        false
                                );
                            }

                            return 1;
                        })
        );

        var set = Commands.literal("set")
                .requires(source -> source.hasPermission(2));

        for (AbyssGrade value : AbyssGrade.values()) {
            set.then(
                    Commands.literal(value.name().toLowerCase())
                            .then(
                                    Commands.argument(
                                                    "player",
                                                    EntityArgument.player()
                                            )
                                            .executes(context -> {
                                                ServerPlayer target =
                                                        EntityArgument.getPlayer(
                                                                context,
                                                                "player"
                                                        );

                                                AbyssGradeManager.setGrade(
                                                        target,
                                                        value
                                                );

                                                context.getSource().sendSuccess(
                                                        () -> Component.literal(
                                                                "Set "
                                                                        + target.getGameProfile().getName()
                                                                        + "'s grade to "
                                                                        + value.getDisplayName()
                                                        ),
                                                        true
                                                );

                                                return 1;
                                            })
                            )
            );
        }

        grade.then(set);
        root.then(grade);

        dispatcher.register(root);
    }
}
