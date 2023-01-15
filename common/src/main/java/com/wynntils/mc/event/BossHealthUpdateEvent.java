/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.wynntils.core.events.WynntilsEvent;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class BossHealthUpdateEvent extends WynntilsEvent {
    private final ClientboundBossEventPacket packet;
    private final Map<UUID, LerpingBossEvent> bossEvents;

    public BossHealthUpdateEvent(ClientboundBossEventPacket packet, Map<UUID, LerpingBossEvent> bossEvents) {
        this.packet = packet;
        this.bossEvents = bossEvents;
    }

    public ClientboundBossEventPacket getPacket() {
        return packet;
    }

    public Map<UUID, LerpingBossEvent> getBossEvents() {
        return bossEvents;
    }
}
