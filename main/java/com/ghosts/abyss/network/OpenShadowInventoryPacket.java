package com.ghosts.abyss.network;

import com.ghosts.abyss.menu.InnerselfInventoryMenu;
import com.ghosts.abyss.menu.ShadowInventoryMenu;
import com.ghosts.abyss.util.InnerselfHelper;
import com.ghosts.abyss.util.TenShadowsHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class OpenShadowInventoryPacket {

    public OpenShadowInventoryPacket() {}
    public OpenShadowInventoryPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Technique 6 → Shadow Inventory
            if (TenShadowsHelper.isTenShadows(player)) {
                int slots = TenShadowsHelper.getShadowSlotCount(player);
                NetworkHooks.openScreen(player,
                        new SimpleMenuProvider(
                                (id, inv, p) -> new ShadowInventoryMenu(id, inv, slots),
                                Component.literal("Shadow Inventory")
                        ),
                        buf -> buf.writeInt(slots)
                );
                return;
            }

            // Technique 15 → Innerself (all 9 slots)
            if (InnerselfHelper.isInnerselfUser(player)) {
                NetworkHooks.openScreen(player,
                        new SimpleMenuProvider(
                                (id, inv, p) -> new InnerselfInventoryMenu(id, inv),
                                Component.literal("Innerself")
                        )
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}