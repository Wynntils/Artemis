/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.character.event;

import net.minecraft.core.Position;
import net.minecraftforge.eventbus.api.Event;

public class CharacterMovedEvent extends Event {
    private final Position position;

    public CharacterMovedEvent(Position position) {
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }
}
