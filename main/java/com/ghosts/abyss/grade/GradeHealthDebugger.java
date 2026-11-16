
package com.ghosts.abyss.grade;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(
        modid = "abyss",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class GradeHealthDebugger {

    // Turn this off after finding the problem.
    private static final boolean ENABLED = true;

    // Same UUID as GradeHealthHandler.
    private static final UUID HEALTH_UUID =
            UUID.fromString("85c4ad72-11db-4e6c-9b47-1b5e31f1c98a");

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<UUID, Snapshot> LAST =
            new HashMap<>();

    private GradeHealthDebugger() {}

    private record Snapshot(
            float health,
            float maxHealth,
            String grade,
            double jcLevel,
            double calculatedBonus,
            double modifierBonus,
            int healthBoostAmplifier
    ) {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (!ENABLED || event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!AbyssGradeManager.hasGrade(player)) {
            return;
        }

        Snapshot current = capture(player);
        UUID id = player.getUUID();

        Snapshot previous = LAST.put(id, current);

        if (previous == null) {
            LOGGER.info(
                    "[Abyss Health Debug] START player={} {}",
                    player.getGameProfile().getName(),
                    describe(current)
            );
            return;
        }

        boolean healthChanged =
                Float.compare(previous.health(), current.health()) != 0;

        boolean maxChanged =
                Float.compare(previous.maxHealth(), current.maxHealth()) != 0;

        boolean gradeChanged =
                !previous.grade().equals(current.grade());

        boolean levelChanged =
                Double.compare(previous.jcLevel(), current.jcLevel()) != 0;

        boolean bonusChanged =
                Double.compare(
                        previous.calculatedBonus(),
                        current.calculatedBonus()
                ) != 0;

        boolean modifierChanged =
                Double.compare(
                        previous.modifierBonus(),
                        current.modifierBonus()
                ) != 0;

        boolean boostChanged =
                previous.healthBoostAmplifier()
                        != current.healthBoostAmplifier();

        if (!healthChanged && !maxChanged
                && !gradeChanged && !levelChanged
                && !bonusChanged && !modifierChanged
                && !boostChanged) {
            return;
        }

        // Log every health-related state change.
        // Ordinary damage and healing will also appear.
        LOGGER.info(
                "[Abyss Health Debug] CHANGE player={} tick={} " +
                        "health={} -> {} max={} -> {} " +
                        "grade={} -> {} jcLevel={} -> {} " +
                        "calculatedBonus={} -> {} " +
                        "abyssModifier={} -> {} " +
                        "healthBoost={} -> {}",
                player.getGameProfile().getName(),
                player.tickCount,
                previous.health(), current.health(),
                previous.maxHealth(), current.maxHealth(),
                previous.grade(), current.grade(),
                previous.jcLevel(), current.jcLevel(),
                previous.calculatedBonus(), current.calculatedBonus(),
                previous.modifierBonus(), current.modifierBonus(),
                previous.healthBoostAmplifier(),
                current.healthBoostAmplifier()
        );

        // Highlight unexplained large drops.
        // This is diagnostic only; it never restores health.
        if (current.health() < previous.health() - 8.0F) {
            LOGGER.warn(
                    "[Abyss Health Debug] LARGE HEALTH DROP player={} " +
                            "health={} -> {} max={} -> {} " +
                            "grade={} jcLevel={} modifier={}",
                    player.getGameProfile().getName(),
                    previous.health(), current.health(),
                    previous.maxHealth(), current.maxHealth(),
                    current.grade(), current.jcLevel(),
                    current.modifierBonus()
            );
        }
    }

    private static Snapshot capture(ServerPlayer player) {

        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).orElse(null);

        double jcLevel = vars != null
                ? vars.PlayerLevel
                : Double.NaN;

        AttributeInstance attribute =
                player.getAttribute(Attributes.MAX_HEALTH);

        AttributeModifier modifier = attribute != null
                ? attribute.getModifier(HEALTH_UUID)
                : null;

        double modifierBonus = modifier != null
                ? modifier.getAmount()
                : 0.0;

        MobEffectInstance healthBoost =
                player.getEffect(MobEffects.HEALTH_BOOST);

        int boostAmplifier = healthBoost != null
                ? healthBoost.getAmplifier()
                : -1;

        return new Snapshot(
                player.getHealth(),
                player.getMaxHealth(),
                AbyssGradeManager.getGrade(player).name(),
                jcLevel,
                GradeHealthCalculator.calculateBonus(player),
                modifierBonus,
                boostAmplifier
        );
    }

    private static String describe(Snapshot state) {
        return "health=" + state.health()
                + " max=" + state.maxHealth()
                + " grade=" + state.grade()
                + " jcLevel=" + state.jcLevel()
                + " calculatedBonus=" + state.calculatedBonus()
                + " abyssModifier=" + state.modifierBonus()
                + " healthBoost=" + state.healthBoostAmplifier();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        LAST.remove(event.getEntity().getUUID());
    }
}
