/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.territorymanagement.widgets.quickfilters;

import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.screens.territorymanagement.TerritoryManagementScreen;
import com.wynntils.services.itemfilter.filters.AnyStatFilters;
import com.wynntils.services.itemfilter.statproviders.territory.TerritoryProductionStatProvider;
import com.wynntils.services.itemfilter.type.StatProviderAndFilterPair;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class TerritoryProducesQuickFilterWidget extends TerritoryQuickFilterWidget {
    private static final List<GuildResource> POSSIBLE_RESOURCES = Arrays.stream(GuildResource.values())
            .filter(GuildResource::isMaterialResource)
            .toList();

    private GuildResource resource = null;

    public TerritoryProducesQuickFilterWidget(int x, int y, int width, int height, TerritoryManagementScreen screen) {
        super(x, y, width, height, screen);
    }

    @Override
    protected void forwardClick() {
        // Cycle between null and the guild resource
        if (resource == null) {
            resource = POSSIBLE_RESOURCES.getFirst();
        } else {
            int index = POSSIBLE_RESOURCES.indexOf(resource) + 1;
            if (index >= POSSIBLE_RESOURCES.size()) {
                resource = null;
            } else {
                resource = POSSIBLE_RESOURCES.get(index);
            }
        }
    }

    @Override
    protected void backwardClick() {
        // Cycle between null and the guild resource
        if (resource == null) {
            resource = POSSIBLE_RESOURCES.getLast();
        } else {
            int index = POSSIBLE_RESOURCES.indexOf(resource) - 1;
            if (index < 0) {
                resource = null;
            } else {
                resource = POSSIBLE_RESOURCES.get(index);
            }
        }
    }

    @Override
    protected void resetClick() {
        resource = null;
    }

    @Override
    protected MutableComponent getFilterName() {
        return Component.literal("Produces: " + (resource == null ? "-" : resource.getName()));
    }

    @Override
    protected CustomColor getFilterColor() {
        return resource == null ? CommonColors.GRAY : CustomColor.fromChatFormatting(resource.getColor());
    }

    @Override
    protected List<StatProviderAndFilterPair> getFilters() {
        if (resource == null) {
            return List.of();
        }

        return List.of(new StatProviderAndFilterPair(
                new TerritoryProductionStatProvider(resource), new AnyStatFilters.AnyIntegerStatFilter()));
    }
}
