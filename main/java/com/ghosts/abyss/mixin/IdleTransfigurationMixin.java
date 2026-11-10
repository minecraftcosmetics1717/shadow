
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.mahito.AbyssIdleTransfiguration;

import net.mcreator.jujutsucraft.procedures.IdleTransfigurationProcedure;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
        value = IdleTransfigurationProcedure.class,
        remap = false
)
public class IdleTransfigurationMixin {

    @Redirect(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/mcreator/jujutsucraft/procedures/RangeAttackProcedure;" +
                                    "execute(Lnet/minecraft/world/level/LevelAccessor;" +
                                    "DDDLnet/minecraft/world/entity/Entity;)V",
                    remap = false
            ),
            remap = false
    )
    private static void abyss$replaceIdleTransfiguration(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity attacker
    ) {
        AbyssIdleTransfiguration.execute(
                world, x, y, z, attacker
        );
    }
}
