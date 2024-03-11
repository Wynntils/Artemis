/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.consumers.overlays;

import com.wynntils.core.persisted.config.Config;

public interface CustomNameProperty {
    Config<String> getCustomName();
}
