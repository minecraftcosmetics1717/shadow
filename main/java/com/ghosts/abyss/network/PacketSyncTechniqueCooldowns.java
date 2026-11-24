package com.ghosts.abyss.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Sends individual cooldowns and the server-resolved Yuta copied selection. */
public class PacketSyncTechniqueCooldowns {

    private static final String YUTA_INDEX_KEY = "abyss_ui_yuta_copy_index";
    private static final String YUTA_SKILL_KEY = "abyss_ui_yuta_original_skill";
    private static final String RECEIVE_TIME_KEY = "abyss_ui_cd_packet_time";
    private static final long DEFAULT_INTERVAL_NANOS = 250_000_000L;

    private final Map<Integer, Integer> remainingBySkill;
    private final int selectedYutaCopyIndex;
    private final int selectedYutaOriginalSkill;

    public PacketSyncTechniqueCooldowns(Map<Integer, Integer> remainingBySkill,
                                        int selectedYutaCopyIndex,
                                        int selectedYutaOriginalSkill) {
        this.remainingBySkill = new HashMap<>(remainingBySkill);
        this.selectedYutaCopyIndex = selectedYutaCopyIndex;
        this.selectedYutaOriginalSkill = selectedYutaOriginalSkill;
    }

    public PacketSyncTechniqueCooldowns(FriendlyByteBuf buf) {
        this.remainingBySkill = new HashMap<>();
        int count = buf.readVarInt();
        if (count < 0 || count > 256) {
            throw new IllegalArgumentException("Invalid cooldown packet size: " + count);
        }
        for (int i = 0; i < count; i++) {
            int skillId = buf.readVarInt();
            int remaining = buf.readVarInt();
            remainingBySkill.put(skillId, Math.max(0, remaining));
        }
        selectedYutaCopyIndex = buf.readVarInt();
        selectedYutaOriginalSkill = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(remainingBySkill.size());
        for (Map.Entry<Integer, Integer> entry : remainingBySkill.entrySet()) {
            buf.writeVarInt(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
        buf.writeVarInt(selectedYutaCopyIndex);
        buf.writeVarInt(selectedYutaOriginalSkill);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> this::handleClient));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        CompoundTag data = player.getPersistentData();
        long now = System.nanoTime();
        long lastPacket = data.getLong(RECEIVE_TIME_KEY);
        long interval = lastPacket == 0L
                ? DEFAULT_INTERVAL_NANOS
                : Math.max(100_000_000L, Math.min(500_000_000L, now - lastPacket));
        data.putLong(RECEIVE_TIME_KEY, now);

        // Retain one previous sample for each skill. Rendering animates
        // its number toward the new authoritative value over the sync interval.
        for (Map.Entry<Integer, Integer> entry : remainingBySkill.entrySet()) {
            int id = entry.getKey();
            int next = Math.max(0, entry.getValue());
            String key = Integer.toString(id);
            String valueKey = "abyss_ui_cd_" + key;
            String fromKey = "abyss_ui_cd_from_" + key;
            String startKey = "abyss_ui_cd_start_" + key;
            String intervalKey = "abyss_ui_cd_interval_" + key;

            double oldDisplayed = next;
            if (data.contains(valueKey)) {
                int oldValue = Math.max(0, data.getInt(valueKey));
                int oldFrom = data.contains(fromKey)
                        ? Math.max(0, data.getInt(fromKey)) : oldValue;
                long oldStart = data.getLong(startKey);
                long oldInterval = Math.max(1L, data.getLong(intervalKey));
                double progress = oldStart == 0L ? 1.0D
                        : Math.max(0.0D, Math.min(1.0D,
                        (now - oldStart) / (double) oldInterval));
                oldDisplayed = oldFrom + (oldValue - oldFrom) * progress;
            }

            // New cooldowns and extensions must appear immediately;
            // only decreasing numbers are interpolated.
            int from = (int) Math.ceil(Math.max(0.0D, oldDisplayed));
            if (next >= from) from = next;
            data.putInt(fromKey, from);
            data.putInt(valueKey, next);
            data.putLong(startKey, now);
            data.putLong(intervalKey, interval);
        }

        // The server knows precisely which original skill the selected
        // copied slot represents. This avoids relying on a stale client cache.
        data.putInt(YUTA_INDEX_KEY, selectedYutaCopyIndex);
        data.putInt(YUTA_SKILL_KEY, selectedYutaOriginalSkill);
    }
}
