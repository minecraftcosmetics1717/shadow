
package com.ghosts.abyss.event;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles player abilities that require
 * periodic server-side updates.
 *
 * Prepared replacement for EventListeners.
 *
 * Intentionally not registered to Forge's
 * event bus until the final migration.
 */
public final class PlayerAbilityEvents {

    private PlayerAbilityEvents() {}

    /**
     * Called once per server player tick
     * after the final migration.
     */
    public static void onPlayerTick(ServerPlayer player) {
        applySpecialGradeDoubleJump(player);
    }

    /**
     * Gives Special Grade sorcerers the
     * enhanced double-jump effect.
     *
     * Preserves the original behavior.
     */
    private static void applySpecialGradeDoubleJump(
            ServerPlayer player) {

        // Run every other tick.
        if (player.tickCount % 2 != 1) {
            return;
        }

        if (!hasAdvancement(
                player,
                "jujutsucraft:sorcerer_grade_special"
        )) {
            return;
        }

        // Only increase the effect while grounded.
        if (!player.onGround()) {
            return;
        }

        MobEffect doubleJumpEffect =
                ForgeRegistries.MOB_EFFECTS.getValue(
                        new ResourceLocation(
                                "jujutsucraft",
                                "double_jump_effect"
                        )
                );

        if (doubleJumpEffect == null) {
            return;
        }

        MobEffectInstance existing =
                player.getEffect(doubleJumpEffect);

        int currentAmp =
                existing != null
                        ? existing.getAmplifier()
                        : -1;

        // Increase the amplifier up to level 4.
        if (currentAmp < 4) {

            player.addEffect(
                    new MobEffectInstance(
                            doubleJumpEffect,
                            999999,
                            currentAmp + 1,
                            false,
                            false
                    )
            );

        } else if (existing.getDuration() < 20) {

            // Refresh the effect before expiration.
            player.addEffect(
                    new MobEffectInstance(
                            doubleJumpEffect,
                            999999,
                            4,
                            false,
                            false
                    )
            );
        }
    }

    /**
     * Checks whether the player has completed
     * a specific advancement.
     */
    private static boolean hasAdvancement(
            ServerPlayer player,
            String advancementId) {

        Advancement advancement =
                player.server.getAdvancements()
                        .getAdvancement(
                                new ResourceLocation(advancementId)
                        );

        return advancement != null
                && player.getAdvancements()
                .getOrStartProgress(advancement)
                .isDone();
    }
}
