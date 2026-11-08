
package com.ghosts.abyss.blood;

import com.ghosts.abyss.entity.PiercingBloodEntity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PiercingBloodBeam {

    public static final double MAX_RANGE = 48.0D;

    private static final double HIT_RADIUS = 0.20D;

    // =========================================
    // DAMAGE SETTINGS
    // =========================================

    // 16 HP = 8 hearts.
    private static final float INITIAL_DAMAGE = 16.0F;

    // 2 HP = 1 heart.
    private static final float SUSTAINED_DAMAGE = 2.0F;

    // The opening attack lasts 4 ticks (0.2 seconds).
    private static final int INITIAL_WINDOW_TICKS = 4;

    // Sustained damage can repeat every 10 ticks.
    private static final int SUSTAINED_INTERVAL_TICKS = 10;

    // =========================================
    // ACTIVE BEAMS AND DAMAGE STATES
    // =========================================

    private static final Map<UUID, PiercingBloodEntity>
            ACTIVE_BEAMS = new HashMap<>();

    private static final Map<UUID, DamageState>
            DAMAGE_STATES = new HashMap<>();

    private PiercingBloodBeam() {}

    // Each activation gets its own damage history.
    private static final class DamageState {

        int ticksActive = 0;

        // Targets that already received initial damage.
        final Set<UUID> initialHitTargets = new HashSet<>();

        // Last successful damage tick for each target.
        final Map<UUID, Integer> lastHitTicks = new HashMap<>();
    }

    // =========================================
    // UPDATE PIERCING BLOOD
    // =========================================

    public static void update(ServerPlayer player) {

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        UUID ownerId = player.getUUID();

        PiercingBloodEntity beam = ACTIVE_BEAMS.get(ownerId);

        if (beam == null
                || beam.isRemoved()
                || beam.level() != level) {

            beam = new PiercingBloodEntity(level, player);

            level.addFreshEntity(beam);

            ACTIVE_BEAMS.put(ownerId, beam);

            // A new beam starts a new damage window.
            DAMAGE_STATES.put(ownerId, new DamageState());
        }

        beam.followOwner(player);

        DamageState state = DAMAGE_STATES.computeIfAbsent(
                ownerId,
                id -> new DamageState()
        );

        // =========================================
        // EXISTING BEAM RAY
        // =========================================

        Vec3 direction = player.getLookAngle().normalize();
        Vec3 eye = player.getEyePosition();

        Vec3 start = eye
                .add(direction.scale(0.85D))
                .add(0.0D, -0.28D, 0.0D);

        Vec3 desiredEnd = eye.add(
                direction.scale(MAX_RANGE)
        );

        BlockHitResult aimHit = level.clip(
                new ClipContext(
                        eye,
                        desiredEnd,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                )
        );

        Vec3 aimedEnd =
                aimHit.getType() == HitResult.Type.MISS
                        ? desiredEnd
                        : aimHit.getLocation();

        BlockHitResult beamHit = level.clip(
                new ClipContext(
                        start,
                        aimedEnd,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                )
        );

        Vec3 end =
                beamHit.getType() == HitResult.Type.MISS
                        ? aimedEnd
                        : beamHit.getLocation();

        // =========================================
        // SYNCHRONIZED BEAM LENGTH
        // =========================================

        double visibleLength = end.subtract(start).dot(direction);

        visibleLength = Math.max(
                0.0D,
                Math.min(MAX_RANGE, visibleLength)
        );

        beam.setBeamLength((float) visibleLength);

        // =========================================
        // EXISTING BLOCK IMPACT PARTICLES
        // =========================================

        boolean hitBlock =
                beamHit.getType() == HitResult.Type.BLOCK
                        || aimHit.getType() == HitResult.Type.BLOCK;

        if (hitBlock && player.tickCount % 2 == 0) {
            spawnImpactParticles(level, end, direction);
        }

        // =========================================
        // NEW DAMAGE SYSTEM
        // =========================================

        boolean initialWindow =
                state.ticksActive < INITIAL_WINDOW_TICKS;

        // Check collisions every tick so the opening
        // attack can hit immediately.
        damageEntities(
                player,
                start,
                end,
                state,
                initialWindow
        );

        state.ticksActive++;
    }

    // =========================================
    // INITIAL AND SUSTAINED DAMAGE
    // =========================================

    private static void damageEntities(
            ServerPlayer player,
            Vec3 start,
            Vec3 end,
            DamageState state,
            boolean initialWindow
    ) {

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        AABB searchBox = new AABB(start, end)
                .inflate(HIT_RADIUS + 1.0D);

        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> entity.isAlive()
                        && entity != player
                        && !entity.isSpectator()
        )) {

            AABB hitbox = target.getBoundingBox()
                    .inflate(HIT_RADIUS);

            boolean intersects =
                    hitbox.contains(start)
                            || hitbox.clip(start, end).isPresent();

            if (!intersects) {
                continue;
            }

            UUID targetId = target.getUUID();

            // =====================================
            // INITIAL PIERCING BLOOD
            // =====================================

            if (initialWindow) {

                // Only one powerful hit per target.
                if (state.initialHitTargets.contains(targetId)) {
                    continue;
                }

                boolean damaged = target.hurt(
                        level.damageSources().magic(),
                        INITIAL_DAMAGE
                );

                if (damaged) {

                    state.initialHitTargets.add(targetId);

                    state.lastHitTicks.put(
                            targetId,
                            state.ticksActive
                    );
                }

                continue;
            }

            // =====================================
            // REDIRECTED / SUSTAINED PIERCING BLOOD
            // =====================================

            Integer lastHit = state.lastHitTicks.get(targetId);

            // Allow an immediate sustained hit on a
            // newly encountered target, but limit
            // repeated hits on the same target.
            if (lastHit != null
                    && state.ticksActive - lastHit
                    < SUSTAINED_INTERVAL_TICKS) {

                continue;
            }

            boolean damaged = target.hurt(
                    level.damageSources().magic(),
                    SUSTAINED_DAMAGE
            );

            if (damaged) {

                state.lastHitTicks.put(
                        targetId,
                        state.ticksActive
                );
            }
        }
    }

    // =========================================
    // BLOCK IMPACT PARTICLES
    // =========================================

    private static void spawnImpactParticles(
            ServerLevel level,
            Vec3 impact,
            Vec3 direction
    ) {

        ParticleOptions crimson = new DustParticleOptions(
                new Vector3f(0.47F, 0.0F, 0.05F),
                1.0F
        );

        level.sendParticles(
                crimson,
                impact.x,
                impact.y,
                impact.z,
                5,
                0.10D,
                0.10D,
                0.10D,
                0.015D
        );

        ParticleOptions brightRed = new DustParticleOptions(
                new Vector3f(0.85F, 0.08F, 0.12F),
                0.7F
        );

        level.sendParticles(
                brightRed,
                impact.x,
                impact.y,
                impact.z,
                2,
                0.035D,
                0.035D,
                0.035D,
                0.005D
        );
    }

    // =========================================
    // STOP PIERCING BLOOD
    // =========================================

    public static void stop(ServerPlayer player) {

        UUID ownerId = player.getUUID();

        PiercingBloodEntity beam =
                ACTIVE_BEAMS.remove(ownerId);

        if (beam != null && !beam.isRemoved()) {
            beam.discard();
        }

        // Reset all damage history so the next
        // activation receives a fresh initial shot.
        DAMAGE_STATES.remove(ownerId);
    }
}
