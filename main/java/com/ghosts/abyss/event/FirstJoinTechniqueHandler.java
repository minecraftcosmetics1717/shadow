
package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.ability.CursedTechniqueEvents;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(
        modid = Abyss.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class FirstJoinTechniqueHandler {

    private static final String INITIALIZED =
            "abyss_starting_technique_initialized";

    private static final double DEFAULT_TECHNIQUE = 601.0D;

    private static final ResourceLocation INSECT_ADVANCEMENT =
            new ResourceLocation(
                    "jujutsucraft",
                    "advancement_insect"
            );

    private FirstJoinTechniqueHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag playerData = player.getPersistentData();

        // Forge's persisted data is retained across player deaths.
        CompoundTag savedData = playerData.getCompound(
                Player.PERSISTED_NBT_TAG
        );

        // This player's starting setup was already processed.
        if (savedData.getBoolean(INITIALIZED)) {
            return;
        }

        // Protect existing world saves from being overwritten
        // when Abyss's starting system is installed.
        int playTime = player.getStats().getValue(
                Stats.CUSTOM.get(Stats.PLAY_TIME)
        );

        if (playTime > 0) {
            savedData.putBoolean(INITIALIZED, true);

            playerData.put(
                    Player.PERSISTED_NBT_TAG,
                    savedData
            );

            return;
        }

        // Assign the starter technique.
        double technique = getStartingTechnique(player);

        var capability = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);

        if (capability == null) {
            Abyss.LOGGER.warn(
                    "Could not initialize starting technique for {}",
                    player.getGameProfile().getName()
            );
            return;
        }

        capability.PlayerCurseTechnique = technique;

        // Synchronize Jujutsucraft's capability to the client.
        capability.syncPlayerVariables(player);

        // Award the starting advancement.
        grantAdvancement(player, INSECT_ADVANCEMENT);

        // Refresh Abyss's technique cache and ability data.
        CursedTechniqueEvents.initializePlayer(player);

        // Mark the starting setup as completed permanently.
        savedData.putBoolean(INITIALIZED, true);

        playerData.put(
                Player.PERSISTED_NBT_TAG,
                savedData
        );

        Abyss.LOGGER.info(
                "Initialized starting technique {} for {}",
                technique,
                player.getGameProfile().getName()
        );
    }

    // Starting technique selection.
    // Future special cases can be added here.
    private static double getStartingTechnique(
            ServerPlayer player
    ) {
        return DEFAULT_TECHNIQUE;
    }

    private static void grantAdvancement(
            ServerPlayer player,
            ResourceLocation advancementId
    ) {
        Advancement advancement = player.server
                .getAdvancements()
                .getAdvancement(advancementId);

        if (advancement == null) {
            Abyss.LOGGER.warn(
                    "Missing starting advancement: {}",
                    advancementId
            );
            return;
        }

        AdvancementProgress progress = player
                .getAdvancements()
                .getOrStartProgress(advancement);

        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(
                    advancement,
                    criterion
            );
        }
    }
}
