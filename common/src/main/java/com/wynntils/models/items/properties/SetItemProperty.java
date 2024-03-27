/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.properties;

import com.wynntils.models.gear.type.SetInfo;
import com.wynntils.models.gear.type.SetInstance;
import java.util.Optional;

public interface SetItemProperty {
    Optional<SetInfo> getSetInfo();

    Optional<SetInstance> getSetInstance();
}
