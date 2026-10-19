package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = "abyss")
public class BrainDamageCleanupHandler {

    private static MobEffect brainDamageEffect = null;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;

        if (event.player instanceof ServerPlayer player) {
            if (brainDamageEffect == null) {
                brainDamageEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "brain_damage"));
            }

            if (brainDamageEffect != null && player.hasEffect(brainDamageEffect)) {
                MobEffectInstance instance = player.getEffect(brainDamageEffect);

                if (instance != null && instance.getDuration() <= 0) {

                    player.removeEffect(brainDamageEffect);

                    player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
                        try {
                            Field brainDamageField = vars.getClass().getDeclaredField("cnt_brain_damage");
                            brainDamageField.setAccessible(true);
                            brainDamageField.set(vars, 0.0D);

                            vars.syncPlayerVariables(player);
                        } catch (Exception ignored) {
                            vars.syncPlayerVariables(player);
                        }
                    });
                }
            }
        }
    }
}