/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gearinfo.types;

import com.wynntils.utils.type.RangedValue;

// The range is actually possible derive from the other values, but are so commonly used
// that we cache it here as well
public record GearStatPossibleValues(GearStat stat, RangedValue range, int baseValue, boolean isPreIdentified) {}
