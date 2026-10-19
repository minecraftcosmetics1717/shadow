package com.ghosts.abyss.network;

import com.ghosts.abyss.AbyssGamerules;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class PacketSyncGamerules {
   private final boolean domainRefinement;
   private final boolean hollowWickerBasket;
   private final boolean perfectBody;
   private final boolean cursedObjectTransform;
   private final boolean openDomain;
   private final boolean blackFlash;
   private final boolean reverseCursedTechnique;
   private final boolean shrineVisual;

   public PacketSyncGamerules(boolean domainRefinement, boolean hollowWickerBasket, boolean perfectBody, boolean cursedObjectTransform, boolean openDomain, boolean blackFlash, boolean reverseCursedTechnique, boolean shrineVisual) {
      this.domainRefinement = domainRefinement;
      this.hollowWickerBasket = hollowWickerBasket;
      this.perfectBody = perfectBody;
      this.cursedObjectTransform = cursedObjectTransform;
      this.openDomain = openDomain;
      this.blackFlash = blackFlash;
      this.reverseCursedTechnique = reverseCursedTechnique;
      this.shrineVisual = shrineVisual;
   }

   public static PacketSyncGamerules decode(FriendlyByteBuf buf) {
      return new PacketSyncGamerules(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.domainRefinement);
      buf.writeBoolean(this.hollowWickerBasket);
      buf.writeBoolean(this.perfectBody);
      buf.writeBoolean(this.cursedObjectTransform);
      buf.writeBoolean(this.openDomain);
      buf.writeBoolean(this.blackFlash);
      buf.writeBoolean(this.reverseCursedTechnique);
      buf.writeBoolean(this.shrineVisual);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> AbyssGamerules.setClientData(this.domainRefinement, this.hollowWickerBasket, this.perfectBody, this.cursedObjectTransform, this.openDomain, this.blackFlash, this.reverseCursedTechnique, this.shrineVisual));
      ctx.setPacketHandled(true);
   }

   public static void sync(ServerPlayer player) {
      GameRules gr = player.getServer().getGameRules();
      ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketSyncGamerules(gr.getBoolean(AbyssGamerules.DOMAIN_REFINEMENT), gr.getBoolean(AbyssGamerules.HOLLOW_WICKER_BASKET), gr.getBoolean(AbyssGamerules.PERFECT_BODY), gr.getBoolean(AbyssGamerules.CURSED_OBJECT_TRANSFORMATION), gr.getBoolean(AbyssGamerules.OPEN_DOMAIN), gr.getBoolean(AbyssGamerules.BLACK_FLASH), gr.getBoolean(AbyssGamerules.REVERSE_CURSED_TECHNIQUE), gr.getBoolean(AbyssGamerules.SHRINE_VISUAL)));
   }
}