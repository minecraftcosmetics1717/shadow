
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
        public final Side side;

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

        if (side == null) {
            return null;
        }

        Participant participant = new Participant(
                entityId,
                skillDomain,
                side
        );

        participants.add(participant);
        return participant;
    }

    private Side nextAvailableSide() {
        for (Side side : new Side[]{
                Side.SOUTH,
                Side.WEST,
                Side.EAST
        }) {
            boolean occupied = false;

            for (Participant participant : participants) {
                if (participant.side == side) {
                    occupied = true;
                    break;
                }
            }

            if (!occupied) {
                return side;
            }
        }

        return null;
    }

    public void remove(UUID entityId) {
        participants.removeIf(
                participant ->
                        participant.entityId.equals(entityId)
        );
    }

    public int size() {
        return participants.size();
    }
}
