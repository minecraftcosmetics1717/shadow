
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
        targets = "net.mcreator.jujutsucraft.procedures.SkillDoorsProcedure",
        remap = false
)
public class HakariDoorsGambleMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            remap = false
    )
    private static void abyss$onShutterDoors(
            LevelAccessor world,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity instanceof LivingEntity living
                && !entity.level().isClientSide) {

            HakariGambleManager.usedDoors(living);
        }
    }
}
