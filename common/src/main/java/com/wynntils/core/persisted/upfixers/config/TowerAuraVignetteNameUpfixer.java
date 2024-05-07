/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted.upfixers.config;

import com.wynntils.core.persisted.upfixers.RenamedKeysUpfixer;
import com.wynntils.utils.type.Pair;
import java.util.List;

public class TowerAuraVignetteNameUpfixer extends RenamedKeysUpfixer {
    private static final List<Pair<String, String>> RENAMED_KEYS =
            List.of(Pair.of("towerAuraVignetteFeature.vignetteColor", "towerAuraVignetteFeature.auraVignetteColor"));

    @Override
    protected List<Pair<String, String>> getRenamedKeys() {
        return RENAMED_KEYS;
    }
}
