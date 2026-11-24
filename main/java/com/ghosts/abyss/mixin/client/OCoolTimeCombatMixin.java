package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.client.AbyssCooldownNumbersOverlay;
import net.mcreator.jujutsucraft.procedures.OCoolTimeCombatProcedure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies Abyss's combat cooldown number at JC's normal HUD position. */
@Mixin(value = OCoolTimeCombatProcedure.class, remap = false)
public class OCoolTimeCombatMixin {

    @Inject(method = "execute", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void abyss$combatCountdown(Entity entity,
                                              CallbackInfoReturnable<String> cir) {
        String number = AbyssCooldownNumbersOverlay.textFor(entity, true);
        if (number != null) {
            cir.setReturnValue(number);
        }
    }
}
