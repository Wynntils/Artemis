/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilities.type;

import java.util.List;

public record AbilityTreeSkillNode(
        String name,
        String formattedName,
        List<String> description,
        ItemInformation itemInformation,
        int cost,
        List<String> blocks,
        String requiredAbility,
        ArchetypeRequirement requiredArchetype,
        String archetype,
        AbilityTreeLocation location,
        List<String> connections) {
    public record ArchetypeRequirement(String name, int required) {}

    public record ItemInformation(int itemId, int damage) {}
}
