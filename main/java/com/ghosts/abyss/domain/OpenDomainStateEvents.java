
package com.ghosts.abyss.domain;

import java.lang.reflect.Field;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles automatic Open Domain state management.
 *
 * Prepared replacement for EventListeners.
 *
 * This class is intentionally not registered
 * to the Forge event bus yet.
 */
public final class OpenDomainStateEvents {

    private OpenDomainStateEvents() {}

    /**
     * Called once per server player tick
     * after the final migration.
     */
    public static void onPlayerTick(ServerPlayer player) {

        // Original behavior: check every 20 ticks.
        if (player.tickCount % 20 != 0) {
            return;
        }

        boolean restricted = isRestrictedCT(player);

        boolean activeFlag = player.getPersistentData()
                .getBoolean("open_domain");

        boolean toggledFlag = player.getPersistentData()
                .getBoolean("open_domain_toggled");

        // Restricted cursed techniques cannot use Open Domain.
        if (restricted && activeFlag) {

            player.getPersistentData().putBoolean(
                    "open_domain",
                    false
            );

            OpenDomain.sync(player);

        } else if (!restricted && toggledFlag && !activeFlag) {

            // Don't reactivate Open Domain during expansion.
            boolean hasDomainEffect = player.getActiveEffects()
                    .stream()
                    .anyMatch(effect -> {

                        ResourceLocation loc =
                                ForgeRegistries.MOB_EFFECTS.getKey(
                                        effect.getEffect()
                                );

                        return loc != null
                                && loc.toString().equals(
                                "jujutsucraft:domain_expansion"
                        );
                    });

            if (!hasDomainEffect) {

                player.getPersistentData().putBoolean(
                        "open_domain",
                        true
                );

                OpenDomain.sync(player);
            }
        }
    }

    /**
     * Checks whether the player is using a cursed
     * technique restricted from Open Domain.
     *
     * Restricted CT IDs:
     * 1, 18, and 41.
     *
     * Also checks the secondary cursed technique
     * when SecondTechnique is active.
     */
    private static boolean isRestrictedCT(ServerPlayer player) {
        // Open domains are controlled by Abyss's unlock system.
        // No cursed technique is automatically excluded.
        return false;
    }}
