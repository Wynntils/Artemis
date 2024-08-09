/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.players.event;

import com.wynntils.hades.protocol.enums.PacketAction;
import java.util.Set;
import net.neoforged.bus.api.Event;

public abstract class HadesRelationsUpdateEvent extends Event {
    private final Set<String> changedPlayers;
    private final ChangeType changeType;

    protected HadesRelationsUpdateEvent(Set<String> changedPlayers, ChangeType changeType) {
        this.changedPlayers = changedPlayers;
        this.changeType = changeType;
    }

    public Set<String> getChangedPlayers() {
        return changedPlayers;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public static class FriendList extends HadesRelationsUpdateEvent {
        public FriendList(Set<String> changedPlayers, ChangeType changeType) {
            super(changedPlayers, changeType);
        }
    }

    public static class PartyList extends HadesRelationsUpdateEvent {
        public PartyList(Set<String> changedPlayers, ChangeType changeType) {
            super(changedPlayers, changeType);
        }
    }

    public enum ChangeType {
        ADD(PacketAction.ADD),
        REMOVE(PacketAction.REMOVE),
        RELOAD(PacketAction.RESET); // This is used to indicate that we have a new fully parsed relations list

        private final PacketAction packetAction;

        ChangeType(PacketAction packetAction) {
            this.packetAction = packetAction;
        }

        public PacketAction getPacketAction() {
            return packetAction;
        }
    }
}
