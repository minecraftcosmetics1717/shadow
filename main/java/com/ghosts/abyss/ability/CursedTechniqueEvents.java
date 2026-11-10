
package com.ghosts.abyss.ability;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Detects changes to a player's cursed technique
 * and refreshes their available abilities.
 *
 * Prepared replacement for EventListeners.
 *
 * This class is intentionally not registered
 * to the Forge event bus yet.
 */
public final class CursedTechniqueEvents {

    private static final Map<UUID, Double> lastKnownCT =
            new HashMap<>();

    private CursedTechniqueEvents() {}

    /**
     * Called once per server player tick
     * after the final migration.
     */
    public static void onPlayerTick(ServerPlayer player) {
        checkCTChanged(player);
    }

    /**
     * Detects changes to the player's primary
     * cursed technique every 20 ticks.
     */
    private static void checkCTChanged(ServerPlayer player) {

        if (player.tickCount % 20 == 0) {

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
                    return;
                }

                Field ctField = vars.getClass()
                        .getDeclaredField("PlayerCurseTechnique");

                ctField.setAccessible(true);

                double ct = (Double) ctField.get(vars);

                Double prev = lastKnownCT.get(player.getUUID());

                if (prev == null || prev != ct) {

                    lastKnownCT.put(player.getUUID(), ct);

                    AbilityScanHandler.scanAndSync(player);
                }

            } catch (Exception ignored) {
                // Preserve original behavior.
            }
        }
    }

    /**
     * Initializes the cached technique on login.
     *
     * This will be called from PlayerLifecycleEvents
     * during the final migration.
     */
    public static void initializePlayer(ServerPlayer player) {

        double ct = AbilityScanHandler.getCurrentCT(player);

        lastKnownCT.put(player.getUUID(), ct);

        AbilityScanHandler.scanAndSync(player);
    }

    /**
     * Removes the player's cached technique
     * when they leave the server.
     */
    public static void clearPlayer(UUID playerId) {
        lastKnownCT.remove(playerId);
    }
}
