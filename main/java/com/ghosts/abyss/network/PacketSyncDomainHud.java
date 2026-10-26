package com.ghosts.abyss.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketSyncDomainHud {
    public final boolean inDomainSession;
    public final boolean yourDomainActive;
    public final boolean clashing;
    public final String ownerName;
    public final List<String> clasherNames;

    public PacketSyncDomainHud(boolean inDomainSession, boolean yourDomainActive,
                               boolean clashing, String ownerName, List<String> clasherNames) {
        this.inDomainSession = inDomainSession;
        this.yourDomainActive = yourDomainActive;
        this.clashing = clashing;
        this.ownerName = ownerName == null ? "" : ownerName;
        this.clasherNames = clasherNames == null ? List.of() : clasherNames;
    }

    public PacketSyncDomainHud(FriendlyByteBuf buf) {
        this.inDomainSession = buf.readBoolean();
        this.yourDomainActive = buf.readBoolean();
        this.clashing = buf.readBoolean();
        this.ownerName = buf.readUtf(64);
        int n = buf.readVarInt();
        List<String> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(buf.readUtf(64));
        this.clasherNames = list;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(inDomainSession);
        buf.writeBoolean(yourDomainActive);
        buf.writeBoolean(clashing);
        buf.writeUtf(ownerName, 64);
        buf.writeVarInt(clasherNames.size());
        for (String s : clasherNames) buf.writeUtf(s, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> com.ghosts.abyss.client.DomainHudOverlay.update(this))
        );
        ctx.get().setPacketHandled(true);
    }
}