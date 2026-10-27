
package com.ghosts.abyss.domain;

import com.ghosts.abyss.effect.ModEffects;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncDomainHud;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class DomainHudSync {

    private DomainHudSync() {}

    public static void tick(ServerPlayer player) {
        boolean inDim = player.level().dimension().equals(DomainDimension.ID);

        DomainInstance inst = null;

        if (inDim) {
            inst = DomainClash.findInstanceContaining(player.position());
        }

        if (inst == null) {
            inst = DomainInstanceManager.getByOwner(player.getUUID());
        }

        if (inst == null) {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new PacketSyncDomainHud(
                            false, false, false, "", List.of()
                    )
            );
            return;
        }

        boolean yourDomain =
                inst.ownerId.equals(player.getUUID())
                        || (player.getPersistentData().getBoolean("abyss_domain_clashing")
                        && player.hasEffect(
                        net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(
                                DomainEvents.DOMAIN_EFFECT_ID
                        )
                ));

        String ownerName = "?";

        ServerLevel domainLevel =
                player.server.getLevel(DomainDimension.ID);

        if (domainLevel != null) {
            LivingEntity owner =
                    DomainArenaService.findLivingIn(
                            domainLevel, inst.ownerId
                    );

            if (owner == null) {
                ServerLevel over =
                        player.server.getLevel(inst.returnDim);

                if (over != null) {
                    owner = DomainArenaService.findLivingIn(
                            over, inst.ownerId
                    );
                }
            }

            if (owner != null) {
                ownerName = owner.getName().getString();
            }
        }

        List<String> clashers = new ArrayList<>();
        boolean clashing = false;

        if (domainLevel != null) {
            int r = DomainDimension.OUTER_RADIUS + 4;
            var c = inst.arenaCenter;

            var box = new net.minecraft.world.phys.AABB(
                    c.getX() - r,
                    c.getY() - r,
                    c.getZ() - r,
                    c.getX() + r,
                    c.getY() + r,
                    c.getZ() + r
            );

            for (LivingEntity e : domainLevel.getEntitiesOfClass(
                    LivingEntity.class,
                    box,
                    ent -> true
            )) {
                if (e.getUUID().equals(inst.ownerId)) {
                    continue;
                }

                var data = e.getPersistentData();

                if (data.getBoolean("DomainDefeated")) {
                    continue;
                }

                if (!data.getBoolean("abyss_domain_clashing")) {
                    continue;
                }

                var jcDomain = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(
                        DomainEvents.DOMAIN_EFFECT_ID
                );

                if (jcDomain == null || !e.hasEffect(jcDomain)) {
                    continue;
                }

                clashers.add(e.getName().getString());
                clashing = true;
            }
        }

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncDomainHud(
                        true,
                        yourDomain,
                        clashing,
                        ownerName,
                        clashers
                )
        );
    }
}
