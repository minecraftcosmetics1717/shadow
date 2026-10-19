package com.ghosts.abyss.capability;

import net.minecraft.nbt.CompoundTag;

public class PlayerBodyData implements IPlayerBodyData {
    private boolean hasPerfectBody = false;

    @Override
    public boolean hasPerfectBody() {
        return hasPerfectBody;
    }

    @Override
    public void setHasPerfectBody(boolean value) {
        this.hasPerfectBody = value;
    }

    public void saveNBT(CompoundTag tag) {
        tag.putBoolean("HasPerfectBody", hasPerfectBody);
    }

    public void loadNBT(CompoundTag tag) {
        hasPerfectBody = tag.getBoolean("HasPerfectBody");
    }
}