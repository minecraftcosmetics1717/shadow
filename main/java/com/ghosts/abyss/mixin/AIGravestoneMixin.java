
package com.ghosts.abyss.mixin;

import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.AIGravestoneProcedure",
        remap = false
)
public class AIGravestoneMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$keepDomainDecorations(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        if (!entity.getPersistentData().getBoolean(
                "abyss_domain_decoration"
        )) {
            return;
        }

        // Preserve the centerpiece's original particle effects,
        // without allowing Jujutsucraft to remove the decoration.
        ResourceLocation id =
                ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        if (id != null && id.equals(
                new ResourceLocation(
                        "jujutsucraft",
                        "entity_unlimited_void"
                )
        )) {
            if (world instanceof ServerLevel level) {

                // Center of the actual entity, not the arena.
                double px = entity.getX();
                double py = entity.getY()
                        + entity.getBbHeight() * 0.5;
                double pz = entity.getZ();

                // Jujutsucraft's hole effect, every 6 ticks.
                if (entity.tickCount % 6 == 0) {

                    level.sendParticles(
                            JujutsucraftModParticleTypes
                                    .PARTICLE_UNLIMITED_VOID_HOLE.get(),
                            px, py, pz,
                            1,
                            0.0, 0.0, 0.0,
                            0.0
                    );
                }
            }
        }

        // Prevent Jujutsucraft's original decoration
        // cleanup/ownership logic from running.
        ci.cancel();
    }
}
