
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainDimension;
import com.ghosts.abyss.domain.DomainInstanceManager;
import com.ghosts.abyss.effect.ModEffects;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.DomainExpansionEffectExpiresProcedure",
        remap = false
)
public abstract class JujutsuDomainExpirationMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$skipOriginalDomainExpiration(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (living.level().isClientSide) {
            return;
        }

        // Only interfere with JC when Abyss owns this domain.
        boolean registeredOwner =
                DomainInstanceManager.getByOwner(living.getUUID()) != null;

        boolean activeAbyssCaster =
                living.level().dimension().equals(DomainDimension.ID)
                        && (
                        living.getPersistentData().getBoolean("abyss_domain_clashing")
                                || living.getPersistentData().getBoolean("abyss_domain_handled")
                );

        boolean suppressExpiration =
                living.getPersistentData().getBoolean("abyss_domain_suppress_jc_expire");

        if (registeredOwner || activeAbyssCaster || suppressExpiration) {
            ci.cancel();
        }
        }
    }
