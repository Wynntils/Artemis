/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.activities.caves;

import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.type.ActivityDifficulty;
import com.wynntils.models.activities.type.ActivityDistance;
import com.wynntils.models.activities.type.ActivityLength;
import com.wynntils.models.activities.type.ActivityStatus;
import com.wynntils.utils.mc.StyledTextUtils;
import com.wynntils.utils.mc.type.Location;
import java.util.List;
import java.util.Optional;

public record CaveInfo(
        String name,
        ActivityStatus status,
        String description,
        int recommendedLevel,
        ActivityDistance distance,
        ActivityLength length,
        ActivityDifficulty difficulty,
        List<String> rewards) {
    public Optional<Location> getNextLocation() {
        return StyledTextUtils.extractLocation(StyledText.fromString(description));
    }

    public boolean isTrackable() {
        return status == ActivityStatus.AVAILABLE || status == ActivityStatus.STARTED;
    }
}
