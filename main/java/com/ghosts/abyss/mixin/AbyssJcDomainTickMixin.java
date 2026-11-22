
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainDimension;
import com.ghosts.abyss.domain.OpenDomainLifecycle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.DomainExpansionOnEffectActiveTickProcedure",
        remap = false
)
public class AbyssJcDomainTickMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$skipJcDomainTick(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        // Abyss owns the active open Malevolent Shrine.
        if (OpenDomainLifecycle.isActive(entity.getUUID())) {
            ci.cancel();
            return;
        }

        // Preserve existing closed-domain suppression.
        if (entity.level().dimension().equals(DomainDimension.ID)
                && entity.getPersistentData()
                .getBoolean("abyss_domain_handled")) {
            ci.cancel();
        }
    }
}
