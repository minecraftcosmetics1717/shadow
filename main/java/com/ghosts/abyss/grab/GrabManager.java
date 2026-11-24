package com.ghosts.abyss.grab;

import com.ghosts.abyss.technique.TechniqueCooldownManager;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One server-authoritative grab per player and one grabber per victim.
 *
 * Future abilities call tryStart() when their attack makes contact and
 * release() when the attack button is released. This class handles the
 * following, release finisher, time limit, and cooldown.
 */
@Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GrabManager {

    private static final Map<UUID, Session> ACTIVE = new HashMap<>();
    private static final Set<UUID> HELD_TARGETS = new HashSet<>();

    private static final double CONTACT_RANGE_SQR = 16.0D; // 4 blocks
    private static final double BREAK_RANGE_SQR = 64.0D;   // 8 blocks
    private static final double FOLLOW_DISTANCE = 1.25D;

    private GrabManager() {}

    private record Session(
            UUID targetId,
            ServerLevel level,
            long endTick,
            GrabConfig config
    ) {}

    /** Call only on the server, after the move detects contact. */
    public static boolean tryStart(
            ServerPlayer grabber,
            LivingEntity target,
            GrabConfig config
    ) {
        if (grabber == null || target == null || config == null
                || !grabber.isAlive() || !target.isAlive()
                || grabber == target || grabber.level() != target.level()
                || grabber.distanceToSqr(target) > CONTACT_RANGE_SQR
                || ACTIVE.containsKey(grabber.getUUID())
                || HELD_TARGETS.contains(target.getUUID())
                || HELD_TARGETS.contains(grabber.getUUID())
                || target.isPassenger()) {
            return false;
        }

        if (target instanceof Player player
                && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        if (config.skillId() >= 0 && TechniqueCooldownManager.isOnCooldown(
                grabber, config.skillId())) {
            return false;
        }

        // The grab is validated by reach/contact and server-side eligibility
        // above, not by hurt()'s return value. Some JC damage handlers can
        // apply contact damage while returning false from the hurt call.
        // Previously this resulted in damage but NO grab session/cooldown.
        ACTIVE.put(grabber.getUUID(), new Session(
                target.getUUID(), grabber.serverLevel(),
                grabber.serverLevel().getGameTime() + config.durationTicks(),
                config));
        HELD_TARGETS.add(target.getUUID());

        // Apply the move's contact damage separately. A blocked/reduced hit
        // doesn't silently cancel the grab after contact was confirmed.
        if (config.contactDamage() > 0.0F) {
            target.hurt(grabber.damageSources().mobAttack(grabber),
                    config.contactDamage());
        }
        return true;
    }

    /** Normal release: apply this move's configured finishing effect. */
    public static void release(ServerPlayer grabber) {
        if (grabber != null) {
            finish(grabber, true);
        }
    }

    /** Interruption: stop dragging without applying the finisher. */
    public static void interrupt(ServerPlayer grabber) {
        if (grabber != null) {
            finish(grabber, false);
        }
    }

    public static boolean isGrabbing(ServerPlayer grabber) {
        return grabber != null && ACTIVE.containsKey(grabber.getUUID());
    }

    public static boolean isGrabbed(LivingEntity target) {
        return target != null && HELD_TARGETS.contains(target.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer grabber)) {
            return;
        }

        Session session = ACTIVE.get(grabber.getUUID());
        if (session == null) return;

        Entity entity = session.level().getEntity(session.targetId());
        if (!grabber.isAlive() || grabber.serverLevel() != session.level()
                || !(entity instanceof LivingEntity target)
                || !target.isAlive() || target.level() != grabber.level()) {
            finish(grabber, false);
            return;
        }

        if (grabber.serverLevel().getGameTime() >= session.endTick()) {
            finish(grabber, true);
            return;
        }

        if (grabber.distanceToSqr(target) > BREAK_RANGE_SQR) {
            finish(grabber, false);
            return;
        }

        // Keep the victim a short distance behind the grabber. Check space
        // first so grabbing doesn't force entities through solid blocks.
        Vec3 horizontal = grabber.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = Vec3.directionFromRotation(0.0F, grabber.getYRot());
        }
        horizontal = horizontal.normalize();
        Vec3 desired = grabber.position().subtract(horizontal.scale(FOLLOW_DISTANCE));
        Vec3 offset = desired.subtract(target.position());
        AABB destinationBox = target.getBoundingBox().move(offset);

        if (!session.level().noCollision(target, destinationBox)) {
            return;
        }

        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;

        if (target instanceof ServerPlayer victimPlayer) {
            victimPlayer.connection.teleport(
                    desired.x, desired.y, desired.z,
                    victimPlayer.getYRot(), victimPlayer.getXRot(), Set.of());
        } else {
            target.teleportTo(desired.x, desired.y, desired.z);
        }
        target.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            interrupt(player);
        }
    }

    private static void finish(ServerPlayer grabber, boolean applyFinisher) {
        Session session = ACTIVE.remove(grabber.getUUID());
        if (session == null) return;

        HELD_TARGETS.remove(session.targetId());
        GrabConfig config = session.config();

        Entity entity = session.level().getEntity(session.targetId());
        if (applyFinisher && grabber.isAlive()
                && grabber.serverLevel() == session.level()
                && entity instanceof LivingEntity target && target.isAlive()) {

            if (config.finisher().dealsDamage() && config.releaseDamage() > 0.0F) {
                target.hurt(grabber.damageSources().mobAttack(grabber),
                        config.releaseDamage());
            }

            if (config.finisher().throwsTarget() && config.throwPower() > 0.0D) {
                Vec3 look = grabber.getLookAngle().normalize();
                Vec3 velocity = look.scale(config.throwPower())
                        .add(0.0D, 0.22D, 0.0D);
                target.setDeltaMovement(velocity);
                target.hurtMarked = true;
                target.hasImpulse = true;
                if (target instanceof ServerPlayer victimPlayer) {
                    victimPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
                }
            }
        }

        // Start cooldown after any successful grab, even if an interruption
        // cancels the finisher (so dimension hops cannot bypass cooldown).
        if (config.skillId() >= 0 && config.cooldownTicks() > 0) {
            TechniqueCooldownManager.startCooldown(
                    grabber, config.skillId(), config.cooldownTicks());
        }
    }
}
