
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

        try {
            Class<?> varsClass = Class.forName(
                    "net.mcreator.jujutsucraft.network."
                            + "JujutsucraftModVariables"
            );

            Field capField = varsClass.getDeclaredField(
                    "PLAYER_VARIABLES_CAPABILITY"
            );

            capField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Capability<Object> cap =
                    (Capability<Object>) capField.get(null);

            Object vars = player.getCapability(cap, null)
                    .orElse(null);

            if (vars == null) {
                return false;
            }

            Field ctField = vars.getClass()
                    .getDeclaredField("PlayerCurseTechnique");

            ctField.setAccessible(true);

            double ct = (Double) ctField.get(vars);

            // Check primary cursed technique.
            if (ct == 1.0D || ct == 18.0D || ct == 41.0D) {
                return true;
            }

            // Check secondary cursed technique.
            Field ct2Field = vars.getClass()
                    .getDeclaredField("PlayerCurseTechnique2");

            ct2Field.setAccessible(true);

            double ct2 = (Double) ct2Field.get(vars);

            Field secondField = vars.getClass()
                    .getDeclaredField("SecondTechnique");

            secondField.setAccessible(true);

            boolean secondActive =
                    (Boolean) secondField.get(vars);

            return secondActive
                    && (ct2 == 1.0D
                    || ct2 == 18.0D
                    || ct2 == 41.0D);

        } catch (Exception e) {
            // Preserve original behavior.
            return false;
        }
    }
}
