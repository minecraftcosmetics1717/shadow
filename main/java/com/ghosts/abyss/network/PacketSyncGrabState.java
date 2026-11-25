package com.ghosts.abyss.network;

import com.ghosts.abyss.client.GrabHudState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> grabber: whether the server has an actual victim attached. */
public final class PacketSyncGrabState {
    private final boolean attached;

    public PacketSyncGrabState(boolean attached) {
        this.attached = attached;
    }

    public PacketSyncGrabState(FriendlyByteBuf buf) {
        this.attached = buf.readBoolean();
    }

    public static void encode(PacketSyncGrabState message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.attached);
    }

    public static void handle(PacketSyncGrabState message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> ClientHandler.apply(message)));
        context.setPacketHandled(true);
    }

    /** Don't load a Minecraft client class on a dedicated server. */
    private static final class ClientHandler {
        static void apply(PacketSyncGrabState message) {
            GrabHudState.setAttached(message.attached);
        }
    }
}
