
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.grade.AbyssGradeManager;

import net.mcreator.jujutsucraft.procedures.PlayerPhysicalAbilityProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
        value = PlayerPhysicalAbilityProcedure.class,
        remap = false
)
public abstract class PlayerPhysicalAbilityHealthMixin {

    // Block JC's permanent Health Boost
// for players managed by Abyss.
// Strength and Resistance are intentionally left unchanged.
    @Redirect(
            method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/commands/Commands;m_230957_(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I",
                    remap = false
            ),
            require = 1,
            remap = false
    )
    private static int abyss$interceptGradeEffects(
            Commands commands,
            CommandSourceStack source,
            String command
    ) {

        if (source.getEntity() instanceof ServerPlayer player
                && AbyssGradeManager.hasGrade(player)) {

            if (command.startsWith(
                    "effect give @s minecraft:health_boost "
            )) {
                return 1;
            }

        }

        return commands.performPrefixedCommand(source, command);
    }

    // Prevent JC's original Health Boost healing.
    @Redirect(
            method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_21153_(F)V",
                    remap = false
            ),
            require = 1,
            remap = false
    )
    private static void abyss$preventLegacyHealthBoostHeal(
            LivingEntity entity,
            float health
    ) {

        if (entity instanceof ServerPlayer player
                && AbyssGradeManager.hasGrade(player)) {
            return;
        }

        entity.setHealth(health);
    }
}
