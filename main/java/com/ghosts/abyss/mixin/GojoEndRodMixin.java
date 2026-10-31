
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainDimension;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.ParticleGeneratorProcedure",
        remap = false
)
public class GojoEndRodMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$removeOriginalGojoEndRod(
            LevelAccessor world,
            double caliber_radius,
            double count,
            double inaccuracy,
            double speed,
            double x1,
            double x2,
            double y1,
            double y2,
            double z1,
            double z2,
            String id,
            CallbackInfo ci
    ) {
        // Only target the original End Rod particle.
        if (!"minecraft:end_rod".equals(id)) {
            return;
        }

        // Only suppress it inside the Abyss domain dimension.
        if (!(world instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }

        if (!level.dimension().equals(DomainDimension.ID)) {
            return;
        }

        // Cancel Jujutsucraft's original End Rod generator.
        ci.cancel();
    }
}
