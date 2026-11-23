
package com.ghosts.abyss.network;

import com.ghosts.abyss.technique.TechniqueCooldownManager;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PacketSyncTechniqueCooldowns {

    private final Map<Integer, Integer> remainingBySkill;

    public PacketSyncTechniqueCooldowns(
            Map<Integer, Integer> remainingBySkill
    ) {
        this.remainingBySkill = new HashMap<>(remainingBySkill);
    }

    public PacketSyncTechniqueCooldowns(FriendlyByteBuf buf) {
        this.remainingBySkill = new HashMap<>();

        int count = buf.readVarInt();

        if (count < 0 || count > 64) {
            throw new IllegalArgumentException(
                    "Invalid cooldown packet size: " + count
            );
        }

        for (int i = 0; i < count; i++) {
            int skillId = buf.readVarInt();
            int remaining = buf.readVarInt();

            remainingBySkill.put(
                    skillId,
                    Math.max(0, remaining)
            );
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(remainingBySkill.size());

        for (Map.Entry<Integer, Integer> entry :
                remainingBySkill.entrySet()) {

            buf.writeVarInt(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    public void handle(
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context ctx = contextSupplier.get();

        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> this::handleClient
                )
        );

        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        var player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        for (int skillId :
                TechniqueCooldownManager.getManagedSkillIds()) {

            int remaining = Math.max(
                    0,
                    remainingBySkill.getOrDefault(skillId, 0)
            );

            player.getPersistentData().putInt(
                    "abyss_ui_cd_" + skillId,
                    remaining
            );
        }
    }
}
