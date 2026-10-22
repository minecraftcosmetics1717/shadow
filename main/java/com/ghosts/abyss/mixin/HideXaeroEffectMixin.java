package com.ghosts.abyss.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.stream.Collectors;

@Mixin(LivingEntity.class)
public class HideXaeroEffectMixin {

    private static final ResourceLocation NO_MINIMAP =
            new ResourceLocation("xaerominimap", "no_minimap");

    @Inject(method = "getActiveEffects", at = @At("RETURN"), cancellable = true)
    private void abyss$hideNoMinimapInInventory(CallbackInfoReturnable<Collection<MobEffectInstance>> cir) {
        // Only the client player
        if (!((Object) this instanceof LocalPlayer)) return;

        // Only while inventory / creative inventory is open
        var screen = Minecraft.getInstance().screen;
        if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) {
            return;
        }

        Collection<MobEffectInstance> effects = cir.getReturnValue();
        if (effects == null || effects.isEmpty()) return;

        Collection<MobEffectInstance> filtered = effects.stream()
                .filter(e -> {
                    ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(e.getEffect());
                    return id == null || !id.equals(NO_MINIMAP);
                })
                .collect(Collectors.toList());

        if (filtered.size() != effects.size()) {
            cir.setReturnValue(filtered);
        }
    }
}