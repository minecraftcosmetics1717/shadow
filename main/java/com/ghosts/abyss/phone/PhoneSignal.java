package com.ghosts.abyss.phone;

import com.ghosts.abyss.domain.DomainDimension;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PhoneMessagePackets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abyss")
public final class PhoneSignal {
    public static final String BLOCKED_KEY = "abyss_phone_blocked";
    private static final Map<UUID, Boolean> LAST = new HashMap<>();

    private PhoneSignal() {}

    // This is the authoritative check. Add future jammer conditions HERE.
    public static boolean blocked(ServerPlayer player) {
        return player.level().dimension().equals(DomainDimension.ID)
                || player.getPersistentData().getBoolean(BLOCKED_KEY)
                || player.getPersistentData().getBoolean("jreborn_teleport_disabled");
    }

    public static void sync(ServerPlayer player) {
        boolean blocked = blocked(player);
        LAST.put(player.getUUID(), blocked);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PhoneMessagePackets.Signal(blocked));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 10 != 0) return;
        boolean now = blocked(player);
        if (!LAST.containsKey(player.getUUID()) || LAST.get(player.getUUID()) != now) {
            sync(player);
// Synchronize all stored messages when the player's
// phone connection is restored.
            if (!now) {
                PhoneMessages.sync(player);
            }
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }
}
