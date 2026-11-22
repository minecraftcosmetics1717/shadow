
package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abyss")
public final class OpenDomainLifecycle {

    public static final double RADIUS = 200.0D;

    private static final double RADIUS_SQ = RADIUS * RADIUS;
    private static final int DAMAGE_INTERVAL = 10;
    private static final int DESTRUCTION_INTERVAL = 10;

    private static final Map<UUID, Session> ACTIVE =
            new HashMap<>();

    private record Session(
            ResourceKey<Level> dimension,
            Vec3 center,
            UUID shrineId,
            int endTick
    ) {}

    private OpenDomainLifecycle() {}

    public static boolean isActive(UUID ownerId) {
        return ACTIVE.containsKey(ownerId);
    }

    public static boolean start(ServerPlayer player) {
        if (isActive(player.getUUID())) return true;

        if (!player.level().dimension().equals(Level.OVERWORLD)) {
            return false;
        }

        if (!OpenDomain.shouldUseOpenDomain(player, 1.0D)) {
            return false;
        }

        if (DomainCooldown.cannotExpand(player)) return false;

        if (DomainInstanceManager.getByOwner(player.getUUID()) != null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();

        Entity shrine = JujutsucraftModEntities
                .ENTITY_MALEVOLENT_SHRINE.get().create(level);

        if (shrine == null) return false;

        BlockPos ground = BlockPos.containing(center);

        for (int i = 0; i < 20; i++) {
            BlockPos below = ground.below();
            if (!level.getBlockState(below).isAir()) break;
            ground = below;
        }

        shrine.moveTo(
                ground.getX() + 0.5D,
                ground.getY(),
                ground.getZ() + 0.5D,
                player.getYRot(),
                0.0F
        );

        shrine.setInvulnerable(true);
        shrine.setNoGravity(true);

        if (shrine instanceof net.minecraft.world.entity.Mob mob) {
            mob.setNoAi(true);
        }

        shrine.getPersistentData().putBoolean(
                "abyss_sukuna_shrine", true
        );
        shrine.getPersistentData().putBoolean(
                "abyss_open_shrine", true
        );
        shrine.getPersistentData().putUUID(
                "abyss_decoration_owner", player.getUUID()
        );

        if (!level.addFreshEntity(shrine)) {
            return false;
        }

        int endTick = level.getServer().getTickCount()
                + AbyssDomainCast.DURATION_TICKS;

        ACTIVE.put(
                player.getUUID(),
                new Session(
                        level.dimension(),
                        center,
                        shrine.getUUID(),
                        endTick
                )
        );

        var data = player.getPersistentData();

        data.putBoolean("abyss_open_domain_session", true);
        data.putBoolean("abyss_domain_active", true);
        data.putBoolean("abyss_domain_committed", true);
        data.putBoolean("abyss_domain_handled", true);
        data.putBoolean("abyss_domain_pending", false);

        data.putLong("abyss_domain_end_tick", endTick);
        data.putDouble("skill_domain", 1.0D);
        data.putDouble("x_pos_doma", center.x);
        data.putDouble("y_pos_doma", center.y);
        data.putDouble("z_pos_doma", center.z);

        return true;
    }

    public static void end(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        UUID ownerId = entity.getUUID();
        Session session = ACTIVE.remove(ownerId);
        if (session == null) return;

        MinecraftServer server = entity.getServer();

        if (server != null) {
            removeShrine(server, session);
        }

        entity.getPersistentData().putBoolean(
                "abyss_open_domain_session", false
        );

        DomainLifecycle.clearCastState(entity);

        if (entity.isAlive()) {
            DomainCooldown.apply(entity);
        }
    }

    public static boolean endByOwnerId(
            MinecraftServer server,
            UUID ownerId
    ) {
        if (!isActive(ownerId)) return false;

        ServerPlayer player = server.getPlayerList()
                .getPlayer(ownerId);

        if (player != null) {
            end(player);
            return true;
        }

        Session session = ACTIVE.remove(ownerId);

        if (session != null) {
            removeShrine(server, session);
        }

        return true;
    }

    private static void removeShrine(
            MinecraftServer server,
            Session session
    ) {
        ServerLevel level = server.getLevel(session.dimension());
        if (level == null) return;

        Entity shrine = level.getEntity(session.shrineId());

        if (shrine != null) {
            shrine.discard();
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();

        for (Map.Entry<UUID, Session> entry :
                new ArrayList<>(ACTIVE.entrySet())) {

            UUID ownerId = entry.getKey();
            Session session = entry.getValue();

            ServerPlayer player = server.getPlayerList()
                    .getPlayer(ownerId);

            if (player == null) {
                endByOwnerId(server, ownerId);
                continue;
            }

            if (!player.isAlive()
                    || !player.level().dimension()
                    .equals(session.dimension())
                    || tick >= session.endTick()) {

                end(player);
                continue;
            }

            ServerLevel level = player.serverLevel();

            // Apply sure-hit damage twice per second.
            if (tick % DAMAGE_INTERVAL == 0) {
                tickSureHit(level, player, session);
            }

            // Only open domains destroy terrain.
            if (tick % DESTRUCTION_INTERVAL == 0) {
                tickDestruction(level, player, session);
            }

            // Match the existing Abyss CE drain.
            if (tick % 20 == 0) {
                AbyssDomainCast.drainOpenDomain(player);
            }
        }
    }

    private static void tickSureHit(
            ServerLevel level,
            ServerPlayer owner,
            Session session
    ) {
        Vec3 center = session.center();

        // Search horizontal range of 200 blocks, with
        // 48 blocks vertically in either direction.
        AABB area = new AABB(
                center.x - RADIUS,
                center.y - 48.0D,
                center.z - RADIUS,
                center.x + RADIUS,
                center.y + 48.0D,
                center.z + RADIUS
        );

        for (LivingEntity target :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        e -> e.isAlive() && e != owner
                )) {

            if (target.isAlliedTo(owner)) continue;

            if (target instanceof Player other
                    && (other.isCreative() || other.isSpectator())) {
                continue;
            }

            // Exclude shrine decorations.
            if (target.getType() ==
                    JujutsucraftModEntities
                            .ENTITY_MALEVOLENT_SHRINE.get()
                    || target.getType() ==
                    JujutsucraftModEntities
                            .ENTITY_MALEVOLENT_SHRINE_2.get()) {
                continue;
            }

            double dx = target.getX() - center.x;
            double dz = target.getZ() - center.z;

            if (dx * dx + dz * dz > RADIUS_SQ) continue;

            // Preliminary sure-hit damage.
            // Does not break any blocks.
            target.hurt(
                    owner.damageSources().playerAttack(owner),
                    6.0F
            );

            // Light visual feedback, not a particle storm.
            if (level.random.nextInt(3) == 0) {
                level.sendParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        target.getX(),
                        target.getY() + target.getBbHeight() * 0.5,
                        target.getZ(),
                        1,
                        0.2D,
                        0.2D,
                        0.2D,
                        0.0D
                );
            }
        }
    }

    private static void tickDestruction(
            ServerLevel level,
            ServerPlayer owner,
            Session session
    ) {
        // Respect the world's mob-griefing setting for
        // this experimental terrain-destruction effect.
        if (!level.getGameRules().getBoolean(
                GameRules.RULE_MOBGRIEFING)) {
            return;
        }

        Vec3 center = session.center();

        // Attempt only four positions per update.
        // Never scan hundreds of blocks every tick.
        for (int i = 0; i < 4; i++) {

            double angle = level.random.nextDouble()
                    * Math.PI * 2.0D;

            double radius = 12.0D
                    + Math.sqrt(level.random.nextDouble())
                    * (RADIUS - 16.0D);

            int x = (int) Math.floor(
                    center.x + Math.cos(angle) * radius
            );
            int z = (int) Math.floor(
                    center.z + Math.sin(angle) * radius
            );

            BlockPos probe = new BlockPos(
                    x,
                    (int) center.y,
                    z
            );

            // Never generate new chunks merely to destroy them.
            if (!level.hasChunkAt(probe)) continue;

            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            ) - 1;

            // Keep destruction near the domain's
            // original vertical position.
            if (Math.abs(y - center.y) > 32.0D) continue;

            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);

            if (state.isAir()) continue;
            if (state.getDestroySpeed(level, pos) < 0.0F) continue;

            // Leave inventories and other block entities alone.
            if (level.getBlockEntity(pos) != null) continue;

            var blockId = ForgeRegistries.BLOCKS.getKey(
                    state.getBlock()
            );

            if (blockId == null) continue;

            // Don't destroy Abyss domain blocks or JC blocks.
            if (blockId.getNamespace().equals("abyss")
                    || blockId.getNamespace().equals("jujutsucraft")) {
                continue;
            }

            // Check Forge's block-break protections before
            // removing anything.
            int result = ForgeHooks.onBlockBreakEvent(
                    level,
                    owner.gameMode.getGameModeForPlayer(),
                    owner,
                    pos
            );

            if (result < 0) continue;

            level.destroyBlock(pos, false, owner);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && isActive(player.getUUID())) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && isActive(player.getUUID())) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Recover from an interrupted server shutdown.
        // Never reset the player's saved Open/Closed preference.
        if (!isActive(player.getUUID())
                && player.getPersistentData()
                .getBoolean("abyss_open_domain_session")) {

            player.getPersistentData().putBoolean(
                    "abyss_open_domain_session", false
            );

            DomainLifecycle.clearCastState(player);
        }
    }
}
