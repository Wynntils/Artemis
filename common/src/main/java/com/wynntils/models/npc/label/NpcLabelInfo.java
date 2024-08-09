/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.npc.label;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.labels.type.LabelInfo;
import com.wynntils.utils.mc.type.Location;
import net.minecraft.world.entity.Entity;

public class NpcLabelInfo extends LabelInfo {
    private final String icon;
    private final String description;

    public NpcLabelInfo(StyledText label, String name, Location location, Entity entity) {
        super(label, name, location, entity);
        this.icon = null;
        this.description = null;
    }

    public NpcLabelInfo(
            StyledText label, String name, Location location, Entity entity, String icon, String description) {
        super(label, name, location, entity);
        this.icon = icon;
        this.description = description;
    }

    @Override
    public String toString() {
        return "NpcLabelInfo{" + "icon='"
                + icon + '\'' + ", description='"
                + description + '\'' + ", label="
                + label + ", name='"
                + name + '\'' + ", location="
                + location + ", entity="
                + entity + '}';
    }
}
