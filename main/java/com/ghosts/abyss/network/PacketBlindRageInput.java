package com.ghosts.abyss.network;

import com.ghosts.abyss.grab.BlindRageHandler;
import com.ghosts.abyss.grab.SoulGraspHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Existing grab-input packet; forwards hold/release to both abilities. */
public final class PacketBlindRageInput {
    private final boolean holding;

    public PacketBlindRageInput(boolean holding) {
        this.holding = holding;
    }

    public PacketBlindRageInput(FriendlyByteBuf buffer) {
        this.holding = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(holding);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Each handler checks its own selected technique. A release
                // resets both, even if the player changed skill mid-grab.
                BlindRageHandler.setHolding(player, holding);
                SoulGraspHandler.setHolding(player, holding);
            }
        });
        context.setPacketHandled(true);
    }
}
