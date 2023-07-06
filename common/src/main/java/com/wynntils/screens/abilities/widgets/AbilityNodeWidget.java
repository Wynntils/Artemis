/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.abilities.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.models.abilitytree.type.AbilityTreeNodeState;
import com.wynntils.models.abilitytree.type.AbilityTreeSkillNode;
import com.wynntils.models.abilitytree.type.ParsedAbilityTree;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import java.util.Optional;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class AbilityNodeWidget extends AbstractWidget {
    public static final int SIZE = 20;

    private final AbilityTreeSkillNode node;

    public AbilityNodeWidget(int x, int y, int width, int height, AbilityTreeSkillNode node) {
        super(x, y, width, height, Component.literal(node.name()));
        this.node = node;
    }

    @Override
    public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        ItemStack itemStack = new ItemStack(Item.byId(node.itemInformation().itemId()));

        Optional<ParsedAbilityTree> treeOptional = Models.AbilityTree.getCurrentAbilityTree();

        if (treeOptional.isEmpty()) return;

        ParsedAbilityTree currentAbilityTree = treeOptional.get();

        AbilityTreeNodeState nodeState = currentAbilityTree.getNodeState(node);
        int damage =
                switch (nodeState) {
                    case UNREACHABLE, REQUIREMENT_NOT_MET -> node.itemInformation()
                            .getLockedDamage();
                    case UNLOCKABLE -> node.itemInformation().getUnlockableDamage();
                    case UNLOCKED -> node.itemInformation().getUnlockedDamage();
                    case BLOCKED -> node.itemInformation().getBlockedDamage();
                };

        itemStack.setDamageValue(damage);
        CompoundTag tag = itemStack.getOrCreateTag();
        tag.putBoolean("Unbreakable", true);

        RenderUtils.renderItem(poseStack, itemStack, this.getX(), this.getY());

        if (isMouseOver(mouseX, mouseY)) {
            RenderUtils.drawTooltipAt(
                    poseStack,
                    mouseX,
                    mouseY,
                    0,
                    node.getDescription(nodeState, currentAbilityTree),
                    FontRenderer.getInstance().getFont(),
                    false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    public AbilityTreeSkillNode getNode() {
        return node;
    }
}
