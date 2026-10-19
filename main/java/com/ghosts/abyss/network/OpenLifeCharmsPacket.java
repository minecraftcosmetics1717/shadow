package com.ghosts.abyss.network;

import com.ghosts.abyss.menu.LifeCharmsMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class OpenLifeCharmsPacket {

    public OpenLifeCharmsPacket() {}
    public OpenLifeCharmsPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            NetworkHooks.openScreen(player,
                    new SimpleMenuProvider(
                            (id, inv, p) -> new LifeCharmsMenu(id, inv),
                            Component.literal("Life Charms")
                    )
            );
        });
        ctx.get().setPacketHandled(true);
    }
}