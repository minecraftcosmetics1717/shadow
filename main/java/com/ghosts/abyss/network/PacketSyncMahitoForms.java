package com.ghosts.abyss.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Tiny S2C sync so client animations and skill names see active toggles. */
public final class PacketSyncMahitoForms {
    private final boolean blades, small, trueForm, unlocked, multiplicity, polymorphic, domain;

    public PacketSyncMahitoForms(boolean blades, boolean small, boolean trueForm,
                                 boolean unlocked, boolean multiplicity,
                                 boolean polymorphic, boolean domain) {
        this.blades = blades;
        this.small = small;
        this.trueForm = trueForm;
        this.unlocked = unlocked;
        this.multiplicity = multiplicity;
        this.polymorphic = polymorphic;
        this.domain = domain;
    }

    public PacketSyncMahitoForms(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void encode(PacketSyncMahitoForms packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.blades);
        buf.writeBoolean(packet.small);
        buf.writeBoolean(packet.trueForm);
        buf.writeBoolean(packet.unlocked);
        buf.writeBoolean(packet.multiplicity);
        buf.writeBoolean(packet.polymorphic);
        buf.writeBoolean(packet.domain);
    }

    public static void handle(PacketSyncMahitoForms packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientApply.run(packet)));
        context.setPacketHandled(true);
    }

    private static final class ClientApply {
        static void run(PacketSyncMahitoForms packet) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            var data = mc.player.getPersistentData();
            data.putBoolean("abyss_mahito_idle_blades", packet.blades);
            data.putBoolean("abyss_mahito_idle_diminution", packet.small);
            data.putBoolean("abyss_mahito_true_form", packet.trueForm);
            data.putBoolean("abyss_mahito_unlock_true_form", packet.unlocked);
            data.putBoolean("abyss_mahito_unlock_multiplicity", packet.multiplicity);
            data.putBoolean("abyss_mahito_unlock_polymorphic", packet.polymorphic);
            data.putBoolean("abyss_mahito_unlock_domain", packet.domain);
        }
    }
}
