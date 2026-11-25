package com.ghosts.abyss.event;

import com.ghosts.abyss.effect.ModEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = "abyss")
public final class FlightEffectHandler {

    private static final int URO_TECHNIQUE_ID = 38;
    private static final int PERMANENT_DURATION = Integer.MAX_VALUE;

    private FlightEffectHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        boolean blocked = player.hasEffect(
                JujutsucraftModMobEffects.UNSTABLE.get())
                || player.hasEffect(
                JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());

        boolean isUro = isUroTechnique(player);
        MobEffectInstance flight = player.getEffect(ModEffects.FLIGHT.get());

        // Match existing Uro behavior: UNSTABLE or DA removes Uro's effect;
        // it is granted again when both blockers are gone. For other sources
        // of abyss:flight, keep the effect but temporarily revoke flight.
        if (blocked) {
            if (isUro && flight != null) {
                player.removeEffect(ModEffects.FLIGHT.get());
            }
        } else if (isUro && flight == null) {
            player.addEffect(new MobEffectInstance(
                    ModEffects.FLIGHT.get(),
                    PERMANENT_DURATION,
                    0,
                    false, // Ambient
                    false, // Particles
                    false  // Effect icon
            ));
        }

        boolean hasFlight = !blocked && player.hasEffect(ModEffects.FLIGHT.get());
        if (hasFlight) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else if (!player.isCreative() && !player.isSpectator()) {
            if (player.getAbilities().mayfly || player.getAbilities().flying) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getEffect() != ModEffects.FLIGHT.get()) return;

        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private static boolean isUroTechnique(ServerPlayer player) {
        try {
            Class<?> varsClass = Class.forName(
                    "net.mcreator.jujutsucraft.network.JujutsucraftModVariables"
            );

            Field capField = varsClass.getDeclaredField(
                    "PLAYER_VARIABLES_CAPABILITY"
            );
            capField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Capability<Object> cap =
                    (Capability<Object>) capField.get(null);

            Object vars = player.getCapability(cap, null).orElse(null);

            if (vars != null) {
                Field secondField = vars.getClass().getDeclaredField(
                        "SecondTechnique"
                );
                secondField.setAccessible(true);

                boolean secondActive = (Boolean) secondField.get(vars);

                Field techField = vars.getClass().getDeclaredField(
                        secondActive
                                ? "PlayerCurseTechnique2"
                                : "PlayerCurseTechnique"
                );
                techField.setAccessible(true);

                double techniqueId = (Double) techField.get(vars);

                return (int) techniqueId == URO_TECHNIQUE_ID;
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
        }

        return false;
    }
}
