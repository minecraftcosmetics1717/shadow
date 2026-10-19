package com.ghosts.abyss.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LifeCharmsCapability implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final int SIZE = 27; // normal single chest

    private final ItemStackHandler inventory = new ItemStackHandler(SIZE) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(Items.TOTEM_OF_UNDYING);
        }
    };

    private final LazyOptional<LifeCharmsCapability> optional = LazyOptional.of(() -> this);

    public ItemStackHandler getInventory() {
        return inventory;
    }

    /** Returns true and consumes one totem if available */
    public boolean tryConsumeTotem() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                stack.shrink(1);
                inventory.setStackInSlot(i, stack);
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ModCapabilities.LIFE_CHARMS.orEmpty(cap, optional.cast());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("Items", inventory.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("Items")) {
            inventory.deserializeNBT(nbt.getCompound("Items"));
        }
    }
}