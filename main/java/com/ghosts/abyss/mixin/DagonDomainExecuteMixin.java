
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
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueDagonProcedure",
        remap = false
)
public class DagonDomainExecuteMixin {

    /**
     * Completely replaces Dagon's JC domain skill (820).
     * Other Dagon skills continue through JujutsuCraft normally.
     */
    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceDagonDomain(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) {
            return;
        }

        double skill = entity.getPersistentData().getDouble("skill");

        // Only replace Dagon's domain skill.
        if (skill != 820.0) {
            return;
        }

        // Never allow JC's original Dagon domain procedure to execute.
        ci.cancel();

        // Run our custom domain logic on the server-side living entity.
        if (entity instanceof LivingEntity living) {
            AbyssDomainCast.tickDagonDomain(world, living);
        }
    }
}
