
package com.ghosts.abyss.event;

import com.ghosts.abyss.ability.CursedTechniqueEvents;
import com.ghosts.abyss.csm.CsmObservationEvents;
import com.ghosts.abyss.csm.CsmSpiritEvents;
import com.ghosts.abyss.domain.DomainObservationEvents;
import com.ghosts.abyss.domain.HollowWickerBasketEvents;
import com.ghosts.abyss.domain.OpenDomainStateEvents;
import com.ghosts.abyss.progression.AdvTechPoints;
import com.ghosts.abyss.rika.RikaEnergyEvents;
import com.ghosts.abyss.rika.RikaInteractionEvents;
import com.ghosts.abyss.sukuna.PerfectBodyEvents;

import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * Central Forge event dispatcher for Abyss.
 *
 * All feature implementations live in separate classes.
 *
 * IMPORTANT:
 * This class is NOT registered to Forge yet.
 * EventListeners remains active until final migration.
 */

public final class AbyssEventDispatcher {

    private static boolean registered = false;

    private AbyssEventDispatcher() {}

    public static void register() {

        if (registered) {
            return;
        }

        registered = true;

        var bus = MinecraftForge.EVENT_BUS;

        // Player tick
        bus.addListener(
                AbyssEventDispatcher::onPlayerTick
        );

        // Player lifecycle
        bus.addListener(
                AbyssEventDispatcher::onPlayerLoggedIn
        );

        bus.addListener(
                EventPriority.HIGHEST,
                AbyssEventDispatcher::onPlayerDeath
        );

        bus.addListener(
                EventPriority.LOWEST,
                AbyssEventDispatcher::onPlayerRespawn
        );

        bus.addListener(
                AbyssEventDispatcher::onPlayerLoggedOut
        );

        // Hollow Wicker Basket
        bus.addListener(
                EventPriority.LOWEST,
                AbyssEventDispatcher::onPlayerHurtCancelHwb
        );

        bus.addListener(
                AbyssEventDispatcher::onItemUseStart
        );

        // Cursed Spirit Manipulation
        bus.addListener(
                EventPriority.HIGHEST,
                AbyssEventDispatcher::onCsmSpiritRightClickRecall
        );

        bus.addListener(
                AbyssEventDispatcher::onSpiritDeath
        );

        bus.addListener(
                AbyssEventDispatcher::onSpiritUnload
        );

        // Rika inventory
        bus.addListener(
                AbyssEventDispatcher::onRikaInteract
        );
    }


    // ========================================
    // PLAYER TICK
    // ========================================

    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        // Advanced technique progression
        AdvTechPoints.onTick(player);

        // Cursed technique change detection
        CursedTechniqueEvents.onPlayerTick(player);

        // Witnessing advanced techniques
        DomainObservationEvents.onPlayerTick(player);

        // Hollow Wicker Basket
        HollowWickerBasketEvents.onPlayerTick(player);

        // Sukuna Perfect Body
        PerfectBodyEvents.onPlayerTick(player);

        // Rika cursed energy
        RikaEnergyEvents.onPlayerTick(player);

        // Special Grade double jump
        PlayerAbilityEvents.onPlayerTick(player);

        // Cursed Spirit Manipulation observation
        CsmObservationEvents.onPlayerTick(player);

        // Open Domain state
        OpenDomainStateEvents.onPlayerTick(player);
    }

    // ========================================
    // PLAYER LOGIN
    // ========================================

    public static void onPlayerLoggedIn(
            PlayerEvent.PlayerLoggedInEvent event) {

        PlayerLifecycleEvents.onPlayerLoggedIn(event);
    }

    // ========================================
    // PLAYER DEATH / SPIRIT DEATH
    // ========================================

    public static void onPlayerDeath(
            LivingDeathEvent event) {

        PlayerLifecycleEvents.onPlayerDeath(event);
    }

    public static void onSpiritDeath(
            LivingDeathEvent event) {

        CsmSpiritEvents.onSpiritDeath(event);
    }

    // ========================================
    // PLAYER RESPAWN
    // ========================================

    public static void onPlayerRespawn(
            PlayerEvent.PlayerRespawnEvent event) {

        PlayerLifecycleEvents.onPlayerRespawn(event);
    }

    // ========================================
    // HOLLOW WICKER BASKET
    // ========================================

    public static void onPlayerHurtCancelHwb(
            LivingHurtEvent event) {

        HollowWickerBasketEvents.onPlayerHurtCancelHwb(event);
    }

    public static void onItemUseStart(
            LivingEntityUseItemEvent.Start event) {

        HollowWickerBasketEvents.onItemUseStart(event);
    }

    // ========================================
    // CURSED SPIRIT MANIPULATION
    // ========================================

    public static void onCsmSpiritRightClickRecall(
            PlayerInteractEvent.EntityInteract event) {

        CsmSpiritEvents.onCsmSpiritRightClickRecall(event);
    }

    public static void onSpiritUnload(
            EntityLeaveLevelEvent event) {

        CsmSpiritEvents.onSpiritUnload(event);
    }

    // ========================================
    // RIKA
    // ========================================

    public static void onRikaInteract(
            PlayerInteractEvent.EntityInteract event) {

        RikaInteractionEvents.onRikaInteract(event);
    }

    // ========================================
    // PLAYER DISCONNECT
    // ========================================

    public static void onPlayerLoggedOut(
            PlayerEvent.PlayerLoggedOutEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CursedTechniqueEvents.clearPlayer(player.getUUID());

        HollowWickerBasketEvents.clearPlayer(player.getUUID());
    }
}
