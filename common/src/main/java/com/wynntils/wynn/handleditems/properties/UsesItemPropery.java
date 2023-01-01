/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.handleditems.properties;

import com.wynntils.utils.CappedValue;

public interface UsesItemPropery extends CountedItemProperty {
    CappedValue getUses();

    default int getCount() {
        return getUses().getCurrent();
    }
}
