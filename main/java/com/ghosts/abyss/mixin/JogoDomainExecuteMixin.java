
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
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueJogoProcedure",
        remap = false
)
public class JogoDomainExecuteMixin {

    /**
     * Replace Jogo's original domain skill (420)
     * with Abyss's custom domain system.
     *
     * Other Jogo techniques remain unchanged.
     */
    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceJogoDomain(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        // Jogo's original domain skill is 420.
        if (entity.getPersistentData().getDouble("skill") != 420.0D) {
            return;
        }

        // Prevent Jujutsucraft's original domain creation.
        ci.cancel();

        // Use Abyss's reworked domain system instead.
        if (entity instanceof LivingEntity living) {
            AbyssDomainCast.tickJogoDomain(world, living);
        }
    }
}
