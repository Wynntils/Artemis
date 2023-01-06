/*
 * Copyright © Wynntils 2022, 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.handleditems.properties;

import com.wynntils.utils.CappedValue;

public interface UsesItemPropery extends CountedItemProperty {
    CappedValue getUses();

    default int getCount() {
        CappedValue value = getUses();
        if (value == null) return 0;
        return value.getCurrent();
    }

    @Override
    default boolean hasCount() {
        return getUses() != null;
    }
}
