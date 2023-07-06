/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilitytree;

import com.google.common.collect.ImmutableMap;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.container.ScriptedContainerQuery;
import com.wynntils.handlers.container.type.ContainerContent;
import com.wynntils.models.abilitytree.parser.UnprocessedAbilityTreeInfo;
import com.wynntils.models.abilitytree.type.AbilityTreeInfo;
import com.wynntils.models.abilitytree.type.AbilityTreeNodeState;
import com.wynntils.models.abilitytree.type.AbilityTreeSkillNode;
import com.wynntils.models.abilitytree.type.ParsedAbilityTree;
import com.wynntils.screens.abilities.CustomAbilityTreeScreen;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.wynn.InventoryUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AbilityTreeContainerQueries {
    private static final int ABILITY_TREE_SLOT = 9;
    private static final int PREVIOUS_PAGE_SLOT = 57;
    private static final int NEXT_PAGE_SLOT = 59;
    private static final int DUMMY_SLOT = 76; // This is the second archetype icon
    private static final StyledText NEXT_PAGE_ITEM_NAME = StyledText.fromString("§7Next Page");

    public void dumpAbilityTree(Consumer<AbilityTreeInfo> supplier) {
        queryAbilityTree(new AbilityTreeContainerQueries.AbilityPageDumper(supplier));
    }

    public void updateParsedAbilityTree() {
        McUtils.player().closeContainer();

        // Wait for the container to close
        Managers.TickScheduler.scheduleNextTick(() -> queryAbilityTree(
                new AbilityTreeContainerQueries.AbilityPageSoftProcessor(Models.AbilityTree::setCurrentAbilityTree)));
    }

    private void queryAbilityTree(AbilityTreeProcessor processor) {
        ScriptedContainerQuery.QueryBuilder queryBuilder = ScriptedContainerQuery.builder("Ability Tree Dump Query")
                .onError(msg -> {
                    WynntilsMod.warn("Problem querying Ability Tree: " + msg);
                    McUtils.sendMessageToClient(
                            Component.literal("Error dumping ability tree.").withStyle(ChatFormatting.RED));

                    if (McUtils.mc().screen instanceof CustomAbilityTreeScreen abilityTreeScreen) {
                        abilityTreeScreen.setTreeParseState(CustomAbilityTreeScreen.TreeParseState.FAILED);
                    }
                })
                .useItemInHotbar(InventoryUtils.COMPASS_SLOT_NUM)
                .matchTitle("Character Info")
                .processContainer(container -> {})
                .setWaitForMenuReopen(false)
                .clickOnSlot(ABILITY_TREE_SLOT)
                .matchTitle(Models.Container.ABILITY_TREE_PATTERN.pattern())
                .processContainer(c -> {});

        // region Hack for going back to first page without knowing our current page

        for (int i = Models.AbilityTree.ABILITY_TREE_PAGES - 1; i > 0; i--) {
            queryBuilder
                    .clickOnSlotIfExists(PREVIOUS_PAGE_SLOT, DUMMY_SLOT)
                    .matchTitle(Models.Container.ABILITY_TREE_PATTERN.pattern())
                    .processContainer(c -> {});
        }

        // endregion

        // We are on the first page now
        queryBuilder
                .clickOnSlot(DUMMY_SLOT)
                .matchTitle(Models.Container.ABILITY_TREE_PATTERN.pattern())
                .processContainer(c -> processor.processPage(c, 1));

        // region Hack for setWaitForMenuReopen to work with page 2
        // Going from first page to second adds a new button, "previous". This triggers processContainer prematurely.

        queryBuilder
                .clickOnSlotWithName(NEXT_PAGE_SLOT, Items.STONE_AXE, NEXT_PAGE_ITEM_NAME)
                .matchTitle(Models.Container.ABILITY_TREE_PATTERN.pattern())
                .processContainer(c -> {});
        queryBuilder
                .clickOnSlot(DUMMY_SLOT)
                .matchTitle(Models.Container.ABILITY_TREE_PATTERN.pattern())
                .processContainer(c -> processor.processPage(c, 2));

        // endregion

        for (int i = 3; i <= Models.AbilityTree.ABILITY_TREE_PAGES; i++) {
            final int page = i; // Lambdas need final variables
            queryBuilder
                    .clickOnSlotWithName(NEXT_PAGE_SLOT, Items.STONE_AXE, NEXT_PAGE_ITEM_NAME)
                    .matchTitle(Models.Container.ABILITY_TREE_PATTERN.pattern())
                    .processContainer(c -> processor.processPage(c, page));
        }

        queryBuilder.build().executeQuery();
    }

    @FunctionalInterface
    public interface AbilityTreeProcessor {
        void processPage(ContainerContent content, int page);
    }

    /**
     * Parses the whole ability tree and saves it to disk.
     */
    public static class AbilityPageDumper implements AbilityTreeProcessor {
        private final Consumer<AbilityTreeInfo> supplier;
        private final UnprocessedAbilityTreeInfo unprocessedTree = new UnprocessedAbilityTreeInfo();

        protected AbilityPageDumper(Consumer<AbilityTreeInfo> supplier) {
            this.supplier = supplier;
        }

        @Override
        public void processPage(ContainerContent content, int page) {
            List<ItemStack> items = content.items();

            for (int slot = 0; slot < items.size(); slot++) {
                ItemStack itemStack = items.get(slot);

                unprocessedTree.processItem(itemStack, page, slot, true);
            }

            if (page == Models.AbilityTree.ABILITY_TREE_PAGES) {
                this.supplier.accept(unprocessedTree.getProcesssed());
            }
        }
    }

    /**
     * Only parses nodes of an ability tree, and stores it.
     */
    public static class AbilityPageSoftProcessor implements AbilityTreeProcessor {
        private final Map<AbilityTreeSkillNode, AbilityTreeNodeState> collectedInfo = new LinkedHashMap<>();
        private final Consumer<ParsedAbilityTree> callback;

        protected AbilityPageSoftProcessor(Consumer<ParsedAbilityTree> callback) {
            this.callback = callback;
        }

        @Override
        public void processPage(ContainerContent content, int page) {
            List<ItemStack> items = content.items();

            for (int slot = 0; slot < items.size(); slot++) {
                ItemStack itemStack = items.get(slot);
                if (!Models.AbilityTree.ABILITY_TREE_PARSER.isNodeItem(itemStack, slot)) continue;

                Pair<AbilityTreeSkillNode, AbilityTreeNodeState> parsedNode =
                        Models.AbilityTree.ABILITY_TREE_PARSER.parseNodeFromItem(
                                itemStack, page, slot, collectedInfo.size() + 1);

                collectedInfo.put(parsedNode.key(), parsedNode.value());
            }

            boolean lastPage = page == Models.AbilityTree.ABILITY_TREE_PAGES;

            if (lastPage) {
                callback.accept(new ParsedAbilityTree(ImmutableMap.copyOf(collectedInfo)));
            }
        }
    }
}
