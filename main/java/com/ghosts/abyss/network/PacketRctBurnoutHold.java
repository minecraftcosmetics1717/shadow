
package com.ghosts.abyss.network;

import com.ghosts.abyss.rct.RctBurnout;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketRctBurnoutHold {

    private final boolean holding;

    public PacketRctBurnoutHold(boolean holding) {
        this.holding = holding;
    }

    public static PacketRctBurnoutHold decode(FriendlyByteBuf buf) {
        return new PacketRctBurnoutHold(buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(holding);
    }

    public void handle(
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ServerPlayer player = ctx.getSender();

        if (player != null) {
            ctx.enqueueWork(() ->
                    RctBurnout.setHolding(player, holding)
            );
        }

        ctx.setPacketHandled(true);
    }
}
