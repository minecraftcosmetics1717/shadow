package com.ghosts.abyss.grab;

import com.ghosts.abyss.technique.TechniqueCooldownManager;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
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

/** Server-side input/contact glue for Uro's Blind Rage.
 * Other characters can reuse GrabManager with their own GrabConfig.
 */
@Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlindRageHandler {
    public static final int SKILL_ID = 3809;
    private static final int DURATION_TICKS = 80; // 4 seconds
    private static final int COOLDOWN_TICKS = 200; // 10 seconds

    // First balancing values. Tune these without changing GrabManager.
    private static final float CONTACT_DAMAGE = 8.0F;
    private static final double THROW_POWER = 1.65D;

    private static final GrabConfig BLIND_RAGE = new GrabConfig(
            DURATION_TICKS, CONTACT_DAMAGE, 0.0F, THROW_POWER,
            SKILL_ID, COOLDOWN_TICKS, GrabFinisher.THROW);

    /** One press = at most one successful grab, including if it ends early. */
    private static final class Hold {
        boolean used;
        boolean warned;
    }

    private static final Map<UUID, Hold> HOLDING = new HashMap<>();

    private BlindRageHandler() {}

    public static void setHolding(ServerPlayer player, boolean pressed) {
        if (!pressed) {
            HOLDING.remove(player.getUUID());
            if (GrabManager.isGrabbing(player)) {
                GrabManager.release(player); // Mouse release: directional throw
            }
            return;
        }

        if (!isSelected(player) || !player.isAlive()
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
        return (int) Math.round(ct) == 38
                && (int) Math.round(vars.PlayerSelectCurseTechnique) == 9;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) return;

        Hold hold = HOLDING.get(player.getUUID());
        if (hold == null) return;

        if (!player.isAlive() || !isSelected(player)) {
            HOLDING.remove(player.getUUID());
            if (GrabManager.isGrabbing(player)) GrabManager.interrupt(player);
            return;
        }

        if (GrabManager.isGrabbing(player) || hold.used) return;
        if (TechniqueCooldownManager.isOnCooldown(player, SKILL_ID)
                || TechniqueCooldownManager.getPostDomainRemainingTicks(player) > 0) return;

        LivingEntity target = findContact(player);
        if (target == null) return;

        var vars = player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
        if (vars == null) return;

        // The display mixin supplies the effective cost, including JC discounts.
        double cost = Math.max(0.0D, vars.PlayerSelectCurseTechniqueCost);
        if (!player.isCreative()) {
            double available = vars.PlayerCursePower + vars.PlayerCursePowerChange;
            if (available < cost) {
                if (!hold.warned) {
                    player.displayClientMessage(
                            Component.literal("Not enough cursed energy for Blind Rage."), true);
                    hold.warned = true;
                }
                return;
            }
        }

        if (GrabManager.tryStart(player, target, BLIND_RAGE)) {
            hold.used = true;
            if (!player.isCreative() && cost > 0.0D) {
                vars.PlayerCursePowerChange -= cost;
                vars.syncPlayerVariables(player);
            }
        }
    }

    /** A server-side reach/contact check, rather than client-supplied victims. */
    private static LivingEntity findContact(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 reachEnd = eye.add(look.scale(2.6D));
        AABB search = player.getBoundingBox().inflate(2.5D);
        List<LivingEntity> nearby = player.serverLevel()
                .getEntitiesOfClass(LivingEntity.class, search,
                        e -> e != player && e.isAlive() && !GrabManager.isGrabbed(e));

        LivingEntity best = null;
        double closest = Double.MAX_VALUE;
        for (LivingEntity candidate : nearby) {
            double distance = player.distanceToSqr(candidate);
            if (distance > 2.7D * 2.7D || distance >= closest) continue;
            if (!player.hasLineOfSight(candidate)) continue;

            // Aim at the target OR physically collide with it while facing it.
            boolean aiming = candidate.getBoundingBox().inflate(0.3D)
                    .clip(eye, reachEnd).isPresent();
            Vec3 toTarget = candidate.getBoundingBox().getCenter()
                    .subtract(player.getBoundingBox().getCenter());
            boolean touching = distance < 1.75D * 1.75D
                    && toTarget.lengthSqr() > 1.0E-6D
                    && toTarget.normalize().dot(look) > 0.05D;
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
            GrabManager.interrupt(player);
        }
    }
}
