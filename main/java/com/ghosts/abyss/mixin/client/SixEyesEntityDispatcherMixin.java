
package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.client.sixeyes.SixEyesMaskCapture;
import com.ghosts.abyss.client.sixeyes.SixEyesVisionHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(EntityRenderDispatcher.class)
public abstract class SixEyesEntityDispatcherMixin {

    private static final Set<ResourceLocation> LOGGED_TYPES =
            new HashSet<>();

    @Inject(method = "render", at = @At("HEAD"))
    private <E extends Entity> void abyss$onRender(
            E entity,
            double x,
            double y,
            double z,
            float rotationYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null
                || !SixEyesVisionHandler.shouldRender(mc.player)) {
            LOGGED_TYPES.clear();
            return;
        }

        if (!(entity instanceof LivingEntity) || entity == mc.player) {
            return;
        }
        SixEyesMaskCapture.capture(entity);
        ResourceLocation id =
                ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        if (id == null) return;

        if ("jujutsucraft".equals(id.getNamespace())) {
            String path = id.getPath();

            if (path.equals("entity_attack_slash")
                    || path.equals("entity_attack_strike")
                    || path.equals("entity_attack_sweep")) {
                return;
            }
        }

        }
    }
