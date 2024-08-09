/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.activities.worldevents;

import com.wynntils.core.components.Model;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.type.ActivityDifficulty;
import com.wynntils.models.activities.type.ActivityDistance;
import com.wynntils.models.activities.type.ActivityInfo;
import com.wynntils.models.activities.type.ActivityLength;
import java.util.List;

public class WorldEventModel extends Model {
    public WorldEventModel() {
        super(List.of());
    }

    private WorldEventInfo getCaveInfoFromActivity(ActivityInfo activity) {
        return new WorldEventInfo(
                activity.name(),
                activity.specialInfo().orElse(""),
                activity.description().orElse(StyledText.EMPTY).getString(),
                activity.status(),
                activity.requirements().level().key(),
                activity.distance().orElse(ActivityDistance.NEAR),
                activity.length().orElse(ActivityLength.SHORT),
                activity.difficulty().orElse(ActivityDifficulty.EASY),
                activity.rewards());
    }
}
