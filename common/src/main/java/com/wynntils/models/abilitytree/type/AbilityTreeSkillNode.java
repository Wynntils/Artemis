/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilitytree.type;

import com.wynntils.core.text.StyledText;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public record AbilityTreeSkillNode(
        int id,
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
        List<Integer> connections) {

    public List<Component> getDescription(AbilityTreeNodeState state, ParsedAbilityTree abilityTree) {
        List<Component> description = description().stream()
                .map(StyledText::fromString)
                .map(StyledText::getComponent)
                .collect(Collectors.toList());

        description.add(Component.empty());

        if (state == AbilityTreeNodeState.BLOCKED) {
            List<AbilityTreeSkillNode> blockedByNodes = abilityTree.nodes().keySet().stream()
                    .filter(node -> abilityTree.getNodeState(node) == AbilityTreeNodeState.UNLOCKED)
                    .filter(node -> node.blocks().contains(this.name()))
                    .toList();

            description.add(Component.translatable("screens.wynntils.abilityTree.nodeState.blockedBy")
                    .withStyle(ChatFormatting.RED)
                    .withStyle(ChatFormatting.BOLD));

            description.addAll(blockedByNodes.stream()
                    .map(AbilityTreeSkillNode::name)
                    .map(nodeName -> Component.literal("- ")
                            .withStyle(ChatFormatting.RED)
                            .append(Component.literal(nodeName)))
                    .toList());

            description.add(Component.empty());
        }

        description.add(state.getComponent());

        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbilityTreeSkillNode that = (AbilityTreeSkillNode) o;
        return id == that.id && Objects.equals(name, that.name) && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, location);
    }
}
