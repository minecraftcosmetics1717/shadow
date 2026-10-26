package com.ghosts.abyss.domain;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncDomainHud;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

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
                    new PacketSyncDomainHud(false, false, false, "", List.of())
            );
            return;
        }

        MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(DomainEvents.DOMAIN_EFFECT_ID);
        boolean yourDomain = domainEffect != null && player.hasEffect(domainEffect);

        String ownerName = "?";
        if (player.level() instanceof ServerLevel sl) {
            LivingEntity owner = DomainArenaService.findLivingIn(sl, inst.ownerId);
            if (owner == null) {
                ServerLevel over = sl.getServer().getLevel(inst.returnDim);
                if (over != null) owner = DomainArenaService.findLivingIn(over, inst.ownerId);
            }
            if (owner != null) {
                ownerName = owner.getName().getString();
            }
        }

        List<String> clashers = new ArrayList<>();
        boolean clashing = false;
        if (player.level() instanceof ServerLevel sl && domainEffect != null) {
            int r = DomainDimension.OUTER_RADIUS + 4;
            var c = inst.arenaCenter;
            var box = new net.minecraft.world.phys.AABB(
                    c.getX() - r, c.getY() - r, c.getZ() - r,
                    c.getX() + r, c.getY() + r, c.getZ() + r
            );
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, ent -> true)) {
                if (e.getUUID().equals(inst.ownerId)) continue;
                if (!e.hasEffect(domainEffect)) continue;
                clashers.add(e.getName().getString());
                clashing = true;
            }
            // Also count owner domain vs you
            if (yourDomain && !player.getUUID().equals(inst.ownerId)) {
                clashing = true;
            }
        }

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncDomainHud(true, yourDomain, clashing, ownerName, clashers)
        );
    }
}