
package com.ghosts.abyss.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncAscendantAdvancements {

    private final boolean grade1;
    private final boolean specialGrade;

    public PacketSyncAscendantAdvancements(
            boolean grade1, boolean specialGrade
    ) {
        this.grade1 = grade1;
        this.specialGrade = specialGrade;
    }

    public PacketSyncAscendantAdvancements(FriendlyByteBuf buf) {
        this.grade1 = buf.readBoolean();
        this.specialGrade = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(grade1);
        buf.writeBoolean(specialGrade);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> this::handleClient
                )
        );

        ctx.setPacketHandled(true);
    }

    private void handleClient() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;

        mc.player.getPersistentData().putBoolean(
                "abyss_has_grade_1", grade1
        );

        mc.player.getPersistentData().putBoolean(
                "abyss_has_special_grade", specialGrade
        );
    }
}
