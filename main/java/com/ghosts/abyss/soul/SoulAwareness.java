
package com.ghosts.abyss.soul;

import net.mcreator.jujutsucraft.entity.MahitoEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class SoulAwareness {

    public static final int MAX = 50;

    private static final String KEY = "abyss_soul_awareness";
    private static final String INITIALIZED =
            "abyss_soul_awareness_initialized";

    private SoulAwareness() {}

    public static int get(LivingEntity entity) {
        initialize(entity);
        return Math.max(0, Math.min(MAX,
                entity.getPersistentData().getInt(KEY)));
    }

    public static void set(LivingEntity entity, int value) {
        initialize(entity);
        entity.getPersistentData().putInt(
                KEY,
                Math.max(0, Math.min(MAX, value))
        );
    }

    public static void add(LivingEntity entity, int amount) {
        set(entity, get(entity) + amount);
    }


    public static void initialize(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();

        boolean initialized = data.getBoolean(INITIALIZED);

        // Existing value, or zero for newly initialized entities.
        int awareness = initialized
                ? data.getInt(KEY)
                : 0;

        int starting = entity instanceof MahitoEntity ? 30 : 0;

        if (entity instanceof Player player) {
            JujutsucraftModVariables.PlayerVariables vars =
                    player.getCapability(
                            JujutsucraftModVariables
                                    .PLAYER_VARIABLES_CAPABILITY
                    ).orElse(null);

            if (vars != null) {
                // Mahito technique.
                if (vars.PlayerCurseTechnique == 15.0D
                        || vars.PlayerCurseTechnique2 == 15.0D) {
                    starting = Math.max(starting, 30);
                }

                // Six Eyes.
                if (vars.FlagSixEyes) {
                    starting = Math.max(starting, 15);
                }
            }
        }

        // Temporary Heavenly Restriction check.
        if (entity.hasEffect(
                JujutsucraftModMobEffects.PHYSICAL_GIFTED_EFFECT.get())) {

            var effect = entity.getEffect(
                    JujutsucraftModMobEffects
                            .PHYSICAL_GIFTED_EFFECT.get()
            );

            if (effect != null && effect.getAmplifier() > 0) {
                starting = 50;
            }
        }

        // Never reduce earned awareness.
        awareness = Math.max(awareness, starting);

        data.putInt(KEY, Math.min(MAX, awareness));
        data.putBoolean(INITIALIZED, true);
    }


    public static double damageMultiplier(
            LivingEntity attacker,
            LivingEntity defender
    ) {
        int attackAwareness = get(attacker);
        int defenseAwareness = get(defender);

        double attackPower =
                1.25 + 0.025 * (attackAwareness - 30);

        double defenseFactor =
                Math.pow(1.0 - defenseAwareness / 50.0, 2.0);

        double minimumDamage =
                0.10 * (attackAwareness / 50.0);

        return Math.max(
                minimumDamage,
                attackPower * defenseFactor
        );
    }

    public static float calculateDamage(
            LivingEntity attacker,
            LivingEntity defender
    ) {
        return (float) (
                defender.getMaxHealth()
                        * damageMultiplier(attacker, defender)
        );
    }
}
