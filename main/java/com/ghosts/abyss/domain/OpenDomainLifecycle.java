
package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.BlockDestroyAllDirectionProcedure;
import net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure;
import net.mcreator.jujutsucraft.procedures.RangeAttackProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.mcreator.jujutsucraft.procedures.TechniqueDecideProcedure;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

    private static void selectCancelDomain(ServerPlayer player) {
        player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).ifPresent(vars -> {

            double techniqueId = vars.SecondTechnique
                    ? vars.PlayerCurseTechnique2
                    : vars.PlayerCurseTechnique;

            // Use Jujutsucraft's original technique selection
            // procedure to update and synchronize the slot.
            TechniqueDecideProcedure.execute(
                    player,
                    true,
                    true,
                    0.0D,
                    techniqueId,
                    21.0D,
                    Component.translatable(
                            "jujutsu.technique.cancel_domain"
                    ).getString()
            );
        });
    }

    private static void restoreDomainSelection(ServerPlayer player) {
        player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
        ).ifPresent(vars -> {

            // Don't override another technique the player
            // manually selected while their domain was active.
            if (vars.PlayerSelectCurseTechnique != 21.0D) {
                return;
            }

            // Refresh the original domain-expansion name and
            // cost using JC's own technique-selection procedure.
            vars.PlayerSelectCurseTechnique = 20.0D;
            vars.noChangeTechnique = true;

            KeyChangeTechniqueOnKeyPressedProcedure.execute(
                    player.serverLevel(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player
            );
        });
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
        data.putDouble("dust_amount", 0.0D);
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

// Release JC's running technique lock.
        data.putDouble("skill", 0.0D);
        data.putDouble("cnt3", 0.0D);

        player.removeEffect(
                JujutsucraftModMobEffects.CURSED_TECHNIQUE.get()
        );

// Switch Malevolent Shrine to Cancel Domain.
        selectCancelDomain(player);

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

        // Match JC: remove accumulated DUST on domain expiration.
        entity.getPersistentData().putDouble("dust_amount", 0.0D);

        if (entity instanceof ServerPlayer player) {
            player.getCapability(
                    JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
            ).ifPresent(vars -> {
                if ("DUST".equals(vars.OVERLAY1)) {
                    vars.OVERLAY1 = "";
                    vars.OVERLAY2 = "";
                    vars.syncPlayerVariables(player);
                }
            });
        }

        var cursedTechnique =
                ForgeRegistries.MOB_EFFECTS.getValue(
                        new net.minecraft.resources.ResourceLocation(
                                "jujutsucraft", "cursed_technique"
                        )
                );

        if (cursedTechnique != null) {
            entity.removeEffect(cursedTechnique);
        }
        // Return the selected ability to Domain Expansion
// once the open domain has ended.
        if (entity instanceof ServerPlayer player) {
            restoreDomainSelection(player);
        }
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

// Prepare targets using JC's Neutralization effect.
            prepareShrineTargets(level, player, session);

// Execute JC's original slash attacks.
            tickJcShrineAttacks(level, player, session);
        }
    }


    private static void prepareShrineTargets(
            ServerLevel level,
            ServerPlayer owner,
            Session session
    ) {
        Vec3 center = session.center();

        // Match the region covered by the four
        // original JC RangeAttackProcedure calls.
        AABB area = new AABB(
                center.x - RADIUS,
                center.y - 15.0D,
                center.z - RADIUS,
                center.x + RADIUS,
                center.y + RADIUS - 15.0D,
                center.z + RADIUS
        );

        for (LivingEntity target :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        e -> e.isAlive() && e != owner
                )) {

            // JC does not neutralize another
            // entity's active Domain Expansion.
            if (target.hasEffect(
                    JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()
            )) {
                continue;
            }

            // Respect JC's domain-selection check.
            if (target.getPersistentData()
                    .getDouble("select") != 0.0D) {
                continue;
            }

            // JC's original Neutralization effect:
            // duration 20 ticks, amplifier domainId + 10.
            target.addEffect(new MobEffectInstance(
                    JujutsucraftModMobEffects.NEUTRALIZATION.get(),
                    20,
                    11,
                    false,
                    false
            ));
        }
    }

    private static void tickJcShrineAttacks(
            ServerLevel level,
            ServerPlayer owner,
            Session session
    ) {
        var data = owner.getPersistentData();
        Vec3 center = session.center();

        double span = RADIUS * 2.0D;

        // Save existing technique and attack settings.
        double oldSkill = data.getDouble("skill");
        double oldCooldown = data.getDouble("COOLDOWN_TICKS");
        double oldDamage = data.getDouble("Damage");
        double oldRange = data.getDouble("Range");
        double oldEffect = data.getDouble("effect");
        double oldEffectConfirm = data.getDouble("effectConfirm");
        double oldKnockback = data.getDouble("knockback");
        double oldYKnockback = data.getDouble("y_knockback");
        double oldBlockRange = data.getDouble("BlockRange");
        double oldBlockDamage = data.getDouble("BlockDamage");

        boolean oldSwing = data.getBoolean("swing");
        boolean oldAttack = data.getBoolean("attack");
        boolean oldDomainAttack = data.getBoolean("DomainAttack");
        boolean oldExtinction = data.getBoolean("ExtinctionBlock");
        boolean oldNoParticle = data.getBoolean("noParticle");
        boolean oldNoEffect = data.getBoolean("noEffect");

        try {
            data.putDouble("y_knockback", -0.1D);


// Mirror JC's remaining Domain Expansion duration.
// Abyss keeps its own 160-second domain lifecycle.
            int remainingTicks = Math.max(
                    0,
                    session.endTick() - level.getServer().getTickCount()
            );

// JC uses the remaining duration to select
// the slash technique and damage value.
            for (int i = 0; i < 4; i++) {

                double xSign = i % 2 == 0 ? 1.0D : -1.0D;
                double zSign = i % 4 <= 1 ? 1.0D : -1.0D;

                double x = center.x + span * 0.25D * xSign;
                double y = center.y + span * 0.25D - 15.0D;
                double z = center.z + span * 0.25D * zSign;


// Exact JC slash selection based on remaining duration.
                if (remainingTicks % 2 == 1) {
                    data.putDouble("skill", 105.0D);
                    data.putDouble("COOLDOWN_TICKS", 50.0D);
                } else {
                    data.putDouble("skill", 106.0D);
                    data.putDouble("COOLDOWN_TICKS", 100.0D);
                }

// Original JC damage formula.
                data.putDouble(
                        "Damage",
                        10.5D + (remainingTicks % 10) * 0.1D);
                data.putDouble("Range", span * 0.5D);

                data.putDouble("effect", 1.0D);
                data.putDouble("effectConfirm", 1.0D);
                data.putDouble("knockback", 0.001D);

                data.putBoolean("swing", false);
                data.putBoolean("attack", false);
                data.putBoolean("DomainAttack", true);
                data.putBoolean("ExtinctionBlock", true);

                RangeAttackProcedure.execute(level, x, y, z, owner);
            }

            // JC's destruction sampling.
            double dust = data.getDouble("dust_amount");
            if (dust <= 0.0D) dust = 1.0D;

            double attempts = 0.0D;

            for (int i = 0; i < 512; i++) {
                double angle = Math.toRadians(
                        level.random.nextDouble() * 360.0D
                );

                double offset = span * 0.5D *
                        (level.random.nextDouble() * 2.0D - 1.0D);

                double x = center.x + Math.sin(angle) * offset;
                double y = center.y +
                        level.random.nextDouble() * span * 0.2D;
                double z = center.z + Math.cos(angle) * offset;

                if (!level.isEmptyBlock(BlockPos.containing(x, y, z))) {
                    dust = Math.min(dust + 1.0D, 200.0D);

                    level.sendParticles(
                            ParticleTypes.EXPLOSION,
                            x, y, z,
                            2, 1.5D, 1.5D, 1.5D, 0.0D
                    );

                    level.sendParticles(
                            ParticleTypes.POOF,
                            x, y, z,
                            2, 1.5D, 1.5D, 1.5D, 0.5D
                    );

                    data.putBoolean("noParticle", true);
                    data.putBoolean("ExtinctionBlock", true);
                    data.putDouble("BlockRange", 16.0D);
                    data.putDouble("BlockDamage", 99.0D);

                    BlockDestroyAllDirectionProcedure.execute(
                            level,
                            x,
                            Math.max(y, center.y + 8.0D),
                            z,
                            owner
                    );

                    if (attempts < 32.0D
                            || attempts / 512.0D
                            < level.random.nextDouble()) {
                        break;
                    }
                }

                attempts++;
            }

            data.putDouble("dust_amount", dust);

            // Jujutsucraft's original 10-square DUST overlay.
            if (owner.tickCount % 10 == 0) {
                int filled = (int) Math.round(
                        dust / 200.0D * 10.0D
                );

                filled = Math.max(0, Math.min(10, filled));

                StringBuilder meter = new StringBuilder("§l§4");

                for (int i = 0; i < 10; i++) {
                    if (i == filled) {
                        meter.append("§r§7");
                    }
                    meter.append("■");
                }

                String dustMeter = meter.toString();

                owner.getCapability(
                        JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
                ).ifPresent(vars -> {
                    vars.OVERLAY1 = "DUST";
                    vars.OVERLAY2 = dustMeter;
                    vars.syncPlayerVariables(owner);
                });
            }

        } finally {
            data.putDouble("skill", oldSkill);
            data.putDouble("COOLDOWN_TICKS", oldCooldown);
            data.putDouble("Damage", oldDamage);
            data.putDouble("Range", oldRange);
            data.putDouble("effect", oldEffect);
            data.putDouble("effectConfirm", oldEffectConfirm);
            data.putDouble("knockback", oldKnockback);
            data.putDouble("y_knockback", oldYKnockback);
            data.putDouble("BlockRange", oldBlockRange);
            data.putDouble("BlockDamage", oldBlockDamage);

            data.putBoolean("swing", oldSwing);
            data.putBoolean("attack", oldAttack);
            data.putBoolean("DomainAttack", oldDomainAttack);
            data.putBoolean("ExtinctionBlock", oldExtinction);
            data.putBoolean("noParticle", oldNoParticle);
            data.putBoolean("noEffect", oldNoEffect);
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
