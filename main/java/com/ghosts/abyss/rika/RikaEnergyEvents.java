package com.ghosts.abyss.rika;

import com.ghosts.abyss.technique.CursedEnergyCapacity;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.LocateRikaProcedure;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

/** Keeps Copy's permanent capacity separate from Rika's summon/regen bonus. */
public final class RikaEnergyEvents {
    private static final String NBT_BOOSTED = "jslop_rika_ce_boosted";
    private static final String NBT_BASE_MAX = "jslop_rika_base_ce_max";
    private static final String NBT_LAST_SET_MAX = "jslop_rika_last_set_max";

    private RikaEnergyEvents() {}

    // Already called on the server by AbyssEventDispatcher.
    public static void onPlayerTick(ServerPlayer player) {
        if (player.tickCount % 10 != 0) return;

        // Re-evaluate after grade changes, respawns, external technique
        // changes and acquisition of skill_curseis_lifted.
        CursedEnergyCapacity.refresh(player);

        var vars = player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY)
                .orElse(null);
        if (vars == null || player.isSpectator()) return;

        // Old temporary capacity values must never restore a stale max.
        var data = player.getPersistentData();
        data.remove(NBT_BASE_MAX);
        data.remove(NBT_LAST_SET_MAX);

        if (vars.PlayerCurseTechnique != 5.0D
                || !LocateRikaProcedure.execute(player.level(), player)) {
            data.remove(NBT_BOOSTED);
            return;
        }

        // Retain the existing refill on Rika's first activation, but refill
        // only to the permanent cap (never multiply that cap).
        if (!data.getBoolean(NBT_BOOSTED)) {
            data.putBoolean(NBT_BOOSTED, true);
            if (vars.PlayerCursePower < vars.PlayerCursePowerMAX) {
                vars.PlayerCursePower = vars.PlayerCursePowerMAX;
                vars.syncPlayerVariables(player);
            }
            return;
        }

        // Preserve the old regeneration bonus: 2x normal Rika, 5x awakened.
        double multiplier = CursedEnergyCapacity.hasCurseLifted(player) ? 5.0D : 2.0D;
        double baseRegen = 1.0D + ((2.0D + vars.PlayerLevel) / 1.1D) * 0.2D;
        if (player.getHealth() >= player.getMaxHealth()) baseRegen *= 2.0D;
        MobEffect zone = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "zone"));
        if (zone != null && player.hasEffect(zone)) baseRegen *= 1.2D;

        double bonus = Math.round(baseRegen) * (multiplier - 1.0D);
        double current = Math.min(vars.PlayerCursePower + bonus, vars.PlayerCursePowerMAX);
        if (Double.compare(vars.PlayerCursePower, current) != 0) {
            vars.PlayerCursePower = current;
            vars.syncPlayerVariables(player);
        }
    }
}
