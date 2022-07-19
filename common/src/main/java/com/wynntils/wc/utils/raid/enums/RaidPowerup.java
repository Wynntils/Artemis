/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.utils.raid.enums;

public enum RaidPowerup {
    // Grootslang
    SPELLCASTER,
    SKILL_MASTER,
    BESERK,
    RACER,
    VAMPIRE,

    // The Canyon Colossus
    MONK,
    STONEWALKER,
    GIANT,
    INTREPID,
    PESTILENT,

    // Nexus of Light
    CHERUBIM,
    SERAPHIM,
    OPHANIM,
    THRONE,
    ANTI;

    private int level;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
