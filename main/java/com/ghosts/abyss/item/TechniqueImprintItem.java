package com.ghosts.abyss.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class TechniqueImprintItem extends Item {
    private final int techniqueId;

    public TechniqueImprintItem(int techniqueId) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
        this.techniqueId = techniqueId;
    }

    public int getTechniqueId() {
        return techniqueId;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
                                List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable("tooltip.abyss.technique_imprint")
                .withStyle(ChatFormatting.GRAY));
    }
}
