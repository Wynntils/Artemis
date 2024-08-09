/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.profession.event;

import net.neoforged.bus.api.Event;

public abstract class ProfessionNodeGatheredEvent extends Event {
    public static class LabelShown extends ProfessionNodeGatheredEvent {}
}
