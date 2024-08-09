/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.activities.event;

import com.wynntils.models.activities.type.ActivityType;
import net.neoforged.bus.api.Event;

public class ActivityUpdatedEvent extends Event {
    private final ActivityType activityType;

    public ActivityUpdatedEvent(ActivityType activityType) {
        this.activityType = activityType;
    }

    public ActivityType getActivityType() {
        return activityType;
    }
}
