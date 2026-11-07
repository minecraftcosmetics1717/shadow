
package com.ghosts.abyss.blood;

import com.ghosts.abyss.Abyss;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Abyss.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class BloodManipulationEvents {

    private static final String KEY_TRANSFORMED =
            "abyss_death_womb_painting";

    private static final String KEY_LAST_SKILL =
            "abyss_blood_last_skill";

    private static final String KEY_LAST_COUNT =
            "abyss_blood_last_count";

    private BloodManipulationEvents() {}

    // Jujutsucraft Choso technique IDs.
    private static boolean isBloodSkill(int skill) {
        return switch (skill) {
            case 1005, // Slicing Exorcism
                 1006, // Convergence
                 1007, // Piercing Blood
                 1008, // Supernova
                 1009, // Flowing Red Scale
                 1018, // Wing King
                 1019  // Kechizu Poison
                    -> true;
            default -> false;
        };
    }

    private static float getSelfDamage(int skill) {
        return switch (skill) {
            case 1005 -> 4.0f;
            case 1006 -> 2.0f;
            case 1007 -> 8.0f;
            case 1008 -> 2.0f;
            case 1009 -> 2.0f;
            case 1018 -> 2.0f;
            case 1019 -> 1.0f;
            default -> 0.0f;
        };
    }

    // Exclude Jujutsucraft's water variant
    // of Piercing Blood.
    private static boolean isWaterTechnique(Player player) {
        var vars = player.getCapability(
                JujutsucraftModVariables
                        .PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);

        if (vars == null) {
            return false;
        }

        double activeTechnique = vars.SecondTechnique
                ? vars.PlayerCurseTechnique2
                : vars.PlayerCurseTechnique;

        return activeTechnique == 6.0D;
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag data = player.getPersistentData();

        int skill = (int) data.getDouble("skill");
        double count = data.getDouble("cnt1");

        int previousSkill = data.getInt(KEY_LAST_SKILL);
        double previousCount =
                data.getDouble(KEY_LAST_COUNT);

        boolean started =
                isBloodSkill(skill)
                        && (previousSkill != skill
                        || count < previousCount);

        // Remember state for the next tick.
        data.putInt(KEY_LAST_SKILL, skill);
        data.putDouble(KEY_LAST_COUNT, count);

        if (!started) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        if (DeathWombPainting.is(player)) {
            return;
        }

        if (skill == 1007 && isWaterTechnique(player)) {
            return;
        }

        float damage = getSelfDamage(skill);

        if (damage > 0.0f) {
            player.hurt(
                    player.damageSources().magic(),
                    damage
            );
        }
    }

    // Preserve transformation when the player
    // entity is recreated.
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag oldData =
                event.getOriginal().getPersistentData();

        CompoundTag newData =
                event.getEntity().getPersistentData();

        if (oldData.getBoolean(KEY_TRANSFORMED)) {
            newData.putBoolean(
                    KEY_TRANSFORMED,
                    true
            );
        }

        // Do not preserve active casting state.
        newData.remove(KEY_LAST_SKILL);
        newData.remove(KEY_LAST_COUNT);
    }
}
