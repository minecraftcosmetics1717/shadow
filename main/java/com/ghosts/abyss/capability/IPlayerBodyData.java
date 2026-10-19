package com.ghosts.abyss.capability;

public interface IPlayerBodyData {
    boolean hasPerfectBody();
    void setHasPerfectBody(boolean value);

    default int getAllowedHotbarSlots() {
        return hasPerfectBody() ? 5 : 3;
    }
}