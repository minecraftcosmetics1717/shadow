package com.ghosts.abyss.grab;

import com.ghosts.abyss.soul.SoulAwareness;
import com.ghosts.abyss.technique.TechniqueCooldownManager;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Mahito's Soul Grasp: Blind Rage's drag/throw, plus a delayed soul hit. */
@Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SoulGraspHandler {
    public static final int SKILL_ID = 1511; // CT 15, slot 11 (unused in JC)
    private static final int DURATION_TICKS = 40;   // 2 seconds
    private static final int COOLDOWN_TICKS = 200; // 10 seconds
    private static final float CONTACT_DAMAGE = 8.0F;
    private static final double THROW_POWER = 1.65D;

    private static final int SOUL_DELAY_TICKS = 20; // 1 second after release
    // Preserve the existing Soul Grasp soul-damage multiplier.
    // Domain Amplification suppresses the delayed soul hit entirely.
    private static final float SOUL_DAMAGE_MULTIPLIER = 1F;

    private static final GrabConfig SOUL_GRASP = new GrabConfig(
            DURATION_TICKS, CONTACT_DAMAGE, 0.0F, THROW_POWER,
            SKILL_ID, COOLDOWN_TICKS, GrabFinisher.THROW);

    private static final class Hold {
        boolean used;
    }

    private record PendingSoulHit(UUID targetId, ServerLevel level, long hitAtTick) {}

    private static final Map<UUID, Hold> HOLDING = new HashMap<>();
    private static final Map<UUID, PendingSoulHit> PENDING = new HashMap<>();

    private SoulGraspHandler() {}

    /** Called by the existing PacketBlindRageInput on press/release. */
    public static void setHolding(ServerPlayer player, boolean pressed) {
        if (!pressed) {
            HOLDING.remove(player.getUUID());
            if (GrabManager.isGrabbing(player)) {
                GrabManager.release(player);
            }
            return;
        }

        if (!player.isAlive() || !isSelected(player)
                || TechniqueCooldownManager.isOnCooldown(player, SKILL_ID)
                || TechniqueCooldownManager.getPostDomainRemainingTicks(player) > 0) {
            return;
        }
        HOLDING.computeIfAbsent(player.getUUID(), unused -> new Hold());
    }

    public static boolean isSelected(ServerPlayer player) {
        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
        if (vars == null) return false;
        double ct = vars.SecondTechnique
                ? vars.PlayerCurseTechnique2 : vars.PlayerCurseTechnique;
        return (int) Math.round(ct) == 15
                && (int) Math.round(vars.PlayerSelectCurseTechnique) == 11;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) return;

        // Resolve queued soul hits even when Soul Grasp is no longer selected.
        PendingSoulHit pending = PENDING.get(player.getUUID());
        // DA suppresses the soul portion entirely, even while the hit waits.
        if (pending != null && player.hasEffect(
                JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get())) {
            PENDING.remove(player.getUUID());
            pending = null;
        }
        if (pending != null && player.serverLevel().getGameTime() >= pending.hitAtTick()) {
            PENDING.remove(player.getUUID()); // exactly one delayed hit
            if (player.isAlive() && player.serverLevel() == pending.level()) {
                Entity entity = pending.level().getEntity(pending.targetId());
                if (entity instanceof LivingEntity target && target.isAlive()
                        && target.level() == player.level()
                        // Never allow the delayed bypass-damage to pierce Infinity.
                        && !target.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get())
                        && !(target instanceof Player targetPlayer
                             && (targetPlayer.isCreative() || targetPlayer.isSpectator()))) {
                    float damage = SoulAwareness.calculateDamage(player, target)
                            * SOUL_DAMAGE_MULTIPLIER;
                    if (damage > 0.0F) {
                        // Same armor-bypassing source as AbyssIdleTransfiguration.
                        target.hurt(target.damageSources().genericKill(), damage);
                    }
                }
            }
        }

        Hold hold = HOLDING.get(player.getUUID());
        if (hold == null) return;
        if (!player.isAlive() || !isSelected(player)) {
            HOLDING.remove(player.getUUID());
            if (GrabManager.isGrabbing(player)) GrabManager.interrupt(player);
            return;
        }
        if (hold.used || GrabManager.isGrabbing(player)
                || TechniqueCooldownManager.isOnCooldown(player, SKILL_ID)
                || TechniqueCooldownManager.getPostDomainRemainingTicks(player) > 0) {
            return;
        }

        LivingEntity target = findContact(player);
        if (target == null) return;

        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
        if (vars == null) return;

        // TechniqueDecideProcedure supplies the real CE cost and discounts.
        double cost = Math.max(0.0D, vars.PlayerSelectCurseTechniqueCost);
        if (!player.isCreative()
                && vars.PlayerCursePower + vars.PlayerCursePowerChange < cost) {
            return;
        }
        if (GrabManager.tryStart(player, target, SOUL_GRASP)) {
            hold.used = true;
            if (!player.isCreative() && cost > 0.0D) {
                vars.PlayerCursePowerChange -= cost;
                vars.syncPlayerVariables(player);
            }
        }
    }

    /** A generic event lets other grab moves implement custom release effects. */
    @SubscribeEvent
    public static void onGrabReleased(GrabReleasedEvent event) {
        if (event.config().skillId() != SKILL_ID) return;
        ServerPlayer grabber = event.grabber();
        // If amplification was used at ANY point during the hold, the
        // technique functions only as a physical grab and throw.
        if (GrabManager.amplificationWasUsed(grabber)
                || grabber.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get())) {
            return;
        }
        LivingEntity target = event.target();
        PENDING.put(grabber.getUUID(), new PendingSoulHit(
                target.getUUID(), grabber.serverLevel(),
                grabber.serverLevel().getGameTime() + SOUL_DELAY_TICKS));
    }

    private static LivingEntity findContact(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 reachEnd = eye.add(look.scale(2.6D));
        AABB search = player.getBoundingBox().inflate(2.5D);
        List<LivingEntity> nearby = player.serverLevel().getEntitiesOfClass(
                LivingEntity.class, search,
                e -> e != player && e.isAlive() && !GrabManager.isGrabbed(e));

        LivingEntity best = null;
        double closest = Double.MAX_VALUE;
        for (LivingEntity candidate : nearby) {
            double distance = player.distanceToSqr(candidate);
            if (distance > 2.7D * 2.7D || distance >= closest) continue;
            if (!player.hasLineOfSight(candidate)) continue;
            boolean aiming = candidate.getBoundingBox().inflate(0.3D)
                    .clip(eye, reachEnd).isPresent();
            Vec3 towards = candidate.getBoundingBox().getCenter()
                    .subtract(player.getBoundingBox().getCenter());
            boolean touching = distance < 1.75D * 1.75D
                    && towards.lengthSqr() > 1.0E-6D
                    && towards.normalize().dot(look) > 0.05D;
            if (!aiming && !touching) continue;
            best = candidate;
            closest = distance;
        }
        return best;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HOLDING.remove(player.getUUID());
            PENDING.remove(player.getUUID());
            GrabManager.interrupt(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HOLDING.remove(player.getUUID());
            PENDING.remove(player.getUUID());
            GrabManager.interrupt(player);
        }
    }
}
