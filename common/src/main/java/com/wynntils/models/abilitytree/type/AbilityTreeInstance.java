/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilitytree.type;

import com.google.common.collect.ImmutableMap;

/**
 * This class represents the current ability tree, where all nodes have a state.
 */
public record AbilityTreeInstance(ImmutableMap<AbilityTreeSkillNode, AbilityTreeNodeState> nodes) {
    public AbilityTreeNodeState getNodeState(AbilityTreeSkillNode node) {
        return nodes().keySet().stream()
                .filter(n -> n.equals(node))
                .map(nodes()::get)
                .findFirst()
                .orElse(AbilityTreeNodeState.UNREACHABLE);
    }
}
