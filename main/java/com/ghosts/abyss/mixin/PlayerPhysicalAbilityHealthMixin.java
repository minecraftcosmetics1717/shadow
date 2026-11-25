package com.ghosts.abyss.mixin;

import net.mcreator.jujutsucraft.procedures.PlayerPhysicalAbilityProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep JC's other grade effects, but never its permanent Health Boost. */
@Mixin(value = PlayerPhysicalAbilityProcedure.class, remap = false)
public abstract class PlayerPhysicalAbilityHealthMixin {

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
        // No stored Abyss-grade check: every JC player uses the attribute
        // health replacement, whether Ungraded or Special Grade.
        if (source.getEntity() instanceof ServerPlayer
                && command.startsWith("effect give @s minecraft:health_boost ")) {
            return 1;
        }
        return commands.performPrefixedCommand(source, command);
    }

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
        if (entity instanceof ServerPlayer) return;
        entity.setHealth(health);
    }
}
