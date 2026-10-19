package com.ghosts.abyss.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.ghosts.abyss.util.InventoryRestrictionHelper;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private int screenWidth;
    @Shadow private int screenHeight;

    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void jujutsuslop$customHotbar(float partialTick, GuiGraphics guiGraphics, CallbackInfo ci) {
        Player player = this.minecraft.player;
        if (player == null) return;

        int allowed = InventoryRestrictionHelper.getAllowedHotbarSlots(player);
        if (allowed <= 0) {
            ci.cancel();
            return;
        }

        // Cancel vanilla rendering completely
        ci.cancel();

        // Vanilla hotbar slot width is 20 pixels
        int slotWidth = 20;
        int barWidth = allowed * slotWidth;
        int startX = this.screenWidth / 2 - barWidth / 2 + 1;
        int y = this.screenHeight - 22;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // === Background ===
        // Left end cap
        guiGraphics.blit(WIDGETS, startX - 1, y, 0, 0, 1, 22);

        // Middle sections (one per slot)
        for (int i = 0; i < allowed; i++) {
            guiGraphics.blit(WIDGETS, startX + i * slotWidth, y, 1, 0, 20, 22);
        }

        // Right end cap
        guiGraphics.blit(WIDGETS, startX + barWidth, y, 181, 0, 1, 22);

        // === Selection highlight ===
        int selected = player.getInventory().selected;
        if (selected >= 0 && selected < allowed) {
            // The selection box is 24×23 and is drawn 1 pixel up and 1 pixel left of the slot
            guiGraphics.blit(WIDGETS, startX + selected * slotWidth - 1, y - 1, 0, 22, 24, 23);
        }

        // === Items ===
        for (int i = 0; i < allowed; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                int itemX = startX + i * slotWidth + 3;
                int itemY = y + 3;

                guiGraphics.renderItem(stack, itemX, itemY, i + 1); // seed helps with bobbing
                guiGraphics.renderItemDecorations(this.minecraft.font, stack, itemX, itemY);
            }
        }

        RenderSystem.disableBlend();
    }
}