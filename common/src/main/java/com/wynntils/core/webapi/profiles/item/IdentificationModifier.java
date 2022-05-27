/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.webapi.profiles.item;

public enum IdentificationModifier {
    INTEGER(""),
    PERCENTAGE("%"),
    FOUR_SECONDS("/4s"),
    THREE_SECONDS("/3s"),
    TIER(" tier");

    final String inGame;

    IdentificationModifier(String inGame) {
        this.inGame = inGame;
    }

    public String getInGame(String name) {
        if (this != FOUR_SECONDS) return inGame;

        if (name.equals("manaRegen")) return "/5s";
        return "/3s";
    }
}
