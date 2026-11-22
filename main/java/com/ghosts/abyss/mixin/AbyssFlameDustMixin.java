
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.OpenDomainLifecycle;
import net.mcreator.jujutsucraft.entity.FlameArrowEntity;
import net.mcreator.jujutsucraft.procedures.GetEntityFromUUIDProcedure;
import net.mcreator.jujutsucraft.procedures.LogicOwnerExistProcedure;
import net.minecraft.server.level.ServerLevel;
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
        targets = "net.mcreator.jujutsucraft.procedures.AIFlameArrowProcedure",
        remap = false
)
public class AbyssFlameDustMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            remap = false
    )
    private static void abyss$transferOpenDomainDust(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (!(world instanceof ServerLevel)) {
            return;
        }

        if (!(entity instanceof FlameArrowEntity flame)) {
            return;
        }

        // Match JC's two synchronization checks.
        if (!flame.getEntityData().get(FlameArrowEntity.DATA_sync)
                || !flame.getEntityData().get(FlameArrowEntity.DATA_sync2)) {
            return;
        }

        // Use JC's original owner lookup.
        if (!LogicOwnerExistProcedure.execute(world, flame)) {
            return;
        }

        Entity caster = GetEntityFromUUIDProcedure.execute(
                world,
                flame.getPersistentData().getString("OWNER_UUID")
        );

        if (!(caster instanceof LivingEntity)) {
            return;
        }

        // JC handles ordinary domains itself.
        // This only extends DUST compatibility to Abyss's open domain.
        if (!OpenDomainLifecycle.isActive(caster.getUUID())) {
            return;
        }

        if (caster.getPersistentData().getBoolean("Failed")) {
            return;
        }

        var flameData = flame.getPersistentData();
        var casterData = caster.getPersistentData();

        double dust = casterData.getDouble("dust_amount");

        if (dust <= 0.0D) {
            return;
        }

        // Exactly JC's original DUST transfer calculation.
        flameData.putDouble(
                "cnt6",
                Math.max(flameData.getDouble("cnt6"), dust)
        );

        // JC consumes the stored DUST when Divine Flame absorbs it.
        casterData.putDouble("dust_amount", 0.0D);
    }
}
