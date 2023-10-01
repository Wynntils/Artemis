/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.crowdsource;

import com.wynntils.core.crowdsource.CrowdSourcedDataCollector;
import com.wynntils.core.crowdsource.type.CrowdSourcedDataType;
import com.wynntils.models.label.event.LabelIdentifiedEvent;
import com.wynntils.models.label.infos.GatheringNodeLabelInfo;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ProfessionNodeLocationDataCollector extends CrowdSourcedDataCollector<GatheringNodeLabelInfo> {
    @SubscribeEvent
    public void onLabelIdentified(LabelIdentifiedEvent event) {
        if (event.getLabelInfo() instanceof GatheringNodeLabelInfo gatheringNodeLabelInfo) {
            collect(gatheringNodeLabelInfo);
        }
    }

    @Override
    protected CrowdSourcedDataType getDataType() {
        return CrowdSourcedDataType.PROFESSION_NODE_LOCATIONS;
    }
}
