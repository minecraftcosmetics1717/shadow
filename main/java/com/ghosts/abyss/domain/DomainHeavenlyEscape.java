
package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "abyss",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DomainHeavenlyEscape {

    private static final double ESCAPE_RADIUS =
            DomainDimension.INNER_RADIUS - 8.0;

    private DomainHeavenlyEscape() {}

    private static boolean hasPhysicalRestriction(
            ServerPlayer player
    ) {
        var physicalEffect =
                JujutsucraftModMobEffects
                        .PHYSICAL_GIFTED_EFFECT.get();

        var activeEffect = player.getEffect(physicalEffect);

        if (activeEffect == null
                || activeEffect.getAmplifier() < 4) {
            return false;
        }

        return player.getCapability(
                JujutsucraftModVariables
                        .PLAYER_VARIABLES_CAPABILITY
        ).map(vars ->
                vars.PlayerCurseTechnique != 0.0D
                        && vars.PlayerCursePowerFormer == 0.0D
        ).orElse(false);
    }

    @SubscribeEvent
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.level()
                instanceof ServerLevel domainLevel)) {
            return;
        }

        if (!domainLevel.dimension().equals(
                DomainDimension.ID)) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!hasPhysicalRestriction(player)) {
            return;
        }

        DomainInstance inst =
                DomainClash.findInstanceContaining(
                        player.position()
                );

        if (inst == null) {
            return;
        }

        // Don't let the domain owner escape
        // through the participant-only mechanic.
        if (player.getUUID().equals(inst.ownerId)) {
            return;
        }

        Vec3 arenaCenter =
                Vec3.atCenterOf(inst.arenaCenter);

        double distance =
                player.position().distanceTo(arenaCenter);

        if (distance < ESCAPE_RADIUS
                || distance > DomainDimension.OUTER_RADIUS + 2.0) {
            return;
        }

        // Must be right-clicking a nearby block.
        if (player.distanceToSqr(
                Vec3.atCenterOf(event.getPos())
        ) > 36.0) {
            return;
        }

        ServerLevel returnLevel =
                domainLevel.getServer().getLevel(
                        inst.returnDim
                );

        if (returnLevel == null) {
            return;
        }

        Vec3 destination =
                DomainSpawnPositions.heavenlyEscapePosition(
                        returnLevel,
                        inst,
                        player
                );

        // If no safe location exists outside
        // the sphere, do not teleport.
        if (destination == null) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "No safe escape location found."
                    ),
                    true
            );
            return;
        }

        var returned = DomainTeleport.sendBack(
                player,
                returnLevel,
                destination
        );

        if (returned == null
                || returned.level() != returnLevel) {
            return;
        }

        // Remove the old domain state from
        // the escaped participant.
        DomainArenaService.stripDomainFrom(returned);

        event.setCancellationResult(
                InteractionResult.SUCCESS
        );
        event.setCanceled(true);
    }
}
