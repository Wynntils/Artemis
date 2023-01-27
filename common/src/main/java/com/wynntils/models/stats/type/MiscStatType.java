/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.stats.type;

import com.wynntils.models.stats.builders.MiscStatKind;

public final class MiscStatType extends StatType {
    private final MiscStatKind kind;

    public MiscStatType(
            String key, String displayName, String apiName, String loreName, StatUnit unit, MiscStatKind kind) {
        super(key, displayName, apiName, loreName, unit);
        this.kind = kind;
    }

    public MiscStatKind getKind() {
        return kind;
    }
}
