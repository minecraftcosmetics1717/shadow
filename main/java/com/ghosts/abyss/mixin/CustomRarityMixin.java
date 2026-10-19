package com.ghosts.abyss.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class CustomRarityMixin {

    @Inject(method = "getRarity", at = @At("HEAD"), cancellable = true)
    private void setCustomEpicRarity(ItemStack stack, CallbackInfoReturnable<Rarity> cir) {
        ResourceLocation loc = ForgeRegistries.ITEMS.getKey((Item) (Object) this);
        if (loc != null && loc.getNamespace().equals("jujutsucraft")) {
            String path = loc.getPath();
            if (path.equals("hiten") ||
                    path.equals("split_soul_katana") ||
                    path.equals("supreme_martial_solution") ||
                    path.equals("inverted_spear_of_heaven")) {

                cir.setReturnValue(Rarity.EPIC);
            }
        }
    }
}