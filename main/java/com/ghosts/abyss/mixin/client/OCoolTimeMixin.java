package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.client.AbyssCooldownNumbersOverlay;
import net.mcreator.jujutsucraft.procedures.OCoolTimeProcedure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies Abyss's magic cooldown number at JC's normal HUD position. */
@Mixin(value = OCoolTimeProcedure.class, remap = false)
public class OCoolTimeMixin {

    @Inject(method = "execute", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void abyss$magicCountdown(Entity entity,
                                             CallbackInfoReturnable<String> cir) {
        String number = AbyssCooldownNumbersOverlay.textFor(entity, false);
        if (number != null) {
            cir.setReturnValue(number);
        }
    }
}
