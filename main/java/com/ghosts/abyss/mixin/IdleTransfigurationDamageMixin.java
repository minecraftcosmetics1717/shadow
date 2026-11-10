
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.soul.SoulAwareness;
import net.mcreator.jujutsucraft.procedures.ChangeDamage1Procedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChangeDamage1Procedure.class, remap = false)
public class IdleTransfigurationDamageMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void abyss$soulAwarenessDamage(
            LevelAccessor world,
            Entity attacker,
            Entity target,
            CallbackInfoReturnable<Double> cir
    ) {
        if (attacker == null || target == null) {
            return;
        }

        // Only modify Idle Transfiguration (skill 1505).
        if (attacker.getPersistentData().getDouble("skill")
                != 1505.0D) {
            return;
        }

        if (!(attacker instanceof LivingEntity mahito)
                || !(target instanceof LivingEntity defender)) {
            return;
        }

        if (attacker.level().isClientSide()) {
            return;
        }

        // Desired damage from our Soul Awareness system.
        double desiredDamage =
                SoulAwareness.calculateDamage(mahito, defender);

        // RangeAttackProcedure multiplies its existing damage
        // by the value returned from ChangeDamage1Procedure.
        double originalDamage =
                attacker.getPersistentData().getDouble("Damage");

        if (originalDamage <= 0.0D) {
            cir.setReturnValue(0.0D);
            return;
        }

        // Convert the desired damage into a multiplier.
        cir.setReturnValue(desiredDamage / originalDamage);
    }
}
