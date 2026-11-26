package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.entity.SlotEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** Visual-only JC reels. HakariGambleManager alone decides the outcome. */
@Mod.EventBusSubscriber(modid = "abyss")
public final class HakariDomainCards {
    private static final String MARK = "abyss_hakari_card";
    private HakariDomainCards() {}

    public static boolean isCard(Entity entity) {
        return entity instanceof SlotEntity && entity.getPersistentData().getBoolean(MARK);
    }

    public static final class CardSet {
        private final UUID[] ids = new UUID[3];
        private final UUID owner;

        private CardSet(DomainInstance domain) {
            this.owner = domain.ownerId;
        }

        private SlotEntity get(ServerLevel level, int index) {
            Entity entity = ids[index] == null ? null : level.getEntity(ids[index]);
            return entity instanceof SlotEntity slot && isCard(slot) ? slot : null;
        }

        public boolean contains(UUID id) {
            for (UUID card : ids) if (id.equals(card)) return true;
            return false;
        }

        public void revealOuter(ServerLevel level, int number) {
            reveal(level, 0, number);
            reveal(level, 2, number);
        }

        public void revealMiddle(ServerLevel level, int number) {
            reveal(level, 1, number);
        }

        private void reveal(ServerLevel level, int index, int number) {
            SlotEntity slot = get(level, index);
            if (slot == null) return;
            int visualNumber = Math.max(1, Math.min(7, number));
            slot.getEntityData().set(SlotEntity.DATA_SLOT_NUM, visualNumber);
            slot.getEntityData().set(SlotEntity.DATA_ANIMATION_NAME, "slot" + visualNumber);
            slot.setAnimation("slot" + visualNumber);
        }

        public void clear(ServerLevel level) {
            for (int i = 0; i < ids.length; i++) {
                SlotEntity slot = get(level, i);
                if (slot != null) slot.discard();
                ids[i] = null;
            }
        }
    }

    public static CardSet spawn(ServerLevel level, DomainInstance inst) {
        CardSet cards = new CardSet(inst);
        DomainClashParticipants.Participant participant = inst.participants.get(inst.ownerId);
        DomainClashParticipants.Side side = participant == null
                ? DomainClashParticipants.Side.NORTH : participant.side;
        double ox = 0, oz = 0;
        float yaw = 0;
        switch (side) {
            case NORTH -> { oz = -1; yaw = 0; }
            case SOUTH -> { oz = 1; yaw = 180; }
            case WEST -> { ox = -1; yaw = -90; }
            case EAST -> { ox = 1; yaw = 90; }
        }
        double cx = inst.arenaCenter.getX() + 0.5;
        double cy = inst.arenaCenter.getY() + 2.0;
        double cz = inst.arenaCenter.getZ() + 0.5;
        // Three cards, facing the center of the arena. Keep clear of the ticket gates.
        for (int i = 0; i < 3; i++) {
            SlotEntity slot = JujutsucraftModEntities.SLOT.get().create(level);
            if (slot == null) continue;
            double along = (i - 1) * 7.0;
            double x = cx + ox * 28.0 - oz * along;
            double z = cz + oz * 28.0 + ox * along;
            slot.moveTo(x, cy, z, yaw, 0);
            slot.setYHeadRot(yaw);
            slot.setYBodyRot(yaw);
            slot.setNoAi(true);
            slot.setNoGravity(true);
            slot.setInvulnerable(true);
            slot.setPersistenceRequired();
            slot.getPersistentData().putBoolean(MARK, true);
            slot.getPersistentData().putBoolean("abyss_domain_decoration", true);
            slot.getPersistentData().putUUID("abyss_decoration_owner", cards.owner);
            slot.getPersistentData().putString("OWNER_UUID", cards.owner.toString());
            slot.getPersistentData().putInt("abyss_hakari_card_slot", inst.slot);
            slot.getEntityData().set(SlotEntity.DATA_SLOT_MODE, i == 1 ? 3 : i == 0 ? 1 : 2);
            slot.getEntityData().set(SlotEntity.DATA_SLOT_NUM, 0);
            // JC's default idle animation spins continuously. No native slot AI is run.
            if (level.addFreshEntity(slot)) cards.ids[i] = slot.getUUID();
        }
        return cards;
    }

    /** Remove any lingering native JC reels for this owner, without touching other domains. */
    public static void removeNativeCards(ServerLevel level, DomainInstance inst) {
        int r = DomainDimension.OUTER_RADIUS + 4;
        var c = inst.arenaCenter;
        AABB box = new AABB(c.getX()-r, c.getY()-r, c.getZ()-r,
                c.getX()+r+1, c.getY()+r+1, c.getZ()+r+1);
        for (SlotEntity slot : level.getEntitiesOfClass(SlotEntity.class, box,
                e -> !isCard(e) && inst.ownerId.toString().equals(e.getPersistentData().getString("OWNER_UUID")))) {
            slot.discard();
        }
    }

    @SubscribeEvent public static void onAttack(LivingAttackEvent event) {
        if (isCard(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent public static void onTarget(LivingChangeTargetEvent event) {
        if (isCard(event.getNewTarget())) event.setNewTarget(null);
    }

    @SubscribeEvent public static void onTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof SlotEntity slot) || !isCard(slot)
                || !(slot.level() instanceof ServerLevel level)) return;
        DomainInstance inst = DomainInstanceManager.getBySlot(slot.getPersistentData().getInt("abyss_hakari_card_slot"));
        if (inst == null || inst.skillDomain != 29.0D || inst.participants.size() != 1
                || !inst.ownerId.toString().equals(slot.getPersistentData().getString("OWNER_UUID"))
                || !HakariGambleManager.ownsCard(inst, slot.getUUID())) {
            slot.discard();
            return;
        }
        slot.setDeltaMovement(Vec3.ZERO);
        slot.setNoGravity(true);
    }
}
