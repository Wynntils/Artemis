/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.lootruns.type;

public enum LootrunState {
    RECORDING, // Lootrun is being recorded
    LOADED, // Lootrun is loaded and displayed
    DISABLED // No lootrun paths are rendered or loaded
}
