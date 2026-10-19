package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.capability.LifeCharmsCapability;
import com.ghosts.abyss.capability.ModCapabilities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss")
public class LifeCharmsEvents {

    // ===== Attach capability =====
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    new ResourceLocation("abyss", "life_charms"),
                    new LifeCharmsCapability()
            );
        }
    }

    // ===== Copy data on death / respawn =====
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(ModCapabilities.LIFE_CHARMS).ifPresent(oldCap -> {
            event.getEntity().getCapability(ModCapabilities.LIFE_CHARMS).ifPresent(newCap -> {
                newCap.getInventory().deserializeNBT(oldCap.getInventory().serializeNBT());
            });
        });
        event.getOriginal().invalidateCaps();
    }

    // ===== Totem protection =====
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;

        boolean used = player.getCapability(ModCapabilities.LIFE_CHARMS)
                .map(cap -> cap.tryConsumeTotem())
                .orElse(false);

        if (!used) return;

        event.setCanceled(true);

        player.setHealth(1.0F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));

        player.level().broadcastEntityEvent(player, (byte) 35);
    }
}