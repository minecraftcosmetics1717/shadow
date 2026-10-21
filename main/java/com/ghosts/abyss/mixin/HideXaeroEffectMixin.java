package com.ghosts.abyss.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Collection;
import java.util.stream.Collectors;

@Mixin(AbstractContainerScreen.class)
public class HideXaeroEffectMixin {

    private static final ResourceLocation NO_MINIMAP =
            new ResourceLocation("xaerominimap", "no_minimap");

    @ModifyVariable(
            method = "renderEffects",
            at = @At("STORE"),
            ordinal = 0
    )
    private Collection<MobEffectInstance> abyss$filterXaeroEffect(Collection<MobEffectInstance> effects) {
        return effects.stream()
                .filter(e -> {
                    ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(e.getEffect());
                    return id == null || !id.equals(NO_MINIMAP);
                })
                .collect(Collectors.toList());
    }
}