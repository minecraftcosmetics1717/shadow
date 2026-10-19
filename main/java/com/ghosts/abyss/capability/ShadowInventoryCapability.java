package com.ghosts.abyss.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShadowInventoryCapability implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final int MAX_SLOTS = 9;

    private final ItemStackHandler inventory = new ItemStackHandler(MAX_SLOTS);

    private final LazyOptional<ShadowInventoryCapability> optional = LazyOptional.of(() -> this);

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ModCapabilities.SHADOW_INVENTORY.orEmpty(cap, optional.cast());
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