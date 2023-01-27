/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.stats.type;

public enum AttackType {
    ANY(""),
    MAIN_ATTACK("Main Attack"),
    SPELL("Spell");

    private final String displayName;
    private final String apiName;

    AttackType(String name) {
        // displayName needs padding if non-empty
        this.displayName = name.isEmpty() ? "" : name + " ";
        this.apiName = name.replace(" ", "");
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getApiName() {
        return apiName;
    }
}
