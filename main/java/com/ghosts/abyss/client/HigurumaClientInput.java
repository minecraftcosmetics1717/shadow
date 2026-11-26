package com.ghosts.abyss.client;

import com.ghosts.abyss.domain.DomainDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Handles a right-click even when the defendant is beyond vanilla interaction reach. */
@Mod.EventBusSubscriber(modid = "abyss", value = Dist.CLIENT)
public final class HigurumaClientInput {
    private HigurumaClientInput() {}
    private static boolean useHeld;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.options.keyUse.isDown()) useHeld = false;
    }

    @SubscribeEvent
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        if (!mc.level.dimension().equals(DomainDimension.ID)) return;
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "domain_expansion"));
        if (effect == null || !mc.player.hasEffect(effect)) return;
        if (useHeld) return;
        useHeld = true;
        // The server checks ownership, active court, line of sight and trial state.
        mc.player.connection.sendCommand("abyss_trial select");
    }
}
