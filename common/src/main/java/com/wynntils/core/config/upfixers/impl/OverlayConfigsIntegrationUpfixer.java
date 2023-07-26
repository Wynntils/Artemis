/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.config.upfixers.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.config.upfixers.RenamedKeysUpfixer;
import com.wynntils.utils.type.Pair;
import java.util.List;
import java.util.Set;

public class OverlayConfigsIntegrationUpfixer extends RenamedKeysUpfixer {
    private static final List<Pair<String, String>> RENAMED_KEYS = List.of(
            Pair.of(
                    "contentTrackerOverlayFeature.disableTrackerOnScoreboard",
                    "contentTrackerOverlayFeature.contentTrackerOverlay.disableTrackerOnScoreboard"),
            Pair.of(
                    "objectivesOverlayFeature.disableObjectiveTrackingOnScoreboard",
                    "objectivesOverlayFeature.dailyObjectiveOverlay.disableObjectiveTrackingOnScoreboard"),
            Pair.of(
                    "partyMembersOverlayFeature.disablePartyMembersOnScoreboard",
                    "partyMembersOverlayFeature.partyMembersOverlay.disablePartyMembersOnScoreboard"));

    @Override
    protected List<Pair<String, String>> getRenamedKeys() {
        return RENAMED_KEYS;
    }

    @Override
    public boolean apply(JsonObject configObject, Set<ConfigHolder> configHolders) {
        // Special handling of config that split in two
        String oldName = "objectivesOverlayFeature.disableObjectiveTrackingOnScoreboard";
        if (configObject.has(oldName)) {
            JsonElement jsonElement = configObject.get(oldName);
            String newName = "objectivesOverlayFeature.guildObjectiveOverlay.disableObjectiveTrackingOnScoreboard";
            configObject.add(newName, jsonElement);
        }

        return super.apply(configObject, configHolders);
    }
}
