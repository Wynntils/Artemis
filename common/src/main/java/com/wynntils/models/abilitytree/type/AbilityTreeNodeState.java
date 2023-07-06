/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilitytree.type;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum AbilityTreeNodeState {
    UNREACHABLE(Component.translatable("screens.wynntils.abilityTree.nodeState.unreachable")
            .withStyle(ChatFormatting.RED)),
    REQUIREMENT_NOT_MET(Component.translatable("screens.wynntils.abilityTree.nodeState.requirementNotMet")
            .withStyle(ChatFormatting.RED)),
    UNLOCKABLE(Component.translatable("screens.wynntils.abilityTree.nodeState.unlockable")
            .withStyle(ChatFormatting.GREEN)),
    UNLOCKED(Component.translatable("screens.wynntils.abilityTree.nodeState.unlocked")
            .withStyle(ChatFormatting.YELLOW)),
    BLOCKED(Component.translatable("screens.wynntils.abilityTree.nodeState.blocked")
            .withStyle(ChatFormatting.DARK_RED));

    private final MutableComponent component;

    AbilityTreeNodeState(MutableComponent component) {
        this.component = component;
    }

    public MutableComponent getComponent() {
        return component;
    }
}
