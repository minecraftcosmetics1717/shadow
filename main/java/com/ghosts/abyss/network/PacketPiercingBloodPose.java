
package com.ghosts.abyss.network;

import com.ghosts.abyss.client.firstperson.FirstPersonAnimationManager;
import com.ghosts.abyss.client.firstperson.FirstPersonAnimationType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketPiercingBloodPose {

    private final boolean active;

    public PacketPiercingBloodPose(boolean active) {
        this.active = active;
    }

    public PacketPiercingBloodPose(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
    }

    public void handle(
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () ->
                                FirstPersonAnimationManager.setAnimation(
                                        active
                                                ? FirstPersonAnimationType.PIERCING_BLOOD
                                                : FirstPersonAnimationType.NONE))
        );

        context.setPacketHandled(true);
    }
}
