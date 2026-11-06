
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.AbyssDomainCast;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueOkkotsuProcedure",
        remap = false
)
public class YutaDomainExecuteMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceYutaDomain(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        // Only intercept Authentic Mutual Love.
        if (entity.getPersistentData().getDouble("skill") != 520.0D) {
            return;
        }

        // Prevent Jujutsucraft's original domain procedure.
        ci.cancel();

        if (entity.level().isClientSide) return;

        // Preserve the Pure Love advancement requirement.
        if (entity instanceof ServerPlayer player) {

            if (player.getServer() == null) return;

            Advancement advancement = player.getServer()
                    .getAdvancements()
                    .getAdvancement(
                            new ResourceLocation(
                                    "jujutsucraft",
                                    "skill_curseis_lifted"
                            )
                    );

            if (advancement == null
                    || !player.getAdvancements()
                    .getOrStartProgress(advancement)
                    .isDone()) {

                player.displayClientMessage(
                        Component.literal(
                                "Authentic Mutual Love requires Pure Love to have been used first."
                        ),
                        true
                );

                player.getPersistentData().putDouble("skill", 0.0D);
                player.getPersistentData().putDouble("cnt3", 0.0D);
                return;
            }
        }

        // Use the custom Abyss domain system.
        if (entity instanceof LivingEntity living) {
            AbyssDomainCast.tickYutaDomain(world, living);
        }
    }
}
