
package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;

@Mod.EventBusSubscriber(
        modid = Abyss.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class AbyssDatapackRegistry {

    private AbyssDatapackRegistry() {}

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {

        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }

        Path packPath = ModList.get()
                .getModFileById(Abyss.MOD_ID)
                .getFile()
                .findResource("resourcepacks/start_items");

        Pack pack = Pack.readMetaAndCreate(
                "abyss:start_items",
                Component.literal("Abyss - No Starter Items"),
                true,
                id -> new PathPackResources(id, packPath, true),
                PackType.SERVER_DATA,
                Pack.Position.TOP,
                PackSource.BUILT_IN
        );

        if (pack != null) {
            event.addRepositorySource(
                    consumer -> consumer.accept(pack)
            );

            Abyss.LOGGER.info(
                    "Registered Abyss starting items datapack"
            );
        } else {
            Abyss.LOGGER.warn(
                    "Failed to register Abyss starting items datapack"
            );
        }
    }
}
