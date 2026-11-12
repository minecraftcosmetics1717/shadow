
package com.ghosts.abyss.network;

import com.ghosts.abyss.uro.UroAirSwimHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketUroAirSwim {

    private final boolean swimming;

    public PacketUroAirSwim(boolean swimming) {
        this.swimming = swimming;
    }

    public static PacketUroAirSwim decode(FriendlyByteBuf buf) {
        return new PacketUroAirSwim(buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(swimming);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ServerPlayer player = ctx.getSender();

        if (player != null) {
            ctx.enqueueWork(() ->
                    UroAirSwimHandler.setSwimming(
                            player,
                            this.swimming
                    )
            );
        }

        ctx.setPacketHandled(true);
    }
}
