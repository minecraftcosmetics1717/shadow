package com.ghosts.abyss.phone;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PhoneMessagePackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public final class PhoneMessages {
    public static final int MAX_TEXT = 180;
    private static final int MAX_STORED = 250;
    private static final String KEY = "abyss_phone_messages";
    private static final String RATE_KEY = "abyss_phone_last_send";

    private PhoneMessages() {}

    private static CompoundTag data(ServerPlayer player) {
        CompoundTag parent = player.getPersistentData();
        if (!parent.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
            parent.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return parent.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static ListTag stored(ServerPlayer player) {
        return data(player).getList(KEY, Tag.TAG_COMPOUND);
    }

    private static void append(ServerPlayer player, CompoundTag message) {
        CompoundTag persistent = data(player);
        ListTag messages = stored(player);
        messages.add(message);
        while (messages.size() > MAX_STORED) messages.remove(0);
        persistent.put(KEY, messages);
    }

    private static CompoundTag entry(String thread, String displayName,
                                     String body, boolean outgoing, boolean quest, long tick) {
        CompoundTag tag = new CompoundTag();
        tag.putString("thread", thread);
        tag.putString("name", displayName);
        tag.putString("body", body);
        tag.putBoolean("outgoing", outgoing);
        tag.putBoolean("quest", quest);
        tag.putLong("tick", tick);
        return tag;
    }

    public static void sync(ServerPlayer player) {
        PhoneSignal.sync(player);
        // Store messages during signal loss, but do not deliver them until reconnected.
        if (PhoneSignal.blocked(player)) return;
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PhoneMessagePackets.Sync(stored(player).copy()));
    }

    public static void send(ServerPlayer sender, UUID recipientId, String raw) {
        if (PhoneSignal.blocked(sender)) {
            sender.displayClientMessage(net.minecraft.network.chat.Component.literal("No phone connection."), true);
            return;
        }
        if (raw == null) return;
        String body = raw.strip();
        if (body.isEmpty() || body.length() > MAX_TEXT) return;
        ServerPlayer target = sender.server.getPlayerList().getPlayer(recipientId);
        if (target == null || target == sender || PhoneSignal.blocked(target)) {
            sender.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "That player is offline or has no connection."), true);
            return;
        }
        long now = sender.serverLevel().getGameTime();
        if (sender.getPersistentData().contains(RATE_KEY) &&
                now - sender.getPersistentData().getLong(RATE_KEY) < 10L) return;
        sender.getPersistentData().putLong(RATE_KEY, now);

        String senderName = sender.getGameProfile().getName();
        String targetName = target.getGameProfile().getName();
        append(sender, entry(target.getUUID().toString(), targetName, body, true, false, now));
        append(target, entry(sender.getUUID().toString(), senderName, body, false, false, now));
        sync(sender);
        sync(target);
    }

    // Quest integration: call from SERVER quest code. Read-only thread beginning with "quest:".
    public static void addQuestMessage(ServerPlayer player, String questId,
                                       String senderName, String body) {
        if (questId == null || questId.isBlank() || body == null || body.isBlank()) return;
        String trimmed = body.length() > MAX_TEXT ? body.substring(0, MAX_TEXT) : body;
        append(player, entry("quest:" + questId, senderName, trimmed, false, true,
                player.serverLevel().getGameTime()));
        if (!PhoneSignal.blocked(player)) sync(player);
        // Otherwise saved: delivered when inbox is next refreshed with signal.
    }
}
