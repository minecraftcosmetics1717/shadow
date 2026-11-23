
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.technique.TechniqueCooldownManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.StartCursedTechniqueProcedure",
        remap = false
)
public class IndividualCooldownGateMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$checkIndividualCooldown(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        int skillId =
                TechniqueCooldownManager.getSelectedSkill(player);

        if (!TechniqueCooldownManager.isManaged(skillId)) {
            return;
        }

        if (TechniqueCooldownManager.isOnCooldown(
                player, skillId)) {
            // Block the technique without an action-bar message.
            ci.cancel();
        }
    }
}
