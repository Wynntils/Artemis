/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.territorymanagement.widgets;

import com.wynntils.core.text.StyledText;
import com.wynntils.models.items.items.gui.TerritoryItem;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.screens.territorymanagement.TerritoryManagementHolder;
import com.wynntils.screens.territorymanagement.type.TerritoryColor;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class TerritoryWidget extends AbstractWidget implements TooltipProvider {
    private final TerritoryManagementHolder holder;
    private final TerritoryColor territoryColor;
    private final ItemStack itemStack;
    private final TerritoryItem territoryItem;

    public TerritoryWidget(
            int x,
            int y,
            int width,
            int height,
            TerritoryManagementHolder holder,
            TerritoryColor territoryColor,
            ItemStack itemStack,
            TerritoryItem territoryItem) {
        super(x, y, width, height, Component.literal(territoryItem.getName()));
        this.holder = holder;
        this.territoryColor = territoryColor;
        this.itemStack = itemStack;
        this.territoryItem = territoryItem;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!territoryColor.backgroundColors().isEmpty()) {
            RenderUtils.drawMulticoloredRect(
                    guiGraphics.pose(),
                    territoryColor.backgroundColors(),
                    this.getX(),
                    this.getY(),
                    0,
                    this.getWidth(),
                    this.getHeight());
        }

        if (territoryColor.borderColor() != CustomColor.NONE) {
            RenderUtils.drawRectBorders(
                    guiGraphics.pose(),
                    territoryColor.borderColor(),
                    this.getX() + 0.75f,
                    this.getY() + 0.75f,
                    this.getX() + this.getWidth() - 0.75f,
                    this.getY() + this.getHeight() - 0.75f,
                    0,
                    1.5f);
        }

        guiGraphics.pose().pushPose();

        // Pick the texture based on the item type
        Texture texture = Texture.TERRITORY_ITEM;

        if (territoryItem.isSelected()) {
            texture = Texture.CHECKMARK_GREEN;
        } else if (territoryItem.isHeadquarters()) {
            texture = Texture.TERRITORY_ITEM_HQ;
        } else if (!territoryItem.getAlerts().isEmpty()) {
            texture = Texture.TERRITORY_ITEM_ALERT;
        }

        // Center the item
        int itemWidth = (int) (this.getWidth() * 0.9f);
        int itemHeight = (int) (this.getHeight() * 0.9f);
        int itemRenderX = this.getX() + (this.getWidth() - itemWidth) / 2;
        int itemRenderY = this.getY() + (this.getHeight() - itemHeight) / 2;

        // Render at the center of the widget
        RenderUtils.drawScalingTexturedRect(
                guiGraphics.pose(),
                texture.resource(),
                itemRenderX,
                itemRenderY,
                0,
                itemWidth,
                itemHeight,
                texture.width(),
                texture.height());

        guiGraphics.pose().popPose();

        // Render the territory production type icons
        Set<GuildResource> productionTypes = territoryItem.getProduction().keySet().stream()
                .filter(GuildResource::isMaterialResource)
                .collect(Collectors.toUnmodifiableSet());

        if (!productionTypes.isEmpty()) {
            // Render the production types

            if (productionTypes.size() <= 2) {
                GuildResource productionType = productionTypes.iterator().next();
                String symbol = productionType.getPrettySymbol().trim();
                symbol += productionTypes.size() == 2
                        ? productionTypes.iterator().next().getPrettySymbol().trim()
                        : "";

                FontRenderer.getInstance()
                        .renderAlignedTextInBox(
                                guiGraphics.pose(),
                                StyledText.fromString(symbol),
                                this.getX(),
                                this.getX() + this.getWidth(),
                                this.getY(),
                                this.getY() + this.getHeight(),
                                0,
                                CommonColors.WHITE,
                                HorizontalAlignment.LEFT,
                                VerticalAlignment.TOP,
                                TextShadow.NORMAL);
            } else {
                // Render the production types in two lines, 2 icons per line
                int i = 0;
                for (GuildResource productionType : productionTypes) {
                    String symbol = productionType.getPrettySymbol().trim();
                    FontRenderer.getInstance()
                            .renderText(
                                    guiGraphics.pose(),
                                    StyledText.fromString(symbol),
                                    this.getX() + i % 2 * 8,
                                    this.getY() + i / 2 * 8,
                                    CommonColors.WHITE,
                                    HorizontalAlignment.LEFT,
                                    VerticalAlignment.TOP,
                                    TextShadow.NORMAL);
                    i++;
                }
            }

            // Render the territory name
            String shortTerritoryName = Arrays.stream(territoryItem.getName().split(" "))
                    .map(s -> s.substring(0, 1))
                    .collect(Collectors.joining());

            int maxTextWidth = (int) (this.getWidth() * 0.85f);
            int textWidth = FontRenderer.getInstance().getFont().width(shortTerritoryName);

            float textScale = Math.min(0.95f, maxTextWidth / (float) textWidth);

            FontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            guiGraphics.pose(),
                            StyledText.fromString(shortTerritoryName),
                            this.getX(),
                            this.getX() + this.getWidth(),
                            this.getY(),
                            this.getY() + this.getHeight(),
                            0,
                            CommonColors.ORANGE,
                            HorizontalAlignment.RIGHT,
                            VerticalAlignment.BOTTOM,
                            TextShadow.OUTLINE,
                            textScale);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        holder.territoryItemClicked(territoryItem);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    @Override
    public List<Component> getTooltipLines() {
        return itemStack.getTooltipLines(
                Item.TooltipContext.of(McUtils.mc().level), McUtils.player(), TooltipFlag.NORMAL);
    }
}
