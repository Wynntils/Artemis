/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.stats.builders;

import com.wynntils.models.concepts.Element;
import com.wynntils.models.stats.type.DefenceStatType;
import com.wynntils.models.stats.type.StatUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class DefenceStatBuilder extends StatBuilder<DefenceStatType> {
    public static List<DefenceStatType> createStats() {
        List<DefenceStatType> statList = new ArrayList<>();

        DefenceStatBuilder builder = new DefenceStatBuilder();
        builder.buildStats(statList::add);
        return statList;
    }

    @Override
    public void buildStats(Consumer<DefenceStatType> callback) {
        for (Element element : Element.values()) {
            // The difference in spelling (defence/defense) is due to Wynncraft. Do not change.
            DefenceStatType statType = new DefenceStatType(
                    "DEFENCE_" + element.name(),
                    element.getDisplayName() + " Defence",
                    "bonus" + element.getDisplayName() + "Defense",
                    element.name() + "DEFENSE",
                    StatUnit.PERCENT);
            callback.accept(statType);
        }
    }
}
