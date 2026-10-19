package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.effect.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = "abyss")
public class KaoriSelfLapseTickHandler {

    private static final double CE_DRAIN_COST = 40.0D;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        boolean isActive = player.getPersistentData().getBoolean("jreborn_selflapse_active");
        if (!isActive) return;

        // Check if player has jujutsucraft:unstable -> Force exit Self-Lapse
        MobEffect unstableEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "unstable"));
        if (unstableEffect != null && player.hasEffect(unstableEffect)) {
            player.getPersistentData().putBoolean("jreborn_selflapse_active", false);
            return;
        }

        // Apply custom flight effect every tick (10 tick buffer)
        player.addEffect(new MobEffectInstance(ModEffects.FLIGHT.get(), 10, 0, false, false, false));

        // Deduct CE every 20 ticks (1 second)
        if (player.tickCount % 20 == 0) {
            try {
                Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
                Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
                capField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Capability<Object> cap = (Capability<Object>) capField.get(null);

                Object vars = player.getCapability(cap, null).orElse(null);
                if (vars != null) {
                    Field ceField = vars.getClass().getDeclaredField("PlayerCursePower");
                    ceField.setAccessible(true);
                    double currentCE = (Double) ceField.get(vars);

                    if (currentCE >= CE_DRAIN_COST) {
                        // Deduct CE
                        ceField.set(vars, currentCE - CE_DRAIN_COST);

                        // Sync capability variable back
                        try {
                            Method syncMethod = varsClass.getMethod("syncPlayerVariables", net.minecraft.world.entity.Entity.class);
                            syncMethod.invoke(null, player);
                        } catch (NoSuchMethodException ignored) {
                        }
                    } else {
                        // Insufficient CE -> Automatically turn off
                        player.getPersistentData().putBoolean("jreborn_selflapse_active", false);
                    }
                }
            } catch (Exception e) {
                Abyss.LOGGER.error("[JReborn] Error processing Self-Lapse tick drain", e);
            }
        }
    }
}