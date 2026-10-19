package com.ghosts.abyss.network;

import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.Abyss;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;

public class PacketPactResponse {
   private final boolean confirmed;

   public PacketPactResponse(boolean confirmed) {
      this.confirmed = confirmed;
   }

   public PacketPactResponse(FriendlyByteBuf buf) {
      this.confirmed = buf.readBoolean();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.confirmed);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player == null) {
            return;
         }

         CompoundTag persistentData = player.getPersistentData();

         if (persistentData.hasUUID("pact_target_uuid")) {
            UUID mahitoUuid = persistentData.getUUID("pact_target_uuid");
            persistentData.remove("pact_target_uuid");

            if (player.level() instanceof ServerLevel serverLevel) {
               // Find Mahito within 120 blocks of the player
               List<Entity> found = serverLevel.getEntities(
                       (Entity) null,
                       player.getBoundingBox().inflate(120.0D),
                       (e) -> e instanceof Mob && e.getUUID().equals(mahitoUuid)
               );

               Mob mahito = found.isEmpty() ? null : (Mob) found.get(0);

               if (!this.confirmed) {
                  // Pact rejected
                  if (mahito != null) {
                     mahito.setNoAi(false);
                     mahito.setTarget(player);
                  }

                  Abyss.LOGGER.debug("{} cancelled the pact.", player.getScoreboardName());

               } else if (!AbyssGamerules.isEnabled(player, AbyssGamerules.PERFECT_BODY)) {
                  // Gamerule check failed
                  player.sendSystemMessage(Component.literal("§cPerfect Body has been disabled by a server rule"));

                  if (mahito != null) {
                     mahito.setNoAi(false);
                     mahito.setTarget(player);
                  }

               } else {
                  // Pact accepted
                  if (mahito != null) {
                     mahito.setNoAi(true);
                     mahito.setPersistenceRequired(); // Or mahito.setInvulnerable(true);
                     mahito.setTarget(null);
                  }

                  persistentData.putBoolean("perfect_body", true);
                  player.getMainHandItem().shrink(1);

                  Abyss.LOGGER.debug("{} completed the Perfect Body pact", player.getScoreboardName());
               }
            }
         }
      });

      ctx.setPacketHandled(true);
   }
}