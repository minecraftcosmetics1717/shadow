
package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.KeybindConfig;

import com.ghosts.abyss.ability.AbilityScanHandler;
import com.ghosts.abyss.ability.CursedTechniqueEvents;
import com.ghosts.abyss.blackflash.BlackFlashPoints;
import com.ghosts.abyss.csm.CsmRoster;
import com.ghosts.abyss.csm.CsmSpiritData;
import com.ghosts.abyss.domain.HollowWickerBasketEvents;
import com.ghosts.abyss.domain.OpenDomain;
import com.ghosts.abyss.domain.RefinementPoints;
import com.ghosts.abyss.network.*;
import com.ghosts.abyss.progression.AdvTechPoints;
import com.ghosts.abyss.rct.RctPoints;
import com.ghosts.abyss.sukuna.PerfectBodyEvents;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles player login, death, respawn, and MBA
 * progression preservation.
 *
 * Prepared replacement for EventListeners.
 *
 * IMPORTANT:
 * This class is not registered to Forge's event bus.
 * The original EventListeners remains active until
 * the final migration.
 */
public final class PlayerLifecycleEvents {

    private static final Map<UUID, CompoundTag> deathDataCache =
            new ConcurrentHashMap<>();

    private PlayerLifecycleEvents() {}

    // =========================================
    // PLAYER LOGIN
    // =========================================

    public static void onPlayerLoggedIn(
            PlayerEvent.PlayerLoggedInEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        RefinementPoints.sync(player);
        AdvTechPoints.sync(player);
        OpenDomain.sync(player);
        PacketSyncYutaTechniques.sync(player);
        RctPoints.sync(player);
        BlackFlashPoints.sync(player);
        PacketSyncGamerules.sync(player);

        CompoundTag data = player.getPersistentData();

        boolean unlocked = data.getBoolean("minimap_unlocked");
        boolean enabled = data.getBoolean("minimap_enabled");

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncMinimapState(unlocked, enabled)
        );

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncCopyToggle(
                        data.getBoolean("jslop_disable_copy")
                )
        );

        KeybindConfig.loadFromNbt(data);

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncKeybinds(KeybindConfig.getSlots())
        );

        CursedTechniqueEvents.initializePlayer(player);
        CsmSync.sendRoster(player);

        PerfectBodyEvents.initializePlayer(player);

        for (ServerPlayer other :
                player.server.getPlayerList().getPlayers()) {

            if (other != player) {

                boolean otherState =
                        other.getPersistentData()
                                .getBoolean("perfect_body")
                                && !other.getPersistentData()
                                .getBoolean("jslop_hide_four_arms");

                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new PacketSyncPerfectBodyVisible(
                                other.getUUID(),
                                otherState
                        )
                );
            }
        }
    }

    // =========================================
    // PLAYER DEATH
    // =========================================

    public static void onPlayerDeath(
            LivingDeathEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID uuid = player.getUUID();

        CompoundTag data = player.getPersistentData();

// Reset Hollow Wicker Basket when the player dies.
        HollowWickerBasketEvents.clearPlayer(player);

        CompoundTag snapshot = new CompoundTag();

        copyBasicProgression(data, snapshot);
        copySecondaryTechnique(data, snapshot);

        snapshot.putBoolean(
                "jujutsuslop_absorbed_active",
                false
        );

        // Yuta's copied techniques
        if (data.contains("jslop_yuta_registered")) {
            snapshot.put(
                    "jslop_yuta_registered",
                    data.getList("jslop_yuta_registered", 10).copy()
            );
        }

        if (data.contains("jslop_yuta_domain_idx")) {
            snapshot.putInt(
                    "jslop_yuta_domain_idx",
                    data.getInt("jslop_yuta_domain_idx")
            );
        }

        // Reverse Cursed Technique progression
        snapshot.putInt("rct_exp", data.getInt("rct_exp"));

        snapshot.putFloat(
                "rct_exp_accumulator",
                data.getFloat("rct_exp_accumulator")
        );

        snapshot.putInt(
                "rct_output_tier",
                data.getInt("rct_output_tier")
        );

        snapshot.putInt(
                "rct_proficiency_tier",
                data.getInt("rct_proficiency_tier")
        );

        // Black Flash progression
        for (String key : new String[]{
                "bf_exp",
                "bf_chance_tier",
                "bf_fatigue_tier",
                "bf_energy_tier",
                "bf_brain_tier"
        }) {
            snapshot.putInt(key, data.getInt(key));
        }

        snapshot.putBoolean(
                "bf_at_will_unlocked",
                data.getBoolean("bf_at_will_unlocked")
        );

        snapshot.putBoolean("bf_at_will_charged", false);

        snapshot.putLong(
                "bf_at_will_cooldown_end",
                data.getLong("bf_at_will_cooldown_end")
        );

        // Cursed Spirit Manipulation
        if (CsmRoster.hasRoster(player)) {

            snapshot.put(
                    "csm_roster",
                    data.getCompound("csm_roster").copy()
            );

            if (player.getServer() != null
                    && !AbyssGamerules.isEnabled(
                    player,
                    AbyssGamerules.CSM_KEEP_CURSES
            )) {

                releaseCsmSpirits(player);
            }
        }

        deathDataCache.put(uuid, snapshot);
    }

    // =========================================
    // PLAYER RESPAWN
    // =========================================

    public static void onPlayerRespawn(
            PlayerEvent.PlayerRespawnEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID uuid = player.getUUID();

        CompoundTag snapshot = deathDataCache.remove(uuid);

        if (snapshot == null) {
            return;
        }

        CompoundTag data = player.getPersistentData();

        restoreBasicProgression(data, snapshot);

        data.putBoolean(
                "open_domain",
                snapshot.getBoolean("open_domain_toggled")
        );

        if (snapshot.contains("jujutsuslop_secondary_ct")) {
            data.putDouble(
                    "jujutsuslop_secondary_ct",
                    snapshot.getDouble("jujutsuslop_secondary_ct")
            );
        }

        if (snapshot.contains("jujutsuslop_original_ct")) {

            double originalCt =
                    snapshot.getDouble("jujutsuslop_original_ct");

            data.putDouble(
                    "jujutsuslop_original_ct",
                    originalCt
            );

            restorePlayerCT(player, originalCt);
        }

        data.putBoolean(
                "jujutsuslop_absorbed_active",
                false
        );

        // Restore RCT progression
        data.putInt("rct_exp", snapshot.getInt("rct_exp"));

        data.putFloat(
                "rct_exp_accumulator",
                snapshot.getFloat("rct_exp_accumulator")
        );

        data.putInt(
                "rct_output_tier",
                snapshot.getInt("rct_output_tier")
        );

        data.putInt(
                "rct_proficiency_tier",
                snapshot.getInt("rct_proficiency_tier")
        );

        RctPoints.sync(player);

        // Restore Black Flash progression
        for (String key : new String[]{
                "bf_exp",
                "bf_chance_tier",
                "bf_fatigue_tier",
                "bf_energy_tier",
                "bf_brain_tier"
        }) {
            data.putInt(key, snapshot.getInt(key));
        }

        data.putBoolean(
                "bf_at_will_unlocked",
                snapshot.getBoolean("bf_at_will_unlocked")
        );

        data.putBoolean("bf_at_will_charged", false);

        data.putLong(
                "bf_at_will_cooldown_end",
                snapshot.getLong("bf_at_will_cooldown_end")
        );

        BlackFlashPoints.sync(player);

        // Restore CSM roster if the gamerule permits it
        if (snapshot.contains("csm_roster")
                && player.getServer() != null) {

            if (AbyssGamerules.isEnabled(
                    player,
                    AbyssGamerules.CSM_KEEP_CURSES
            )) {

                data.put(
                        "csm_roster",
                        snapshot.getCompound("csm_roster").copy()
                );

                CsmRoster.setSpirits(
                        player,
                        CsmRoster.getSpirits(player)
                );

                resetCsmSelection(player);

            } else {
                data.remove("csm_roster");
            }

            CsmSync.sendRoster(player);
        }

        RefinementPoints.sync(player);
        AdvTechPoints.sync(player);
        OpenDomain.sync(player);

        // Restore Yuta's copied techniques
        if (snapshot.contains("jslop_yuta_registered")) {
            data.put(
                    "jslop_yuta_registered",
                    snapshot.getList(
                            "jslop_yuta_registered", 10
                    ).copy()
            );
        }

        if (snapshot.contains("jslop_yuta_domain_idx")) {
            data.putInt(
                    "jslop_yuta_domain_idx",
                    snapshot.getInt("jslop_yuta_domain_idx")
            );
        }

        PacketSyncYutaTechniques.sync(player);
    }

    // =========================================
    // SHARED PROGRESSION HELPERS
    // =========================================

    private static void copyBasicProgression(
            CompoundTag from,
            CompoundTag to) {

        for (String key : new String[]{
                "refinement_points_available",
                "domain_refinement",
                "advtech_points_available",
                "advtech_last_known_fame"
        }) {
            to.putDouble(key, from.getDouble(key));
        }

        for (String key : new String[]{
                "unlocked_hwb",
                "witnessed_hwb",
                "perfect_body",
                "witnessed_open_domain",
                "open_domain_unlocked",
                "open_domain_toggled",
                "cursed_object_transform_unlocked"
        }) {
            to.putBoolean(key, from.getBoolean(key));
        }
    }

    private static void restoreBasicProgression(
            CompoundTag data,
            CompoundTag snapshot) {

        copyBasicProgression(snapshot, data);
    }

    private static void copySecondaryTechnique(
            CompoundTag from,
            CompoundTag to) {

        for (String key : new String[]{
                "jujutsuslop_secondary_ct",
                "jujutsuslop_original_ct"
        }) {
            if (from.contains(key)) {
                to.putDouble(key, from.getDouble(key));
            }
        }
    }

    // =========================================
    // CURSED TECHNIQUE RESTORATION
    // =========================================

    private static void restorePlayerCT(
            ServerPlayer player,
            double technique) {

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

            if (vars != null) {

                Field ctField = vars.getClass()
                        .getDeclaredField("PlayerCurseTechnique");

                ctField.setAccessible(true);
                ctField.set(vars, technique);

                vars.getClass()
                        .getDeclaredMethod(
                                "syncPlayerVariables",
                                Entity.class
                        )
                        .invoke(vars, player);
            }

        } catch (Exception e) {
            Abyss.LOGGER.warn(
                    "Could not restore PlayerCurseTechnique: {}",
                    e.getMessage()
            );
        }
    }

    // =========================================
    // CSM DEATH RELEASE
    // =========================================

    private static void releaseCsmSpirits(
            ServerPlayer player) {

        if (!(player.level() instanceof ServerLevel deathLevel)) {
            return;
        }

        try {
            double px = player.getX();
            double py = player.getY();
            double pz = player.getZ();

            for (CsmSpiritData spirit :
                    CsmRoster.getSpirits(player)) {

                if (spirit.count <= 0) {
                    continue;
                }

                ResourceLocation rl =
                        new ResourceLocation(spirit.entityType);

                EntityType<?> type =
                        ForgeRegistries.ENTITY_TYPES.getValue(rl);

                if (type == null) {
                    continue;
                }

                int spawnCount = Math.min(spirit.count, 5);

                for (int i = 0; i < spawnCount; i++) {

                    double angle =
                            Math.random() * Math.PI * 2.0D;

                    double radius =
                            1.5D + Math.random() * 2.5D;

                    Entity released = type.create(deathLevel);

                    if (released == null) {
                        continue;
                    }

                    released.moveTo(
                            px + Math.cos(angle) * radius,
                            py,
                            pz + Math.sin(angle) * radius
                    );

                    if (!spirit.displayName.isEmpty()) {
                        released.setCustomName(
                                Component.literal(spirit.displayName)
                        );
                    }

                    deathLevel.addFreshEntity(released);
                }
            }

        } catch (Exception e) {
            Abyss.LOGGER.warn(
                    "[JSlop] CSM death-release failed: {}",
                    e.getMessage()
            );
        }
    }

    private static void resetCsmSelection(
            ServerPlayer player) {

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

            if (vars != null) {

                Field selectField = vars.getClass()
                        .getDeclaredField(
                                "PlayerSelectCurseTechnique"
                        );

                selectField.setAccessible(true);
                selectField.set(vars, 0.0D);

                vars.getClass()
                        .getDeclaredMethod(
                                "syncPlayerVariables",
                                Entity.class
                        )
                        .invoke(vars, player);
            }

        } catch (Exception e) {
            Abyss.LOGGER.warn(
                    "[JSlop] Could not reset cycling position on respawn: {}",
                    e.getMessage()
            );
        }
    }
}
