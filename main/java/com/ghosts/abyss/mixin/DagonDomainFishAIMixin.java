
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DagonDomainFish;
import net.mcreator.jujutsucraft.entity.SeaSerpentEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.AISeaSerpentProcedure",
        remap = false
)
public class DagonDomainFishAIMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$domainFishAI(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (!(entity instanceof SeaSerpentEntity fish)) {
            return;
        }

        if (!DagonDomainFish.isDomainFish(fish)) {
            return;
        }

        // Replace JC's original movement/targeting only
        // for Abyss's domain-summoned fish.
        if (world instanceof ServerLevel level) {
            DagonDomainFish.tickFish(level, fish);
        }

        ci.cancel();
    }
}
