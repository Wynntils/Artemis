/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.type;

import com.wynntils.models.elements.type.Powder;

public record PowderSpecialInfo(float charge, Powder powder) {
    public static final PowderSpecialInfo EMPTY = new PowderSpecialInfo(0, Powder.EARTH);
}
