
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
            at = @At("RETURN"),
            remap = false
    )
    private static void abyss$onShutterDoors(
            LevelAccessor world,
            Entity entity,
            CallbackInfo ci
    ) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        // Server-side only.
        if (entity.level().isClientSide) {
            return;
        }

        // Jujutsucraft increments cnt1 inside execute().
        // cnt1 == 1 means this is the first tick
        // of the Shutter Doors technique.
        if (entity.getPersistentData().getDouble("cnt1") != 1.0D) {
            return;
        }

        // Register exactly one 50% gambling attempt
        // for this Shutter Doors activation.
        HakariGambleManager.usedDoors(living);
    }
}
