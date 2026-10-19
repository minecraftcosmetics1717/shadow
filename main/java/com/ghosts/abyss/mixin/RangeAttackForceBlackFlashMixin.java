package com.ghosts.abyss.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.RangeAttackProcedure", remap = false)
public class RangeAttackForceBlackFlashMixin {

    /**
     * Tunable BF multiplier applied AFTER DamageFix.
     * With previous *4.0 you saw ~279 dmg; ~0.45 should land near ~30.
     * Raise/lower after testing.
     */
    private static final double BF_DAMAGE_MULT = 2D;

    /**
     * After DamageFix inside RangeAttack:
     * if this is a timed DF BF hit, apply our BF multiplier once.
     */
    @Inject(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/mcreator/jujutsucraft/procedures/DamageFixProcedure;execute(Lnet/minecraft/world/entity/Entity;)V",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private static void jreborn$afterDamageFix(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;
        if (!entity.getPersistentData().getBoolean("jreborn_force_bf")) return;

        double damage = entity.getPersistentData().getDouble("Damage");
        entity.getPersistentData().putDouble("Damage", damage * BF_DAMAGE_MULT);

        // Consume so it only affects this hit
        entity.getPersistentData().putBoolean("jreborn_force_bf", false);
    }

    /**
     * Remember a nearby victim around the attack point so DF BF can track them.
     */
    @Inject(
            method = "execute",
            at = @At("TAIL"),
            remap = false
    )
    private static void jreborn$rememberDfTarget(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        // Only while a skill/attack is happening
        if (entity.getPersistentData().getDouble("skill") == 0.0D
                && !entity.getPersistentData().getBoolean("attack")) {
            return;
        }

        LivingEntity best = null;
        double bestDist = 3.0D * 3.0D;

        for (LivingEntity other : world.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(x - 3.0D, y - 3.0D, z - 3.0D, x + 3.0D, y + 3.0D, z + 3.0D),
                e -> e.isAlive() && e != entity
        )) {
            double d = other.distanceToSqr(x, y, z);
            if (d < bestDist) {
                bestDist = d;
                best = other;
            }
        }

        if (best != null) {
            entity.getPersistentData().putUUID("jreborn_df_last_target", best.getUUID());
        }
    }
}