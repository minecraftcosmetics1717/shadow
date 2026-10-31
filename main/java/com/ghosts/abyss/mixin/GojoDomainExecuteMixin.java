
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.AbyssDomainCast;
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
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueGojoProcedure",
        remap = false
)
public class GojoDomainExecuteMixin {

    /**
     * Replace Gojo's original Unlimited Void skill 220
     * with Abyss's custom domain system.
     */
    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceGojoDomain(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        if (entity.getPersistentData().getDouble("skill") != 220.0D) {
            return;
        }

        // Prevent JujutsuCraft's original Unlimited Void
        // domain creation procedure from running.
        ci.cancel();

        // Run the Abyss domain system instead.
        if (entity instanceof LivingEntity living) {
            AbyssDomainCast.tickGojoDomain(world, living);
        }
    }
}
