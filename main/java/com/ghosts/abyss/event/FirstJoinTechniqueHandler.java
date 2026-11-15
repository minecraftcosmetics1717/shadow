
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

    // Starting technique IDs.
    private static final double DEFAULT_TECHNIQUE = 601.0D;
    private static final double MAKI_TECHNIQUE = -1.0D;

    // One roll out of 100.
    private static final int SIX_EYES_CHANCE = 1;
    private static final int HEAVENLY_RESTRICTION_CHANCE = 1;
    private static final int MAKI_ONLY_CHANCE = 5;

    private static final ResourceLocation INSECT_ADVANCEMENT =
            new ResourceLocation(
                    "jujutsucraft",
                    "advancement_insect"
            );

    private static final ResourceLocation MAKI_ADVANCEMENT =
            new ResourceLocation(
                    "jujutsucraft",
                    "reward_weapons"
            );

    private static final ResourceLocation GIFTED_ADVANCEMENT =
            new ResourceLocation(
                    "jujutsucraft",
                    "completely_physically_gifted"
            );

    private static final ResourceLocation SOUL_ADVANCEMENT =
            new ResourceLocation(
                    "jujutsucraft",
                    "observation_of_the_soul"
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

        CompoundTag savedData = playerData.getCompound(
                Player.PERSISTED_NBT_TAG
        );

        // Never roll again after initialization.
        if (savedData.getBoolean(INITIALIZED)) {
            return;
        }

        // Preserve existing players' progress.
        int playTime = player.getStats().getValue(
                Stats.CUSTOM.get(Stats.PLAY_TIME)
        );

        if (playTime > 0) {
            savedData.putBoolean(INITIALIZED, true);
            playerData.put(Player.PERSISTED_NBT_TAG, savedData);
            return;
        }

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

        // One roll means starting outcomes cannot overlap.
        // Values range from 0 to 99.
        int roll = player.getRandom().nextInt(100);

        String outcome;
        double technique = DEFAULT_TECHNIQUE;

        if (roll < SIX_EYES_CHANCE) {

            // 1%: Six Eyes with the normal starter technique.
            capability.FlagSixEyes = true;

            grantAdvancement(player, INSECT_ADVANCEMENT);

            outcome = "SIX_EYES";

        } else if (roll <
                SIX_EYES_CHANCE + HEAVENLY_RESTRICTION_CHANCE) {

            // 1%: Full Heavenly Restriction.
            technique = MAKI_TECHNIQUE;

            capability.FlagSixEyes = false;
            capability.PlayerCursePower = 0.0D;
            capability.PlayerCursePowerFormer = 0.0D;

            grantAdvancement(player, MAKI_ADVANCEMENT);
            grantAdvancement(player, GIFTED_ADVANCEMENT);
            grantAdvancement(player, SOUL_ADVANCEMENT);

            outcome = "HEAVENLY_RESTRICTION";

        } else if (roll <
                SIX_EYES_CHANCE
                        + HEAVENLY_RESTRICTION_CHANCE
                        + MAKI_ONLY_CHANCE) {

            // 5%: Maki's technique without full restriction.
            technique = MAKI_TECHNIQUE;

            capability.FlagSixEyes = false;

            // Keep the player's normal cursed energy values.
            // Do not grant the full physical-gifted unlock.
            grantAdvancement(player, MAKI_ADVANCEMENT);

            outcome = "MAKI_ONLY";

        } else {

            // 93%: Normal Insect starter.
            grantAdvancement(player, INSECT_ADVANCEMENT);

            outcome = "NORMAL";
        }

        capability.PlayerCurseTechnique = technique;
        capability.syncPlayerVariables(player);

        // Refresh Abyss's ability data after assigning the technique.
        CursedTechniqueEvents.initializePlayer(player);

        // Save the result permanently.
        savedData.putBoolean(INITIALIZED, true);
        savedData.putString("abyss_starting_outcome", outcome);

        playerData.put(
                Player.PERSISTED_NBT_TAG,
                savedData
        );

        Abyss.LOGGER.info(
                "Initialized {} with outcome {} and technique {}",
                player.getGameProfile().getName(),
                outcome,
                technique
        );
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
