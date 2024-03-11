/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.itemsharing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.screens.WynntilsScreen;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.items.FakeItemStack;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.encoding.type.EncodingSettings;
import com.wynntils.models.items.items.game.CraftedConsumableItem;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.items.properties.IdentifiableItemProperty;
import com.wynntils.screens.base.widgets.WynntilsCheckbox;
import com.wynntils.utils.EncodedByteBuffer;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.ErrorOr;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ItemSharingScreen extends WynntilsScreen {
    private final ItemStack itemStack;
    private final WynnItem wynnItem;
    private final Screen previousScreen;

    private boolean savedItem = false;
    private Button saveButton;
    private EncodedByteBuffer encodedItem;
    private int backgroundX;
    private int backgroundY;
    private int tooltipX;
    private ItemStack previewItemStack;
    private List<AbstractWidget> options = new ArrayList<>();

    private ItemSharingScreen(WynnItem wynnItem, ItemStack itemStack) {
        super(Component.literal("Item Sharing Screen"));

        this.wynnItem = wynnItem;
        this.itemStack = itemStack;
        this.previousScreen = McUtils.mc().screen;
    }

    private ItemSharingScreen(WynnItem wynnItem, ItemStack itemStack, boolean savedItem) {
        super(Component.literal("Item Sharing Screen"));

        this.wynnItem = wynnItem;
        this.itemStack = itemStack;
        this.savedItem = savedItem;
        this.previousScreen = McUtils.mc().screen;
    }

    // Creating screen from an item
    public static Screen create(WynnItem wynnItem, ItemStack itemStack) {
        return new ItemSharingScreen(wynnItem, itemStack);
    }

    // Creating screen from item record
    public static Screen create(WynnItem wynnItem, ItemStack itemStack, boolean savedItem) {
        return new ItemSharingScreen(wynnItem, itemStack, savedItem);
    }

    @Override
    protected void doInit() {
        super.doInit();
        backgroundY = (this.height - Texture.ITEM_SHARING_BACKGROUND.height()) / 2;
        refreshPreview();
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.doRender(guiGraphics, mouseX, mouseY, partialTick);
        PoseStack poseStack = guiGraphics.pose();

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromComponent(Component.translatable("screens.wynntils.itemSharing.sharingOptions")
                                .withStyle(ChatFormatting.BOLD)),
                        backgroundX + 10,
                        backgroundY + 10,
                        CommonColors.BLACK,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NONE);

        renderPreview(guiGraphics);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        RenderUtils.drawTexturedRect(guiGraphics.pose(), Texture.ITEM_SHARING_BACKGROUND, backgroundX, backgroundY);
    }

    @Override
    public void onClose() {
        McUtils.mc().setScreen(previousScreen);
    }

    private void renderPreview(GuiGraphics guiGraphics) {
        if (previewItemStack == null) return;

        guiGraphics.renderTooltip(
                FontRenderer.getInstance().getFont(),
                previewItemStack,
                tooltipX,
                backgroundY + (FontRenderer.getInstance().getFont().lineHeight * 2));
    }

    private void refreshPreview() {
        // Encode the item with the selected settings
        EncodingSettings encodingSettings = new EncodingSettings(
                Models.ItemEncoding.extendedIdentificationEncoding.get(), Models.ItemEncoding.shareItemName.get());
        ErrorOr<EncodedByteBuffer> errorOrEncodedByteBuffer =
                Models.ItemEncoding.encodeItem(wynnItem, encodingSettings);
        if (errorOrEncodedByteBuffer.hasError()) {
            WynntilsMod.error("Failed to encode item: " + errorOrEncodedByteBuffer.getError());
            previewItemStack = null;
            backgroundX = (width - Texture.ITEM_SHARING_BACKGROUND.width()) / 2;
            clearWidgets();
            addError(errorOrEncodedByteBuffer.getError());
            return;
        }

        encodedItem = errorOrEncodedByteBuffer.getValue();

        // Decode the item to be displayed
        ErrorOr<WynnItem> errorOrDecodedByteBuffer = Models.ItemEncoding.decodeItem(encodedItem);
        if (errorOrDecodedByteBuffer.hasError()) {
            WynntilsMod.error("Failed to decode item: " + errorOrDecodedByteBuffer.getError());
            previewItemStack = null;
            backgroundX = (width - Texture.ITEM_SHARING_BACKGROUND.width()) / 2;
            clearWidgets();
            addError(errorOrDecodedByteBuffer.getError());
            return;
        }

        WynnItem renderedItem = errorOrDecodedByteBuffer.getValue();

        previewItemStack = new FakeItemStack(renderedItem, "From chat");

        // Find the width of the tooltip
        int tooltipWidth = LoreUtils.getTooltipLines(previewItemStack).stream()
                .map(component -> McUtils.mc().font.width(component))
                .max(Integer::compareTo)
                .orElse(0);

        // Total area the tooltip + sharing options panel + gap should cover
        int totalRenderWidth = tooltipWidth + Texture.ITEM_SHARING_BACKGROUND.width() + 10;

        // Blank space to the side of each element
        int sideGap = Math.abs((this.width - totalRenderWidth) / 2);

        tooltipX = this.width - sideGap - tooltipWidth - 20;
        backgroundX = Math.max(0, sideGap - 10);

        // Widgets need to be remade in case backgroundX has changed and the options will be misalligned
        for (AbstractWidget widget : options) {
            this.removeWidget(widget);
        }

        options = new ArrayList<>();

        addSharingOptions();
    }

    private void shareItem(String target) {
        switch (target) {
            case "guild" -> Handlers.Command.sendCommandImmediately("g " + encodedItem.toUtf16String());
            case "party" -> Handlers.Command.sendCommandImmediately("p " + encodedItem.toUtf16String());
            case "save" -> {
                ItemStack itemStackToSave = itemStack;

                // Gear items can have their item changed by cosmetics so we need to get their original item
                // FIXME: Does not work for crafted gear
                if (wynnItem instanceof GearItem gearItem) {
                    itemStackToSave = new FakeItemStack(gearItem, "From " + McUtils.playerName() + "'s Item Record");
                }

                // Item name is passed in since it is lost in the instanceof check above and looks nicer
                // saying "Saved Gale's Force to your item record" than "Saved Bow to your item record"
                savedItem = Services.ItemRecord.saveItem(wynnItem, itemStackToSave, itemStack.getHoverName());

                if (savedItem) {
                    saveButton.setMessage(Component.translatable("screens.wynntils.itemSharing.openRecord"));
                }
            }
            default -> {
                McUtils.mc().keyboardHandler.setClipboard(encodedItem.toUtf16String());

                McUtils.sendMessageToClient(Component.translatable("screens.wynntils.itemSharing.copied")
                        .withStyle(ChatFormatting.GREEN));
            }
        }
    }

    private void addSharingOptions() {
        // region Checkbox options
        if (wynnItem instanceof IdentifiableItemProperty<?, ?>) {
            options.add(this.addRenderableWidget(new WynntilsCheckbox(
                    backgroundX + 15,
                    backgroundY + 25,
                    10,
                    10,
                    Component.translatable("screens.wynntils.itemSharing.extended.name"),
                    Models.ItemEncoding.extendedIdentificationEncoding.get(),
                    Texture.ITEM_SHARING_BACKGROUND.width() - 30,
                    true,
                    (b) -> {
                        if (b == 0) {
                            Models.ItemEncoding.extendedIdentificationEncoding.store(
                                    !Models.ItemEncoding.extendedIdentificationEncoding.get());
                            refreshPreview();
                        }
                    },
                    ComponentUtils.wrapTooltips(
                            List.of(
                                    Component.translatable("screens.wynntils.itemSharing.extended.description1"),
                                    Component.translatable("screens.wynntils.itemSharing.extended.description2")),
                            150))));
        } else if (wynnItem instanceof CraftedGearItem || wynnItem instanceof CraftedConsumableItem) {
            options.add(this.addRenderableWidget(new WynntilsCheckbox(
                    backgroundX + 15,
                    backgroundY + 25,
                    10,
                    10,
                    Component.translatable("screens.wynntils.itemSharing.itemName.name"),
                    Models.ItemEncoding.shareItemName.get(),
                    Texture.ITEM_SHARING_BACKGROUND.width() - 30,
                    true,
                    (b) -> {
                        if (b == 0) {
                            Models.ItemEncoding.shareItemName.store(!Models.ItemEncoding.shareItemName.get());
                            refreshPreview();
                        }
                    },
                    ComponentUtils.wrapTooltips(
                            List.of(Component.translatable("screens.wynntils.itemSharing.itemName.description")),
                            150))));
        }
        // endregion

        // region Share buttons
        int shareButtonWidth = (Texture.ITEM_SHARING_BACKGROUND.width() - 20) / 2 - 5;

        options.add(this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.itemSharing.shareParty"), (b) -> shareItem("party"))
                .pos(backgroundX + 10, backgroundY + 45)
                .size(shareButtonWidth, 20)
                .build()));

        options.add(this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.itemSharing.shareGuild"), (b) -> shareItem("guild"))
                .pos(backgroundX + 20 + shareButtonWidth, backgroundY + 45)
                .size(shareButtonWidth, 20)
                .build()));

        // If an item has already been saved then this button will act as easy access to their saved items
        Component saveButtonMessage = savedItem
                ? Component.translatable("screens.wynntils.itemSharing.openRecord")
                : Component.translatable("screens.wynntils.itemSharing.save");

        saveButton = new Button.Builder(saveButtonMessage, (b) -> {
                    if (!savedItem) {
                        shareItem("save");
                    } else {
                        McUtils.mc().setScreen(SavedItemsScreen.create());
                    }
                })
                .pos(backgroundX + 10, backgroundY + Texture.ITEM_SHARING_BACKGROUND.height() - 30)
                .size(shareButtonWidth, 20)
                .build();

        this.addRenderableWidget(saveButton);

        options.add(saveButton);

        options.add(this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.itemSharing.copy"), (b) -> shareItem("clipboard"))
                .pos(backgroundX + 20 + shareButtonWidth, backgroundY + Texture.ITEM_SHARING_BACKGROUND.height() - 30)
                .size(shareButtonWidth, 20)
                .build()));
        // endregion
    }

    private void addError(String error) {
        this.addRenderableWidget(
                new Button.Builder(Component.translatable("screens.wynntils.itemSharing.error"), (b) -> {})
                        .pos(backgroundX + 10, backgroundY + Texture.ITEM_SHARING_BACKGROUND.height() - 30)
                        .size(Texture.ITEM_SHARING_BACKGROUND.width() - 20, 20)
                        .tooltip(Tooltip.create(Component.literal(error)))
                        .build());
    }
}
