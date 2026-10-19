package com.ghosts.abyss.command;

import com.ghosts.abyss.Abyss;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = "abyss")
public class KillBloodCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("killblood")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ServerLevel level = source.getLevel();

                    int killCount = 0;

                    for (Entity entity : level.getEntities().getAll()) {
                        if (entity != null) {
                            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

                            if (entityId != null && entityId.toString().equals("jujutsucrafts:entity_piercing_blood")) {
                                entity.discard();
                                killCount++;
                            }
                        }
                    }

                    final int finalKillCount = killCount;

                    if (finalKillCount > 0) {
                        source.sendSuccess(() -> Component.literal("§aSuccessfully cleared " + finalKillCount + " Piercing Blood entities"), false);
                    } else {
                        source.sendSuccess(() -> Component.literal("§eNo Piercing Blood entities found"), false);
                    }

                    return finalKillCount;
                })
        );
    }
}