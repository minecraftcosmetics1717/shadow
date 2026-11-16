package com.ghosts.abyss.network;

import com.ghosts.abyss.client.phone.PhoneMessageClient;
import com.ghosts.abyss.phone.PhoneMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public final class PhoneMessagePackets {
    private PhoneMessagePackets() {}

    public static class Request {
        public Request() {}
        public Request(FriendlyByteBuf buf) {}
        public void encode(FriendlyByteBuf buf) {}
        public void handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context ctx = supplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer p = ctx.getSender();
                if (p != null) PhoneMessages.sync(p);
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class Send {
        private final UUID target;
        private final String text;
        public Send(UUID target, String text) { this.target = target; this.text = text; }
        public Send(FriendlyByteBuf buf) { target = buf.readUUID(); text = buf.readUtf(PhoneMessages.MAX_TEXT); }
        public void encode(FriendlyByteBuf buf) { buf.writeUUID(target); buf.writeUtf(text, PhoneMessages.MAX_TEXT); }
        public void handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context ctx = supplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer p = ctx.getSender();
                if (p != null) PhoneMessages.send(p, target, text);
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class Sync {
        private final ListTag list;
        public Sync(ListTag list) { this.list = list; }
        public Sync(FriendlyByteBuf buf) {
            CompoundTag tag = buf.readNbt();
            list = tag == null ? new ListTag() : tag.getList("list", Tag.TAG_COMPOUND);
        }
        public void encode(FriendlyByteBuf buf) {
            CompoundTag root = new CompoundTag();
            root.put("list", list);
            buf.writeNbt(root);
        }
        public void handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context ctx = supplier.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                PhoneMessageClient.update(list);
            }));
            ctx.setPacketHandled(true);
        }
    }

    public static class Signal {
        private final boolean blocked;
        public Signal(boolean blocked) { this.blocked = blocked; }
        public Signal(FriendlyByteBuf buf) { blocked = buf.readBoolean(); }
        public void encode(FriendlyByteBuf buf) { buf.writeBoolean(blocked); }
        public void handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context ctx = supplier.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                PhoneMessageClient.setBlocked(blocked);
            }));
            ctx.setPacketHandled(true);
        }
    }
}
