package com.ghosts.abyss.mixin;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.WhenEntityDieProcedure"},
        remap = false
)
public class WhenEntityDieFameMixin {

    @SuppressWarnings({"UnresolvedMixinReference", "MixinAnnotationTarget", "InvalidInjectorMethodSignature"})
    @Redirect(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    // We match both mapped and unmapped invocations of hasEffect on LivingEntity
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_21023_(Lnet/minecraft/world/effect/MobEffect;)Z",
                    ordinal = 1
            ),
            require = 0,
            remap = false
    )
    private static boolean bypassSukunaFameBlock(LivingEntity entity, MobEffect pEffect) {
        // Return false during fame calculation so Jujutsucraft allows Sukuna users to get fame
        return false;
    }
}