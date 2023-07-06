/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilitytree.type;

import com.wynntils.core.components.Models;
import com.wynntils.utils.type.Pair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;

public class AbilityTreeConnectionNode {
    private AbilityTreeConnectionType connectionType;

    // {up, down, left, right}
    // There can be multiple suppliers from a side
    private Set<AbilityTreeSkillNode>[] nodes;
    private Set<Pair<AbilityTreeSkillNode, AbilityTreeSkillNode>> nodePairs;
    private boolean[] connections = new boolean[4];

    // This constuctor is used for new connections, so there are only 2 non-null nodes
    public AbilityTreeConnectionNode(AbilityTreeConnectionType connectionType, AbilityTreeSkillNode[] nodes) {
        this.connectionType = connectionType;
        this.nodes = Arrays.stream(nodes).map(Collections::singleton).toArray(Set[]::new);

        List<AbilityTreeSkillNode> nonNullNodes = Arrays.stream(this.nodes)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .toList();
        this.nodePairs = Collections.singleton(new Pair<>(nonNullNodes.get(0), nonNullNodes.get(1)));

        updateConnectedStates();
    }

    // This node is used for merging connections, so there can be multiple nodes in each direction
    private AbilityTreeConnectionNode(
            AbilityTreeConnectionType connectionType,
            Set<AbilityTreeSkillNode>[] nodes,
            Set<Pair<AbilityTreeSkillNode, AbilityTreeSkillNode>> nodePairs) {
        this.connectionType = connectionType;
        this.nodes = nodes;
        this.nodePairs = nodePairs;

        updateConnectedStates();
    }

    private void updateConnectedStates() {
        Optional<ParsedAbilityTree> treeOptional = Models.AbilityTree.getCurrentAbilityTree();

        if (treeOptional.isEmpty()) return;

        ParsedAbilityTree parsedAbilityTree = treeOptional.get();

        List<AbilityTreeSkillNode> activeNodes = nodePairs.stream()
                .filter(pair -> parsedAbilityTree.getNodeState(pair.a()) == AbilityTreeNodeState.UNLOCKED
                        && parsedAbilityTree.getNodeState(pair.b()) == AbilityTreeNodeState.UNLOCKED)
                .flatMap(pair -> Stream.of(pair.a(), pair.b()))
                .toList();

        for (int i = 0; i < nodes.length; i++) {
            connections[i] = false;

            for (AbilityTreeSkillNode nodeInDirection : nodes[i]) {
                if (activeNodes.contains(nodeInDirection)) {
                    connections[i] = true;
                    break;
                }
            }
        }
    }

    public ItemStack getItemStack() {
        // FIXME: Try to do this less times
        updateConnectedStates();
        return connectionType.getItemStack(connections);
    }

    public AbilityTreeConnectionType getConnectionType() {
        return connectionType;
    }

    public AbilityTreeConnectionNode merge(AbilityTreeConnectionNode other) {
        if (other == null) {
            return this;
        }

        AbilityTreeConnectionType newConnectionType;

        if (this.getConnectionType() == other.getConnectionType()) {
            // Connection type is the same, we only need to merge the nodes
            newConnectionType = connectionType;
        } else {
            // Connection type is different, we need to merge the nodes and the connection type
            newConnectionType = AbilityTreeConnectionType.merge(connectionType, other.getConnectionType());
        }

        Set<AbilityTreeSkillNode>[] newNodes = new HashSet[4];

        for (int i = 0; i < nodes.length; i++) {
            Set<AbilityTreeSkillNode> nodes = this.nodes[i];
            newNodes[i] = new HashSet<>(nodes);
        }

        Set<AbilityTreeSkillNode>[] sets = other.nodes;
        for (int i = 0; i < sets.length; i++) {
            Set<AbilityTreeSkillNode> nodes = sets[i];
            newNodes[i].addAll(nodes);
        }

        Set<Pair<AbilityTreeSkillNode, AbilityTreeSkillNode>> newPairs = new HashSet<>(this.nodePairs);
        newPairs.addAll(other.nodePairs);

        // Return a merged instance
        return new AbilityTreeConnectionNode(newConnectionType, newNodes, newPairs);
    }
}
