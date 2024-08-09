/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.raid.type;

import com.wynntils.core.text.StyledText;
import java.util.regex.Pattern;

public enum RaidKind {
    NEST_OF_THE_GROOTSLANGS(
            "Nest of the Grootslangs",
            StyledText.fromString("§2Nest of The Grootslangs"),
            Pattern.compile("^Slay the Restless$")),
    ORPHIONS_NEXUS_OF_LIGHT(
            "Orphion's Nexus of Light",
            StyledText.fromString("§f§kOrphion's Nexus of §lLight"),
            Pattern.compile("^Save him.$")),
    THE_CANYON_COLOSSUS(
            "The Canyon Colossus",
            StyledText.fromString("§#5f968bffThe Canyon Colossus"),
            Pattern.compile("^Unknown$")), // FIXME: When this is fixed on Wynn, change this
    THE_NAMELESS_ANOMALY(
            "The Nameless Anomaly",
            StyledText.fromString("§9§lThe §1§k§lNameless§9§l Anomaly"),
            Pattern.compile("^Survive.$"));

    private final String name;
    private final StyledText entryTitle;
    private final Pattern bossScoreboardPattern;

    RaidKind(String name, StyledText entryTitle, Pattern bossScoreboardPattern) {
        this.name = name;
        this.entryTitle = entryTitle;
        this.bossScoreboardPattern = bossScoreboardPattern;
    }

    public static RaidKind fromTitle(StyledText title) {
        for (RaidKind raidKind : RaidKind.values()) {
            if (raidKind.getEntryTitle().equals(title)) {
                return raidKind;
            }
        }

        return null;
    }

    public static RaidKind fromName(String name) {
        for (RaidKind raidKind : RaidKind.values()) {
            if (raidKind.getName().equalsIgnoreCase(name)) {
                return raidKind;
            }
        }

        return null;
    }

    public String getName() {
        return name;
    }

    public StyledText getEntryTitle() {
        return entryTitle;
    }

    public Pattern getBossScoreboardPattern() {
        return bossScoreboardPattern;
    }
}
