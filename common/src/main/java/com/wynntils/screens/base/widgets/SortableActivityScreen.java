/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.base.widgets;

import com.wynntils.models.activities.type.ActivitySortOrder;

public interface SortableActivityScreen {
    ActivitySortOrder getActivitySortOrder();

    void setActivitySortOrder(ActivitySortOrder newSortOrder);
}
