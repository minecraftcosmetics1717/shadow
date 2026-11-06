package com.ghosts.abyss.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncHakariGamble {

    public final boolean visible;
    public final boolean rolling;
    public final int remainingTicks;
    public final int first;
    public final int second;
    public final int third;
    public final int inputs;

    public PacketSyncHakariGamble(
            boolean visible,
            boolean rolling,
            int remainingTicks,
            int first,
            int second,
            int third,
            int inputs
    ) {
        this.visible = visible;
        this.rolling = rolling;
        this.remainingTicks = remainingTicks;
        this.first = first;
        this.second = second;
        this.third = third;
        this.inputs = inputs;
    }

    public PacketSyncHakariGamble(FriendlyByteBuf buf) {
        visible = buf.readBoolean();
        rolling = buf.readBoolean();
        remainingTicks = buf.readVarInt();
        first = buf.readVarInt();
        second = buf.readVarInt();
        third = buf.readVarInt();
        inputs = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(visible);
        buf.writeBoolean(rolling);
        buf.writeVarInt(remainingTicks);
        buf.writeVarInt(first);
        buf.writeVarInt(second);
        buf.writeVarInt(third);
        buf.writeVarInt(inputs);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () ->
                                com.ghosts.abyss.client.HakariGambleHud
                                        .update(this)
                )
        );

        ctx.get().setPacketHandled(true);
    }
}
