
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.grade.AbyssGradeManager;

import net.mcreator.jujutsucraft.procedures.CompletedAdvancementProcedure;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AdvancementEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CompletedAdvancementProcedure.class, remap = false)
public abstract class AbyssGradeAdvancementMixin {

    @Inject(
            method = "onAdvancement",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$preventGradeAdvancement(
            AdvancementEvent event,
            CallbackInfo ci
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!AbyssGradeManager.hasGrade(player)) {
            return;
        }

        Advancement advancement = event.getAdvancement();
        if (advancement == null) {
            return;
        }

        ResourceLocation id = advancement.getId();

        if (!"jujutsucraft".equals(id.getNamespace())) {
            return;
        }

        if (id.getPath().startsWith("sorcerer_grade_")) {
            ci.cancel();
        }
    }
}
