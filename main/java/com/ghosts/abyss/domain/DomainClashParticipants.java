
package com.ghosts.abyss.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class DomainClashParticipants {

    public enum Side {
        NORTH,
        SOUTH,
        WEST,
        EAST
    }

    public static final class Participant {
        public final UUID entityId;
        public double skillDomain;
        public Side side;

        public Participant(
                UUID entityId,
                double skillDomain,
                Side side
        ) {
            this.entityId = entityId;
            this.skillDomain = skillDomain;
            this.side = side;
        }
    }

    private final List<Participant> participants =
            new ArrayList<>();

    public DomainClashParticipants(
            UUID originalOwner,
            double originalSkill
    ) {
        participants.add(new Participant(
                originalOwner,
                originalSkill,
                Side.NORTH
        ));
    }

    public List<Participant> all() {
        return Collections.unmodifiableList(participants);
    }

    public Participant get(UUID entityId) {
        for (Participant participant : participants) {
            if (participant.entityId.equals(entityId)) {
                return participant;
            }
        }
        return null;
    }

    public boolean contains(UUID entityId) {
        return get(entityId) != null;
    }

    public Participant add(
            UUID entityId,
            double skillDomain
    ) {
        Participant existing = get(entityId);

        if (existing != null) {
            existing.skillDomain = skillDomain;
            return existing;
        }

        Side side = nextAvailableSide();

        if (side == null) return null;

        Participant participant = new Participant(
                entityId,
                skillDomain,
                side
        );

        participants.add(participant);
        return participant;
    }

    private boolean isOccupied(Side side) {
        for (Participant p : participants) {
            if (p.side == side) {
                return true;
            }
        }
        return false;
    }

    private static Side opposite(Side side) {
        return switch (side) {
            case NORTH -> Side.SOUTH;
            case SOUTH -> Side.NORTH;
            case WEST -> Side.EAST;
            case EAST -> Side.WEST;
        };
    }

    private Side nextAvailableSide() {
        // First priority: opposite the only active domain.
        if (participants.size() == 1) {
            Side preferred = opposite(participants.get(0).side);

            if (!isOccupied(preferred)) {
                return preferred;
            }
        }

        // Otherwise fill remaining positions.
        // With NORTH/SOUTH occupied, WEST is next.
        // With EAST/WEST occupied, NORTH is next.
        for (Side side : new Side[]{
                Side.NORTH,
                Side.SOUTH,
                Side.WEST,
                Side.EAST
        }) {
            if (!isOccupied(side)) {
                return side;
            }
        }

        return null;
    }

    public void remove(UUID entityId) {
        participants.removeIf(
                p -> p.entityId.equals(entityId)
        );
    }

    public int size() {
        return participants.size();
    }
}
