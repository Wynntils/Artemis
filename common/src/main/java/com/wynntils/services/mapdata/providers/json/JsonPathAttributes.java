/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.json;

import com.wynntils.services.mapdata.attributes.type.MapPathAttributes;
import java.util.Optional;

public class JsonPathAttributes implements MapPathAttributes {
    private final String label;
    private final int priority;
    private final int level;

    public JsonPathAttributes(String label, int priority, int level) {
        this.label = label;
        this.priority = priority;
        this.level = level;
    }

    @Override
    public Optional<String> getLabel() {
        return Optional.of(label);
    }

    @Override
    public Optional<Integer> getPriority() {
        return Optional.of(priority);
    }

    @Override
    public Optional<Integer> getLevel() {
        return Optional.of(level);
    }
}
