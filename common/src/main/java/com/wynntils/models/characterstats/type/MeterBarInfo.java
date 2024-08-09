/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.type;

import com.wynntils.utils.type.CappedValue;

public record MeterBarInfo(MeterActionType type, CappedValue value) {
    public static final MeterBarInfo EMPTY = new MeterBarInfo(MeterActionType.BOTH, CappedValue.EMPTY);

    public enum MeterActionType {
        SPRINT,
        BREATH,
        BOTH
    }
}
