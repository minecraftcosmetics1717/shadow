package com.ghosts.abyss.network;

import com.ghosts.abyss.grab.BlindRageHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Only transmits whether the attack button is held. Never trusts client targets. */
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
            if (player != null) BlindRageHandler.setHolding(player, holding);
        });
        context.setPacketHandled(true);
    }
}
