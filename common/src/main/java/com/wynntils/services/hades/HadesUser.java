/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.hades;

import com.wynntils.hades.protocol.packets.server.HSPacketUpdateMutual;
import com.wynntils.services.hades.type.PlayerRelation;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.type.CappedValue;
import java.util.UUID;

public class HadesUser {
    private final UUID uuid;
    private final String name;

    private PlayerRelation relation;
    private float x, y, z;
    private PoiLocation poiLocation;
    private CappedValue health;
    private CappedValue mana;

    public HadesUser(HSPacketUpdateMutual packet) {
        uuid = packet.getUser();
        name = packet.getName();

        this.updateFromPacket(packet);
    }

    // Dummy constructor for previews
    public HadesUser(String name, CappedValue health, CappedValue mana) {
        this.uuid = UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7"); // Steve
        this.name = name;

        this.x = 0;
        this.y = 0;
        this.z = 0;

        this.poiLocation = new PoiLocation((int) x, (int) y, (int) z);

        this.relation = PlayerRelation.FRIEND;

        this.health = health;
        this.mana = mana;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public PoiLocation getMapLocation() {
        return poiLocation;
    }

    public CappedValue getHealth() {
        return health;
    }

    public CappedValue getMana() {
        return mana;
    }

    public void updateFromPacket(HSPacketUpdateMutual packet) {
        this.x = packet.getX();
        this.y = packet.getY();
        this.z = packet.getZ();
        this.poiLocation = new PoiLocation((int) x, (int) y, (int) z);

        this.health = new CappedValue(packet.getHealth(), packet.getMaxHealth());
        this.mana = new CappedValue(packet.getMana(), packet.getMaxMana());

        this.relation = packet.isPartyMember()
                ? PlayerRelation.PARTY
                : packet.isMutualFriend()
                        ? PlayerRelation.FRIEND
                        : packet.isGuildMember() ? PlayerRelation.GUILD : PlayerRelation.OTHER;
    }

    public CustomColor getRelationColor() {
        return relation != null ? relation.getRelationColor() : CommonColors.WHITE;
    }

    public PlayerRelation getRelation() {
        return relation;
    }
}
