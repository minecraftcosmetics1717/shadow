
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
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueMahitoProcedure",
        remap = false
)
public class MahitoDomainExecuteMixin {

    /**
     * Replace Mahito's original domain skill 1520 with Abyss.
     * Other Mahito skills continue normally.
     */
    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceMahitoDomain(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        if (entity.getPersistentData().getDouble("skill") != 1520.0D) {
            return;
        }

        // Skip JujutsuCraft's original domain creation.
        ci.cancel();

        if (entity instanceof LivingEntity living) {
            AbyssDomainCast.tickMahitoDomain(world, living);
        }
    }
}
