/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.containers.containers.crafting;

import com.wynntils.models.profession.type.ProfessionType;
import java.util.regex.Pattern;

public class TailoringStationContainer extends CraftingStationContainer {
    private static final Pattern TITLE_PATTERN = Pattern.compile(ProfessionType.TAILORING.getDisplayName());

    public TailoringStationContainer() {
        super(TITLE_PATTERN, ProfessionType.TAILORING);
    }
}
