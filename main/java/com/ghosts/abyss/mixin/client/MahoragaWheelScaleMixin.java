
package com.ghosts.abyss.mixin.client;

import net.mcreator.jujutsucraft.item.MahoragaWheelItem;
import net.mcreator.jujutsucraft.client.model.Modelmahoraga;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(value = MahoragaWheelItem.Helmet.class, remap = false)
public abstract class MahoragaWheelScaleMixin {

    @Inject(
            method = "initializeClient",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void abyss$customWheelModel(
            Consumer<IClientItemExtensions> consumer,
            CallbackInfo ci
    ) {
        consumer.accept(new IClientItemExtensions() {

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity living,
                    ItemStack stack,
                    EquipmentSlot slot,
                    HumanoidModel<?> defaultModel
            ) {
                Modelmahoraga<?> mahoraga =
                        new Modelmahoraga<>(
                                Minecraft.getInstance()
                                        .getEntityModels()
                                        .bakeLayer(
                                                Modelmahoraga.LAYER_LOCATION
                                        )
                        );

                ModelPart head = mahoraga.head;
                ModelPart wheel = head.getChild("wheel");

                // Enlarge only Mahoraga's wheel.
                wheel.xScale = 1.5F;
                wheel.yScale = 1.5F;
                wheel.zScale = 1.5F;

                ModelPart empty = new ModelPart(
                        Collections.emptyList(),
                        Collections.emptyMap()
                );

                HumanoidModel<?> armorModel =
                        new HumanoidModel<>(
                                new ModelPart(
                                        Collections.emptyList(),
                                        Map.of(
                                                "head", head,
                                                "hat", empty,
                                                "body", empty,
                                                "right_arm", empty,
                                                "left_arm", empty,
                                                "right_leg", empty,
                                                "left_leg", empty
                                        )
                                )
                        );

                armorModel.crouching = living.isCrouching();
                armorModel.riding = defaultModel.riding;
                armorModel.young = living.isBaby();

                return armorModel;
            }
        });

        // Skip JC's original client model setup.
        ci.cancel();
    }
}
