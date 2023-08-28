/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.telemetry.type;

/**
 * This enum represents the game version that the data is being sent from.
 * Not every version may have a version, and versions may be skipped.
 * This enum is only updated whenever relevant changes are made to the game.
 * We may also change this enum if the mod received relevant data collection changes.
 */
public enum CrowdSourceDataGameVersion {
    VERSION_203_HOTFIX_4("2.0.3 Hotfix 4");

    private final String readableVersion;

    CrowdSourceDataGameVersion(String readableVersion) {
        this.readableVersion = readableVersion;
    }

    public String getReadableVersion() {
        return readableVersion;
    }
}
