/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.base.widgets;

import com.wynntils.core.components.Services;
import com.wynntils.services.itemfilter.type.ItemProviderType;
import com.wynntils.services.itemfilter.type.ItemStatProvider;
import com.wynntils.services.itemfilter.type.StatFilter;
import com.wynntils.services.itemfilter.type.StatFilterFactory;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.render.Texture;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ItemSearchHelperWidget extends BasicTexturedButton {
    private static final int ELEMENTS_PER_PAGE = 4;

    private final List<ItemProviderType> supportedProviderTypes;
    private final List<List<Component>> tooltipPages = new ArrayList<>();

    private int page = 0;

    public ItemSearchHelperWidget(
            int x,
            int y,
            int width,
            int height,
            Texture texture,
            boolean scaleTexture,
            List<ItemProviderType> supportedProviderTypes) {
        super(x, y, width, height, texture, (b) -> {}, List.of(), scaleTexture);
        this.supportedProviderTypes = supportedProviderTypes;

        generateTooltipPages();
    }

    @Override
    public List<Component> getTooltipLines() {
        return tooltipPages.get(page);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            page = MathUtils.overflowInRange(page, -1, 0, tooltipPages.size() - 1);
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            page = MathUtils.overflowInRange(page, +1, 0, tooltipPages.size() - 1);
            return true;
        }

        return false;
    }

    private void generateTooltipPages() {
        List<Component> currentTooltip = new ArrayList<>();
        int counter = 0;

        // Filters
        int filterPageCount = 0;
        List<? extends StatFilterFactory<? extends StatFilter<?>>> statFilters = Services.ItemFilter.getStatFilters();
        for (StatFilterFactory<? extends StatFilter<?>> statFilter : statFilters) {
            currentTooltip.add(Component.empty());
            currentTooltip.add(
                    Component.literal(statFilter.getTranslatedName() + ": ").withStyle(ChatFormatting.YELLOW));
            currentTooltip.add(
                    Component.translatable(statFilter.getDescription()).withStyle(ChatFormatting.GRAY));
            currentTooltip.add(Component.translatable(statFilter.getUsage()).withStyle(ChatFormatting.GRAY));

            counter++;

            if (counter == ELEMENTS_PER_PAGE) {
                tooltipPages.add(currentTooltip);
                currentTooltip = new ArrayList<>();
                counter = 0;
                filterPageCount++;
            }
        }

        if (counter != 0) {
            tooltipPages.add(currentTooltip);
            currentTooltip = new ArrayList<>();
            filterPageCount++;
        }

        // Stats
        counter = 0;
        List<ItemStatProvider<?>> itemStatProviders = Services.ItemFilter.getItemStatProviders().stream()
                .filter(itemStatProvider ->
                        itemStatProvider.getFilterTypes().stream().anyMatch(supportedProviderTypes::contains))
                .toList();
        for (ItemStatProvider<?> itemStatProvider : itemStatProviders) {
            currentTooltip.add(Component.empty());
            currentTooltip.add(Component.literal(itemStatProvider.getName() + ": ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal("(" + itemStatProvider.getType().getSimpleName() + ")")
                            .withStyle(ChatFormatting.GRAY)));
            currentTooltip.add(
                    Component.translatable(itemStatProvider.getDescription()).withStyle(ChatFormatting.GRAY));

            counter++;

            if (counter == ELEMENTS_PER_PAGE) {
                tooltipPages.add(currentTooltip);
                currentTooltip = new ArrayList<>();
                counter = 0;
            }
        }

        if (counter != 0) {
            tooltipPages.add(currentTooltip);
        }

        for (int i = 0; i < tooltipPages.size(); i++) {
            List<Component> tooltipPage = tooltipPages.get(i);

            // Header
            if (i < filterPageCount) {
                tooltipPage.addFirst(
                        Component.translatable("screens.wynntils.itemSearchHelperWidget.availableFilters"));
            } else {
                tooltipPage.addFirst(Component.translatable("screens.wynntils.itemSearchHelperWidget.availableStats"));
            }

            // Footer
            tooltipPage.add(Component.empty());
            tooltipPage.add(
                    Component.translatable("screens.wynntils.itemSearchHelperWidget.page", i + 1, tooltipPages.size())
                            .withStyle(ChatFormatting.GRAY)
                            .withStyle(ChatFormatting.BOLD));
            tooltipPage.add(Component.literal("(")
                    .append(Component.translatable("screens.wynntils.itemSearchHelperWidget.switchPage")
                            .append(")"))
                    .withStyle(ChatFormatting.GRAY));
        }

        // Wrap all tooltips pages for a maximum width
        tooltipPages.replaceAll((tooltipPage) -> ComponentUtils.wrapTooltips(tooltipPage, TOOLTIP_WIDTH));
    }
}
