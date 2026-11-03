
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainDimension;
import net.mcreator.jujutsucraft.procedures.CoffinoftheIronMountainActiveProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        value = CoffinoftheIronMountainActiveProcedure.class,
        remap = false
)
public class JogoDomainActiveMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$skipOriginalJogoDomain(
            LevelAccessor world,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        if (!entity.level().dimension().equals(DomainDimension.ID)) {
            return;
        }

        if (entity.getPersistentData().getDouble("skill_domain") != 4.0D) {
            return;
        }

        // Diagnostic: disable original Jogo domain attacks
        // inside Abyss's custom dimension.
        ci.cancel();
    }
}
