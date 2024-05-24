/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.containers.containers.crafting;

import com.wynntils.models.profession.type.ProfessionType;
import java.util.regex.Pattern;

public class WeaponsmithingStationContainer extends CraftingStationContainer {
    private static final Pattern TITLE_PATTERN = Pattern.compile(ProfessionType.WEAPONSMITHING.getDisplayName());

    public WeaponsmithingStationContainer() {
        super(TITLE_PATTERN, ProfessionType.WEAPONSMITHING);
    }
}
