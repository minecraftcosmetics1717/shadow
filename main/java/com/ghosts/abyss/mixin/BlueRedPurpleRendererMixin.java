
package com.ghosts.abyss.mixin;

import net.mcreator.jujutsucraft.entity.BlueEntity;
import net.mcreator.jujutsucraft.entity.RedEntity;
import net.mcreator.jujutsucraft.entity.PurpleEntity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class BlueRedPurpleRendererMixin<T extends Entity> {

    @Inject(
            method = "shouldRender",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$alwaysRenderPurpleRedBlue(
            T entity,
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity instanceof BlueEntity
                || entity instanceof RedEntity
                || entity instanceof PurpleEntity) {

            cir.setReturnValue(true);
        }
    }
}
