package com.ghosts.abyss.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public class XaeroMinimapBridge {

    public static final ResourceLocation NO_MINIMAP_ID =
            new ResourceLocation("xaerominimap", "no_minimap");

    public static boolean clientUnlocked = false;
    public static boolean clientEnabled = false;

    public static void applyTo(Player player) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(NO_MINIMAP_ID);
        if (effect == null) return;

        // Server + persistent data are source of truth; client statics are only a fallback
        boolean unlocked;
        boolean enabled;
        if (!player.level().isClientSide) {
            unlocked = player.getPersistentData().getBoolean("minimap_unlocked");
            enabled  = player.getPersistentData().getBoolean("minimap_enabled");
        } else {
            unlocked = clientUnlocked;
            enabled  = clientEnabled;
        }

        boolean shouldBlock = !unlocked || !enabled;

        if (shouldBlock) {
            player.addEffect(new MobEffectInstance(effect, -1, 0, false, false, false));
        } else {
            player.removeEffect(effect);
        }
    }
}