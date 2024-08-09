/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.labels.event;

import com.wynntils.handlers.labels.type.LabelInfo;
import net.neoforged.bus.api.Event;

public class LabelIdentifiedEvent extends Event {
    private final LabelInfo labelInfo;

    public LabelIdentifiedEvent(LabelInfo labelInfo) {
        this.labelInfo = labelInfo;
    }

    public LabelInfo getLabelInfo() {
        return labelInfo;
    }
}
