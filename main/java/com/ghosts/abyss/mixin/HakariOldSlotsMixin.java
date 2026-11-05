
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.HakariGambleManager;
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
        targets = "net.mcreator.jujutsucraft.procedures.IdleDeathGambleActiveProcedure",
        remap = false
)
public class HakariOldSlotsMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$disableOldSlots(
            LevelAccessor world,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity instanceof LivingEntity living
                && HakariGambleManager.isHakariOwner(living)) {

            ci.cancel();
        }
    }
}
