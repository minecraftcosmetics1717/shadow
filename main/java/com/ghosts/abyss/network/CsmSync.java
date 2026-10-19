package com.ghosts.abyss.network;

import com.ghosts.abyss.csm.CsmRoster;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

public final class CsmSync {
   private CsmSync() {
   }

   public static void sendRoster(ServerPlayer player) {
      List<String[]> deployed = new ArrayList<>();
      Level level = player.level();
      if (level instanceof ServerLevel sl) {
         // Reduced inflate radius from 512.0F to 64.0F to reduce chunk scan overhead
         AABB box = player.getBoundingBox().inflate(128.0D);
         sl.getEntitiesOfClass(Entity.class, box, (e) ->
                 e.isAlive()
                         && e.getPersistentData().getBoolean("CursedSpirit")
                         && player.getStringUUID().equals(e.getPersistentData().getString("OWNER_UUID"))
         ).forEach((e) -> {
            String name = e.getPersistentData().getString("CSM_DISPLAY_NAME");
            if (name.isEmpty()) name = e.getDisplayName().getString();

            ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
            String type = (rl != null) ? rl.toString() : "";
            deployed.add(new String[]{e.getStringUUID(), name, type});
         });
      }

      PacketSyncCsmRoster packet = new PacketSyncCsmRoster(
              CsmRoster.getSpirits(player),
              CsmRoster.getSelectedUuid(player),
              0,
              CsmRoster.getSelectedRosterIdx(player),
              deployed
      );
      ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
   }}