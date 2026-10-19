package com.ghosts.abyss.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class CharmGlintMixin {

    @Inject(method = "isFoil", at = @At("HEAD"), cancellable = true)
    private void jreborn$forceCharmGlint(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return;

        // The three charms you listed
        if (id.equals(new ResourceLocation("jujutsucraftv", "vitality_charm_6"))
                || id.equals(new ResourceLocation("jujutsucraftv", "protective_charm_6"))
                || id.equals(new ResourceLocation("jujutsucraftv", "might_charm_6"))) {
            cir.setReturnValue(true); // same as enchanted golden apple
        }
    }
}