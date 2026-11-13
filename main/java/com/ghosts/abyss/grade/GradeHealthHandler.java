
package com.ghosts.abyss.grade;

import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "abyss",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class GradeHealthHandler {

    private static final UUID HEALTH_UUID =
            UUID.fromString("85c4ad72-11db-4e6c-9b47-1b5e31f1c98a");

    private GradeHealthHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        if (!(event.player instanceof ServerPlayer player)) return;

        if (!AbyssGradeManager.hasGrade(player)) return;

        // Check every 2 ticks to track changes in special buffs.
        if (player.tickCount % 2 != 0) return;

        AttributeInstance attribute =
                player.getAttribute(Attributes.MAX_HEALTH);

        if (attribute == null) return;

        double bonus = GradeHealthCalculator.calculateBonus(player);

        AttributeModifier existing =
                attribute.getModifier(HEALTH_UUID);

        if (existing != null && existing.getAmount() != bonus) {
            attribute.removeModifier(HEALTH_UUID);
            existing = null;
        }

        if (bonus > 0.0 && existing == null) {
            attribute.addPermanentModifier(
                    new AttributeModifier(
                            HEALTH_UUID,
                            "Abyss Grade Health",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    )
            );
        }

        if (bonus <= 0.0 && existing != null) {
            attribute.removeModifier(HEALTH_UUID);
        }

        // Remove legacy infinite Health Boost from JC.
        // Temporary Health Boost effects are left untouched.
        var healthBoost = player.getEffect(MobEffects.HEALTH_BOOST);

        if (healthBoost != null
                && healthBoost.getDuration() > 1_000_000) {
            player.removeEffect(MobEffects.HEALTH_BOOST);
        }
    }
}
