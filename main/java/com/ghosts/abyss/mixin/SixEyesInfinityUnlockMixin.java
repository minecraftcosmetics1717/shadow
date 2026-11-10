
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.progression.GojoProgression;
import net.mcreator.jujutsucraft.procedures.SixEyesEffectStartedappliedProcedure;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SixEyesEffectStartedappliedProcedure.class, remap = false)
public abstract class SixEyesInfinityUnlockMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void abyss$checkInfinityUnlock(
            Entity entity,
            CallbackInfo ci) {

        if (!(entity instanceof ServerPlayer player)) return;

        if (!GojoProgression.isGojo(player)) return;

        // Normal behavior is allowed after RCT is unlocked.
        if (GojoProgression.hasRCT(player)) return;

        // Preserve the original Six Eyes advancement unlock.
        Advancement advancement = player.server
                .getAdvancements()
                .getAdvancement(new ResourceLocation(
                        "jujutsucraft:advancement_six_eyes"
                ));

        if (advancement != null) {
            AdvancementProgress progress = player
                    .getAdvancements()
                    .getOrStartProgress(advancement);

            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements()
                            .award(advancement, criterion);
                }
            }
        }

        // Prevent Jujutsucraft from awarding Infinity yet.
        ci.cancel();
    }
}
