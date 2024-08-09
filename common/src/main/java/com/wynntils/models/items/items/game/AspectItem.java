/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.items.game;

import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.properties.GearTierItemProperty;

public class AspectItem extends GameItem implements GearTierItemProperty {
    private final ClassType classType;
    private final GearTier gearTier;
    private final int aspectTier;

    public AspectItem(ClassType classType, GearTier gearTier, int aspectTier) {
        this.classType = classType;
        this.gearTier = gearTier;
        this.aspectTier = aspectTier;
    }

    public ClassType getClassType() {
        return classType;
    }

    @Override
    public GearTier getGearTier() {
        return gearTier;
    }

    public int getAspectTier() {
        return aspectTier;
    }

    @Override
    public String toString() {
        return "AspectItem{" + "classType=" + classType + ", gearTier=" + gearTier + ", aspectTier=" + aspectTier + '}';
    }
}
