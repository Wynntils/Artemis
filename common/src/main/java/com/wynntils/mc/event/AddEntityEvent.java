/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.Event;

public class AddEntityEvent extends Event {
    private final int id;
    private final UUID uuid;
    private final EntityType<?> type;
    private final double x;
    private final double y;
    private final double z;
    private final double xa;
    private final double ya;
    private final double za;
    private final float xRot;
    private final float yRot;
    private final float yHeadRot;
    private final int data;
    private final Entity entity;

    public AddEntityEvent(ClientboundAddEntityPacket packet, Entity entity) {
        this.id = packet.getId();
        this.uuid = packet.getUUID();
        this.type = packet.getType();
        this.x = packet.getX();
        this.y = packet.getY();
        this.z = packet.getZ();
        this.xa = packet.getXa();
        this.ya = packet.getYa();
        this.za = packet.getZa();
        this.xRot = packet.getXRot();
        this.yRot = packet.getYRot();
        this.yHeadRot = packet.getYHeadRot();
        this.data = packet.getData();
        this.entity = entity;
    }

    public int getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public EntityType<?> getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getXa() {
        return xa;
    }

    public double getYa() {
        return ya;
    }

    public double getZa() {
        return za;
    }

    public float getxRot() {
        return xRot;
    }

    public float getyRot() {
        return yRot;
    }

    public float getyHeadRot() {
        return yHeadRot;
    }

    public int getData() {
        return data;
    }

    public Entity getEntity() {
        return entity;
    }
}
