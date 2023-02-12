/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.ingredientinfo;

import java.util.Locale;

public enum IngredientPosition {
    LEFT("to the left of"),
    RIGHT("to the right of"),
    ABOVE("above"),
    UNDER("under"),
    TOUCHING("touching"),
    NOT_TOUCHING("not touching", "notTouching");

    private final String displayName;
    private final String apiName;

    IngredientPosition(String displayName) {
        this.displayName = displayName;
        this.apiName = this.name().toLowerCase(Locale.ROOT);
    }

    IngredientPosition(String displayName, String apiName) {
        this.displayName = displayName;
        this.apiName = apiName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getApiName() {
        return apiName;
    }
}
