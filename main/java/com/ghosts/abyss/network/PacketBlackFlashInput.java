
package com.ghosts.abyss.network;

import com.ghosts.abyss.BlackFlashTiming;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketBlackFlashInput {

    public static PacketBlackFlashInput decode(FriendlyByteBuf buf) {
        return new PacketBlackFlashInput();
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();

        ctx.enqueueWork(() -> {
            if (player != null) {
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§a[BF DEBUG] E key received by server!"
                        )
                );

                BlackFlashTiming.press(player);
            }
        });

        ctx.setPacketHandled(true);
    }
}
