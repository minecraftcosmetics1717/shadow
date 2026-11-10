
package com.ghosts.abyss.csm;

import com.ghosts.abyss.network.CsmSync;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

public final class CsmSpiritEvents {

    private CsmSpiritEvents() {}

    public static void onCsmSpiritRightClickRecall(
            PlayerInteractEvent.EntityInteract event) {

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player
                && isCsmPlayer(player)) {

            Entity target = event.getTarget();

            if (target != null && target.isAlive()
                    && !(target instanceof Player)) {

                if (target.getPersistentData()
                        .getBoolean("CursedSpirit")) {

                    String spiritMarker = target.getPersistentData()
                            .getString("CSM_SPIRIT_UUID");

                    if (!spiritMarker.isEmpty()
                            && target.getPersistentData()
                            .getString("OWNER_UUID")
                            .equals(player.getStringUUID())) {

                        event.setCanceled(true);

                        String resolvedUuid =
                                spiritMarker.startsWith("DEPLOYED:")
                                        ? spiritMarker.substring(9)
                                        : spiritMarker;

                        String csmRosterType = target
                                .getPersistentData()
                                .getString("CSM_ROSTER_TYPE");

                        ResourceLocation rl =
                                ForgeRegistries.ENTITY_TYPES
                                        .getKey(target.getType());

                        String entityType =
                                !csmRosterType.isEmpty()
                                        ? csmRosterType
                                        : (rl != null
                                        ? rl.toString()
                                        : "unknown");

                        String csmDisplayName = target
                                .getPersistentData()
                                .getString("CSM_DISPLAY_NAME");

                        String displayName =
                                !csmDisplayName.isEmpty()
                                        ? csmDisplayName
                                        : target.getDisplayName()
                                        .getString();

                        CsmRoster.restoreSpirit(
                                player,
                                entityType,
                                resolvedUuid,
                                displayName
                        );

                        target.remove(Entity.RemovalReason.DISCARDED);
                        CsmSync.sendRoster(player);
                    }
                }
            }
        }
    }

    private static boolean isCsmPlayer(ServerPlayer player) {
        try {
            Class<?> varsClass = Class.forName(
                    "net.mcreator.jujutsucraft.network."
                            + "JujutsucraftModVariables"
            );

            Field capField = varsClass.getDeclaredField(
                    "PLAYER_VARIABLES_CAPABILITY"
            );

            capField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Capability<Object> cap =
                    (Capability<Object>) capField.get(null);

            Object vars = player.getCapability(cap, null)
                    .orElse(null);

            if (vars == null) return false;

            Field ct1 = vars.getClass()
                    .getDeclaredField("PlayerCurseTechnique");

            Field ct2 = vars.getClass()
                    .getDeclaredField("PlayerCurseTechnique2");

            ct1.setAccessible(true);
            ct2.setAccessible(true);

            return (Double) ct1.get(vars) == 18.0D
                    || (Double) ct2.get(vars) == 18.0D;

        } catch (Exception e) {
            return false;
        }
    }

    public static void onSpiritDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();

        if (entity.level().isClientSide()) return;

        CompoundTag tag = entity.getPersistentData();

        if (tag.getBoolean("CursedSpirit")) {
            String ownerUuidStr = tag.getString("OWNER_UUID");

            if (!ownerUuidStr.isEmpty()) {
                try {
                    UUID ownerUuid =
                            UUID.fromString(ownerUuidStr);

                    ServerPlayer player = entity.getServer()
                            .getPlayerList()
                            .getPlayer(ownerUuid);

                    if (player != null) {
                        String spiritUuid =
                                tag.getString("CSM_SPIRIT_UUID");

                        ResourceLocation key =
                                ForgeRegistries.ENTITY_TYPES
                                        .getKey(entity.getType());

                        if (key == null) return;

                        String entityType = key.toString();

                        CsmRoster.removeDeployedSpirit(
                                player, entityType, spiritUuid
                        );

                        CsmSync.sendRoster(player);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public static void onSpiritUnload(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        Entity entity = event.getEntity();

        if (entity.isRemoved()) return;

        if (entity.getRemovalReason()
                == Entity.RemovalReason.UNLOADED_TO_CHUNK) {

            CompoundTag tag = entity.getPersistentData();

            if (tag.getBoolean("CursedSpirit")) {
                if (tag.getBoolean("CSM_RESTORED")) return;

                tag.putBoolean("CSM_RESTORED", true);

                String ownerUuidStr =
                        tag.getString("OWNER_UUID");

                if (!ownerUuidStr.isEmpty()) {
                    try {
                        UUID ownerUuid =
                                UUID.fromString(ownerUuidStr);

                        ServerPlayer player = entity.getServer()
                                .getPlayerList()
                                .getPlayer(ownerUuid);

                        String spiritUuid =
                                tag.getString("CSM_SPIRIT_UUID");

                        String entityType =
                                ForgeRegistries.ENTITY_TYPES
                                        .getKey(entity.getType())
                                        .toString();

                        String displayName =
                                entity.getDisplayName().getString();

                        if (player != null) {
                            CsmRoster.restoreSpirit(
                                    player,
                                    entityType,
                                    spiritUuid,
                                    displayName
                            );

                            CsmSync.sendRoster(player);

                        } else if (entity.getServer() != null) {

                            File playerdataDir = entity
                                    .getServer()
                                    .getWorldPath(
                                            LevelResource.PLAYER_DATA_DIR
                                    ).toFile();

                            File playerFile = new File(
                                    playerdataDir,
                                    ownerUuidStr + ".dat"
                            );

                            if (playerFile.exists()) {
                                CompoundTag offlineNbt =
                                        NbtIo.readCompressed(playerFile);

                                CompoundTag forgeData =
                                        offlineNbt.getCompound("ForgeData");

                                CompoundTag rosterRoot =
                                        forgeData.getCompound("csm_roster");

                                ListTag spiritsList =
                                        rosterRoot.getList("spirits", 10);

                                boolean matchesFound = false;

                                for (int i = 0;
                                     i < spiritsList.size();
                                     i++) {

                                    CompoundTag spiritTag =
                                            spiritsList.getCompound(i);

                                    boolean isUnique =
                                            spiritTag.getBoolean("unique");

                                    String uType =
                                            spiritTag.getString("type");

                                    String uUuid =
                                            spiritTag.getString("uuid");

                                    if ((isUnique
                                            && uUuid.equals(spiritUuid))
                                            || (!isUnique
                                            && uType.equals(entityType))) {

                                        spiritTag.putInt(
                                                "count",
                                                spiritTag.getInt("count") + 1
                                        );

                                        matchesFound = true;
                                        break;
                                    }
                                }

                                if (matchesFound) {
                                    rosterRoot.put(
                                            "spirits", spiritsList
                                    );

                                    forgeData.put(
                                            "csm_roster", rosterRoot
                                    );

                                    offlineNbt.put(
                                            "ForgeData", forgeData
                                    );

                                    NbtIo.writeCompressed(
                                            offlineNbt, playerFile
                                    );
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
}
