package com.wynntils.models.items.properties;

public interface RequirementItemProperty {
    /**
     * @return true if the item's lore has no X's in it, for the requirements section
     */
    boolean meetsActualRequirements();
}
