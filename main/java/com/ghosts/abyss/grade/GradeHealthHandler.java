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

/** Uses the same stable max-health modifier UUID as the previous Abyss system. */
@Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GradeHealthHandler {
    private static final UUID HEALTH_UUID =
            UUID.fromString("85c4ad72-11db-4e6c-9b47-1b5e31f1c98a");

    private GradeHealthHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 2 != 0) return;

        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;

        float previousHealth = player.getHealth();

        // Remove only JC's legacy infinite Health Boost. Temporary Health
        // Boost from unrelated gameplay remains untouched.
        var boost = player.getEffect(MobEffects.HEALTH_BOOST);
        if (boost != null && boost.getDuration() > 1_000_000) {
            player.removeEffect(MobEffects.HEALTH_BOOST);
        }

        double bonus = GradeHealthCalculator.calculateBonus(player);
        AttributeModifier current = attribute.getModifier(HEALTH_UUID);
        boolean needsUpdate = (current == null && bonus > 0.0D)
                || (current != null
                && Double.compare(current.getAmount(), bonus) != 0);

        if (needsUpdate) {
            if (current != null) attribute.removeModifier(HEALTH_UUID);
            if (bonus > 0.0D) {
                attribute.addPermanentModifier(new AttributeModifier(
                        HEALTH_UUID,
                        "Abyss JC Grade Max Health",
                        bonus,
                        AttributeModifier.Operation.ADDITION
                ));
            }
        }

        // Removing old effects or replacing the modifier can clamp health.
        // Restore the previous amount, but never exceed the new maximum.
        if (needsUpdate || boost != null && boost.getDuration() > 1_000_000) {
            float restored = Math.min(previousHealth, player.getMaxHealth());
            if (player.getHealth() < restored) player.setHealth(restored);
        }
    }
}
