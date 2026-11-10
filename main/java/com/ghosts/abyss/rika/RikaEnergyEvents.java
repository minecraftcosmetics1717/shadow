
package com.ghosts.abyss.rika;

import java.lang.reflect.Field;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles Rika's cursed energy capacity and regeneration.
 *
 * Prepared replacement for EventListeners.
 * Not registered to the Forge event bus yet.
 */
public final class RikaEnergyEvents {

    private static final String NBT_BOOSTED =
            "jslop_rika_ce_boosted";

    private static final String NBT_BASE_MAX =
            "jslop_rika_base_ce_max";

    private static final String NBT_LAST_SET_MAX =
            "jslop_rika_last_set_max";

    private RikaEnergyEvents() {}

    /**
     * Called once per server player tick after migration.
     */
    public static void onPlayerTick(ServerPlayer player) {
        applyRikaCEBoost(player);
    }

    private static void applyRikaCEBoost(ServerPlayer player) {

        if (player.tickCount % 10 != 0 || player.isSpectator()) {
            return;
        }

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

            if ((Double) ctField.get(vars) == 0.0D) {
                return;
            }

            boolean rikaPresent = (Boolean) Class.forName(
                    "net.mcreator.jujutsucraft.procedures."
                            + "LocateRikaProcedure"
            ).getMethod(
                    "execute",
                    LevelAccessor.class,
                    Entity.class
            ).invoke(
                    null,
                    player.level(),
                    player
            );

            Field ceField = vars.getClass()
                    .getDeclaredField("PlayerCursePower");

            Field ceMaxField = vars.getClass()
                    .getDeclaredField("PlayerCursePowerMAX");

            Field lvlField = vars.getClass()
                    .getDeclaredField("PlayerLevel");

            ceField.setAccessible(true);
            ceMaxField.setAccessible(true);
            lvlField.setAccessible(true);

            double currentCE = (Double) ceField.get(vars);
            double currentMax = (Double) ceMaxField.get(vars);
            double playerLevel = (Double) lvlField.get(vars);

            boolean currentlyBoosted =
                    player.getPersistentData()
                            .getBoolean(NBT_BOOSTED);

            double storedBaseMax =
                    player.getPersistentData()
                            .getDouble(NBT_BASE_MAX);

            double lastSetMax =
                    player.getPersistentData()
                            .getDouble(NBT_LAST_SET_MAX);

            if (rikaPresent) {

                boolean hasPureLove = hasAdvancement(
                        player,
                        "jujutsucraft:skill_curseis_lifted"
                );

                double multiplier = hasPureLove ? 5.0D : 2.0D;

                // First activation
                if (!currentlyBoosted || storedBaseMax <= 0.0D) {

                    player.getPersistentData().putDouble(
                            NBT_BASE_MAX,
                            currentMax
                    );

                    player.getPersistentData().putBoolean(
                            NBT_BOOSTED,
                            true
                    );

                    double fillMax = currentMax * multiplier;

                    ceMaxField.set(vars, fillMax);
                    ceField.set(vars, fillMax);

                    syncVariables(vars, player);

                    player.getPersistentData().putDouble(
                            NBT_LAST_SET_MAX,
                            fillMax
                    );

                    return;
                }

                // Detect external changes to maximum CE
                if (lastSetMax > 0.0D
                        && Math.abs(currentMax - lastSetMax) > 1.0D) {

                    storedBaseMax = currentMax;

                    player.getPersistentData().putDouble(
                            NBT_BASE_MAX,
                            currentMax
                    );
                }

                double newMax = storedBaseMax * multiplier;

                // Base cursed-energy regeneration
                double baseRegen =
                        1.0D
                                + ((2.0D + playerLevel) / 1.1D)
                                * 0.2D;

                if (player.getHealth() >= player.getMaxHealth()) {
                    baseRegen *= 2.0D;
                }

                MobEffect zoneFx =
                        ForgeRegistries.MOB_EFFECTS.getValue(
                                new ResourceLocation(
                                        "jujutsucraft",
                                        "zone"
                                )
                        );

                if (zoneFx != null && player.hasEffect(zoneFx)) {
                    baseRegen *= 1.2D;
                }

                double bonusRegen =
                        (double) Math.round(baseRegen)
                                * (multiplier - 1.0D);

                double newCE = Math.min(
                        currentCE + bonusRegen,
                        newMax
                );

                boolean changed = false;

                if (currentMax != newMax) {
                    ceMaxField.set(vars, newMax);
                    changed = true;
                }

                if (newCE != currentCE) {
                    ceField.set(vars, newCE);
                    changed = true;
                }

                if (changed) {
                    syncVariables(vars, player);
                }

                player.getPersistentData().putDouble(
                        NBT_LAST_SET_MAX,
                        newMax
                );

            } else if (currentlyBoosted && storedBaseMax > 0.0D) {

                // Rika is no longer present.
                // Restore the original cursed-energy capacity.
                if (lastSetMax <= 0.0D
                        || Math.abs(currentMax - lastSetMax) < 2.0D) {

                    ceMaxField.set(vars, storedBaseMax);

                    ceField.set(
                            vars,
                            Math.min(currentCE, storedBaseMax)
                    );

                    syncVariables(vars, player);
                }

                player.getPersistentData().remove(NBT_BOOSTED);
                player.getPersistentData().remove(NBT_BASE_MAX);
                player.getPersistentData().remove(NBT_LAST_SET_MAX);
            }

        } catch (Exception ignored) {
            // Preserve original behavior.
        }
    }

    private static boolean hasAdvancement(
            ServerPlayer player,
            String id) {

        Advancement advancement =
                player.server.getAdvancements()
                        .getAdvancement(new ResourceLocation(id));

        return advancement != null
                && player.getAdvancements()
                .getOrStartProgress(advancement)
                .isDone();
    }

    private static void syncVariables(
            Object vars,
            ServerPlayer player
    ) throws Exception {

        vars.getClass()
                .getMethod(
                        "syncPlayerVariables",
                        Entity.class
                )
                .invoke(vars, player);
    }
}
