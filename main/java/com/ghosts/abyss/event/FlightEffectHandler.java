
package com.ghosts.abyss.event;

import com.ghosts.abyss.effect.ModEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

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

        MobEffect unstable = ForgeRegistries.MOB_EFFECTS.getValue(
                new ResourceLocation("jujutsucraft", "unstable")
        );

        boolean isUro = isUroTechnique(player);
        boolean hasUnstable =
                unstable != null && player.hasEffect(unstable);

        MobEffectInstance flight =
                player.getEffect(ModEffects.FLIGHT.get());

        if (isUro && !hasUnstable) {

            // Only grant once, rather than resetting the duration.
            if (flight == null) {
                player.addEffect(new MobEffectInstance(
                        ModEffects.FLIGHT.get(),
                        PERMANENT_DURATION,
                        0,
                        false,  // Ambient
                        false,  // Show particles
                        false   // Show effect icon
                ));
            }

        } else if (flight != null && hasUnstable && isUro) {

            // Unstable disables Uro's flight.
            player.removeEffect(ModEffects.FLIGHT.get());
        }

        boolean hasFlight = player.hasEffect(ModEffects.FLIGHT.get());

        if (hasFlight) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else if (!player.isCreative() && !player.isSpectator()) {
            if (player.getAbilities().mayfly
                    || player.getAbilities().flying) {
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
