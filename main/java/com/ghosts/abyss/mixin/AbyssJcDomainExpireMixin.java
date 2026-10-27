
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainDimension;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.DomainExpansionEffectExpiresProcedure",
        remap = false
)
public class AbyssJcDomainExpireMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$skipJcDomainExpiration(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        var data = entity.getPersistentData();

        if (data.getBoolean("abyss_domain_suppress_jc_expire")) {
            ci.cancel();
            return;
        }

        if (entity.level().dimension().equals(DomainDimension.ID)
                && data.getBoolean("abyss_domain_handled")) {
            ci.cancel();
        }
    }
}
