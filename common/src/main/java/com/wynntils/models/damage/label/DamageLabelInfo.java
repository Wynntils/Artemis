/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.damage.label;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.labels.type.LabelInfo;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.utils.mc.type.Location;
import java.util.Map;
import net.minecraft.world.entity.Entity;

public class DamageLabelInfo extends LabelInfo {
    private final Map<DamageType, Long> damages;

    public DamageLabelInfo(StyledText label, Location location, Entity entity, Map<DamageType, Long> damages) {
        super(label, location, entity);
        this.damages = damages;
    }

    public Map<DamageType, Long> getDamages() {
        return damages;
    }
}
