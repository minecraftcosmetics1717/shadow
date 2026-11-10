
package com.ghosts.abyss.csm;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Observes skills used by deployed CSM spirits.
 *
 * Prepared replacement for the observation logic
 * currently inside EventListeners.
 *
 * This class is intentionally not registered to
 * the Forge event bus until the final migration.
 */
public final class CsmObservationEvents {

    private CsmObservationEvents() {}

    /**
     * Called once per server player tick
     * after the final migration.
     *
     * Only observes every 10 ticks when the
     * player has a CSM roster.
     */
    public static void onPlayerTick(ServerPlayer player) {

        if (player.tickCount % 10 == 0
                && CsmRoster.hasRoster(player)) {

            observeDeployedSpiritSkills(player);
        }
    }

    /**
     * Finds deployed cursed spirits owned by this player
     * and records their current skill.
     */
    private static void observeDeployedSpiritSkills(
            ServerPlayer player) {

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 pos = player.position();

        serverLevel.getEntitiesOfClass(
                Entity.class,
                new AABB(pos, pos).inflate(128.0D),
                entity -> entity.isAlive()
                        && entity.getPersistentData()
                        .getBoolean("CursedSpirit")
                        && entity.getPersistentData()
                        .getString("OWNER_UUID")
                        .equals(player.getStringUUID())
        ).forEach(entity -> {

            double skill = entity.getPersistentData()
                    .getDouble("skill");

            // Only record positive, whole-number skill IDs.
            if (skill > 0.0D && skill == Math.floor(skill)) {

                int skillId = (int) skill;

                int[] existing = entity.getPersistentData()
                        .getIntArray("CSM_PENDING_SKILLS");

                // Maximum of 16 tracked skills per spirit.
                if (existing.length >= 16) {
                    return;
                }

                // Avoid recording the same skill twice.
                for (int recorded : existing) {
                    if (recorded == skillId) {
                        return;
                    }
                }

                // Append the newly observed skill.
                int[] updated = new int[existing.length + 1];

                System.arraycopy(
                        existing,
                        0,
                        updated,
                        0,
                        existing.length
                );

                updated[existing.length] = skillId;

                entity.getPersistentData().putIntArray(
                        "CSM_PENDING_SKILLS",
                        updated
                );
            }
        });
    }
}
