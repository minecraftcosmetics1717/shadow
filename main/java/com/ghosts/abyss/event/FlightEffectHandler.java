package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.effect.ModEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = "abyss")
public class FlightEffectHandler {

    // 1. Intercept attempts to apply jujutsucraft:fly_effect
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        MobEffectInstance incomingInstance = event.getEffectInstance();
        MobEffect targetEffect = incomingInstance.getEffect();

        MobEffect jujutsuFlyEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "fly_effect"));

        // If something in Jujutsucraft tries to apply jujutsucraft:fly_effect
        if (jujutsuFlyEffect != null && targetEffect == jujutsuFlyEffect) {

            // Give custom flight effect with at least 100 ticks (5 seconds) buffer so jumping doesn't instantly lose flight
            int bufferedDuration = Math.max(incomingInstance.getDuration(), 100);

            player.addEffect(new MobEffectInstance(
                    ModEffects.FLIGHT.get(),
                    bufferedDuration,
                    incomingInstance.getAmplifier(),
                    incomingInstance.isAmbient(),
                    incomingInstance.isVisible(),
                    incomingInstance.showIcon()
            ));

            // Completely DENY jujutsucraft:fly_effect
            event.setResult(Event.Result.DENY);
        }
    }

    // 2. Grant creative-style flight while custom flight effect is active & sustain Uro mid-air (unless Unstable)
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide()) return;

        if (player instanceof ServerPlayer serverPlayer) {
            MobEffect unstableEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "unstable"));
            boolean hasUnstable = unstableEffect != null && serverPlayer.hasEffect(unstableEffect);

            // If player is Uro (38)
            if (isUroTechnique(serverPlayer)) {
                if (hasUnstable) {
                    // Instantly strip custom flight if Uro gets the unstable effect
                    if (serverPlayer.hasEffect(ModEffects.FLIGHT.get())) {
                        serverPlayer.removeEffect(ModEffects.FLIGHT.get());
                    }
                } else {
                    // Otherwise maintain flight if airborne/active
                    if (!serverPlayer.hasEffect(ModEffects.FLIGHT.get())) {
                        serverPlayer.addEffect(new MobEffectInstance(ModEffects.FLIGHT.get(), 100, 0, false, false, false));
                    }
                }
            }
        }

        boolean hasFlightEffect = player.hasEffect(ModEffects.FLIGHT.get());
        boolean isCreativeOrSpectator = player.isCreative() || player.isSpectator();

        if (hasFlightEffect) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else if (!isCreativeOrSpectator) {
            if (player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    // 3. Clean up abilities immediately if flight effect is removed
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getEffect() == ModEffects.FLIGHT.get()) {
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    // Helper method to check if player is using Takako Uro
    private static boolean isUroTechnique(ServerPlayer player) {
        try {
            Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
            Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
            capField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Capability<Object> cap = (Capability<Object>) capField.get(null);

            Object vars = player.getCapability(cap, null).orElse(null);
            if (vars != null) {
                Field secondField = vars.getClass().getDeclaredField("SecondTechnique");
                secondField.setAccessible(true);
                boolean secondActive = (Boolean) secondField.get(vars);

                Field techField = vars.getClass().getDeclaredField(secondActive ? "PlayerCurseTechnique2" : "PlayerCurseTechnique");
                techField.setAccessible(true);
                double techniqueId = (Double) techField.get(vars);
                return (int) techniqueId == 38;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}