/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.itemsharing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.items.FakeItemStack;
import com.wynntils.models.items.WynnItem;
import com.wynntils.screens.base.TextboxScreen;
import com.wynntils.screens.base.WynntilsContainerScreen;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.screens.itemsharing.widgets.SavedCategoryButton;
import com.wynntils.screens.itemsharing.widgets.SavedItemsButton;
import com.wynntils.services.itemrecord.type.SavedItem;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class SavedItemsScreen extends WynntilsContainerScreen<SavedItemsMenu> {
    private static final int ITEMS_PER_ROW = 7;
    private static final int MAX_ITEMS = 49;
    private static final int SCROLL_AREA_HEIGHT = 129;

    private boolean addingCategory = false;
    private boolean draggingScroll = false;
    private boolean editingCategory = false;

    private int itemScrollOffset = 0;
    private List<Integer> selectedSlots = new ArrayList<>();
    private Map<Integer, SavedItem> encodedItems = new TreeMap<>();

    private String currentCategory = Services.ItemRecord.getDefaultCategory();
    private TextInputBoxWidget categoryInput;

    private List<Pair<String, String>> selectedItems = new ArrayList<>();
    private int dragSelectionStartIndex = -1;

    private SavedItemsScreen(SavedItemsMenu abstractContainerMenu) {
        super(abstractContainerMenu, McUtils.inventory(), Component.literal("Saved Items Screen"));
    }

    public static Screen create() {
        return new SavedItemsScreen(SavedItemsMenu.create());
    }

    @Override
    protected void doInit() {
        super.doInit();

        // If our encoding breaks for some reason, we need to clean up the item record to prevent crashes
        Services.ItemRecord.cleanupItemRecord();

        this.leftPos = (this.width - Texture.ITEM_RECORD.width()) / 2;
        this.topPos = (this.height - Texture.ITEM_RECORD.height()) / 2;

        // region Side buttons
        this.addRenderableWidget(new SavedItemsButton(
                this.leftPos + 9,
                this.topPos + 20,
                (b) -> {
                    if (b == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        if (!addingCategory && !editingCategory) {
                            addingCategory = true;
                            addCategoryInput();
                        } else {
                            addCategory(KeyboardUtils.isShiftDown());
                            addingCategory = false;
                            editingCategory = false;
                        }
                    }
                },
                List.of(
                        Component.translatable("screens.wynntils.savedItems.addTooltip1"),
                        Component.translatable("screens.wynntils.savedItems.addTooltip2"),
                        Component.translatable("screens.wynntils.savedItems.addTooltip3"),
                        Component.translatable("screens.wynntils.savedItems.addTooltip4")),
                Texture.ITEM_RECORD_ADD));

        this.addRenderableWidget(new SavedItemsButton(
                this.leftPos + 9,
                this.topPos + 31,
                (b) -> {
                    selectedItems = new ArrayList<>();
                    addingCategory = false;
                    editingCategory = false;

                    deleteCategory();
                },
                List.of(
                        Component.translatable("screens.wynntils.savedItems.deleteTooltip1"),
                        Component.translatable("screens.wynntils.savedItems.deleteTooltip2")),
                Texture.ITEM_RECORD_DELETE));

        this.addRenderableWidget(new SavedItemsButton(
                this.leftPos + 9,
                this.topPos + 42,
                (b) -> {
                    if (b == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        moveSelectedItems();
                    } else if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        selectedItems = new ArrayList<>();
                        selectedSlots = new ArrayList<>();
                    }
                },
                List.of(
                        Component.translatable("screens.wynntils.savedItems.changeCategoryTooltip1"),
                        Component.translatable("screens.wynntils.savedItems.changeCategoryTooltip2"),
                        Component.translatable("screens.wynntils.savedItems.changeCategoryTooltip3")),
                Texture.ITEM_RECORD_CONFIRM));

        this.addRenderableWidget(new SavedItemsButton(
                this.leftPos + 9,
                this.topPos + 144,
                null,
                List.of(
                        Component.translatable("screens.wynntils.savedItems.help")
                                .withStyle(ChatFormatting.UNDERLINE),
                        Component.translatable("screens.wynntils.savedItems.help1"),
                        Component.translatable("screens.wynntils.savedItems.help2"),
                        Component.translatable("screens.wynntils.savedItems.help3"),
                        Component.translatable("screens.wynntils.savedItems.help4"),
                        Component.translatable("screens.wynntils.savedItems.help5")),
                Texture.ITEM_RECORD_HELP));
        // endregion

        // region Category navigation buttons
        this.addRenderableWidget(new SavedCategoryButton(this.leftPos + 21, this.topPos + 9, this, false));

        this.addRenderableWidget(new SavedCategoryButton(this.leftPos + 139, this.topPos + 9, this, true));
        // endregion

        // Re add input box if screen resized during input
        if (addingCategory) {
            addCategoryInput();
        }

        populateItems();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        PoseStack poseStack = guiGraphics.pose();

        RenderUtils.drawTexturedRect(poseStack, Texture.ITEM_RECORD, this.leftPos, this.topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // We don't want a label
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot == null) return;
        if (slot.getItem() == ItemStack.EMPTY) return;

        // Right click goes to sharing menu
        // Right+Shift deletes the item from storage
        // Left click toggles the selection of the item
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (KeyboardUtils.isShiftDown()) {
                deleteItem(encodedItems.get(slot.index).base64());
            } else {
                Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(slot.getItem());

                if (wynnItemOpt.isPresent()) {
                    McUtils.mc().setScreen(ItemSharingScreen.create(wynnItemOpt.get(), slot.getItem(), true));
                } else {
                    McUtils.sendMessageToClient(Component.translatable("screens.wynntils.savedItems.unableToShare"));
                }
            }
        } else if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int rowOffset = ITEMS_PER_ROW * itemScrollOffset;
            int index = slot.index + rowOffset;
            dragSelectionStartIndex = index;
        }
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.doRender(guiGraphics, mouseX, mouseY, partialTick);
        PoseStack poseStack = guiGraphics.pose();

        if (!addingCategory && !editingCategory) {
            FontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            StyledText.fromString(currentCategory),
                            this.leftPos + 37,
                            this.leftPos + 37 + 97,
                            this.topPos + 10,
                            this.topPos + 10 + 8,
                            97,
                            CommonColors.WHITE,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            TextShadow.NONE);
        }

        for (int selectedSlot : selectedSlots) {
            RenderUtils.drawRectBorders(
                    poseStack,
                    CommonColors.WHITE,
                    this.leftPos + this.getMenu().getSlot(selectedSlot).x,
                    this.topPos + this.getMenu().getSlot(selectedSlot).y,
                    this.leftPos + this.getMenu().getSlot(selectedSlot).x + 16,
                    this.topPos + this.getMenu().getSlot(selectedSlot).y + 16,
                    0,
                    1);
        }

        renderScrollButton(poseStack);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scrollButtonRenderX = this.leftPos + 155;
        float scrollButtonRenderY =
                this.topPos + 18 + MathUtils.map(itemScrollOffset, 0, getMaxScrollOffset(), 0, SCROLL_AREA_HEIGHT);

        if (MathUtils.isInside(
                (int) mouseX,
                (int) mouseY,
                (int) scrollButtonRenderX,
                (int) (scrollButtonRenderX + Texture.ITEM_RECORD_SCROLL.width()),
                (int) scrollButtonRenderY,
                (int) (scrollButtonRenderY + Texture.ITEM_RECORD_SCROLL.height()))) {
            draggingScroll = true;
            return true;
        }

        float categoryRenderX = this.leftPos + 36;
        float categoryRenderY = this.topPos + 10;

        // Click the area where the category title is rendered to edit it
        if (!currentCategory.equals(Services.ItemRecord.getDefaultCategory())
                && !editingCategory
                && !addingCategory
                && MathUtils.isInside(
                        (int) mouseX,
                        (int) mouseY,
                        (int) categoryRenderX,
                        (int) (categoryRenderX + 97),
                        (int) categoryRenderY,
                        (int) (categoryRenderY + 8))) {
            editingCategory = true;
            addCategoryInput();

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!draggingScroll) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        int renderY = this.topPos + 18;
        int newValue = Math.round(
                MathUtils.map((float) mouseY, renderY, renderY + SCROLL_AREA_HEIGHT, 0, getMaxScrollOffset()));

        scrollItems(newValue - itemScrollOffset);

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;

        Slot dragSelectionEndSlot = super.findSlot(mouseX, mouseY);

        if (dragSelectionStartIndex != -1) {
            int dragSelectionEndIndex =
                    dragSelectionEndSlot == null ? dragSelectionStartIndex : dragSelectionEndSlot.index;

            int index = Math.min(dragSelectionStartIndex, dragSelectionEndIndex);
            int endIndex = Math.max(dragSelectionStartIndex, dragSelectionEndIndex);

            for (int i = index; i <= endIndex; i++) {
                if (i >= encodedItems.size()) break;

                SavedItem savedItem = encodedItems.get(i);

                if (selectedItems.contains(new Pair<>(currentCategory, savedItem.base64()))) {
                    selectedItems.remove(new Pair<>(currentCategory, savedItem.base64()));
                    selectedSlots.remove((Integer) (i));
                } else {
                    selectedItems.add(new Pair<>(currentCategory, savedItem.base64()));
                    selectedSlots.add(i);
                }
            }

            dragSelectionStartIndex = -1;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double scrollValue = -Math.signum(deltaY);

        if (mouseY < this.topPos + 18) {
            scrollCategories((int) scrollValue);
        } else {
            scrollItems((int) scrollValue);
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter can also be used to submit name for new category title
        if ((addingCategory || editingCategory) && keyCode == GLFW.GLFW_KEY_ENTER) {
            addCategory(KeyboardUtils.isShiftDown());

            if (addingCategory) {
                addingCategory = false;
            } else if (editingCategory) {
                editingCategory = false;
            }
            return false;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void scrollCategories(int scrollDirection) {
        List<String> categories = new ArrayList<>(Services.ItemRecord.categories.get());

        addingCategory = false;
        editingCategory = false;
        this.removeWidget(categoryInput);
        categoryInput = null;

        int currentIndex = categories.indexOf(currentCategory);
        int newIndex = currentIndex + scrollDirection;

        // If scrolling past one end of the set, go to the other end
        if (newIndex >= categories.size()) {
            currentCategory = categories.getFirst();
        } else if (newIndex < 0) {
            currentCategory = categories.getLast();
        } else {
            currentCategory = categories.get(newIndex);
        }

        itemScrollOffset = 0;
        populateItems();
    }

    private void renderScrollButton(PoseStack poseStack) {
        float renderX = this.leftPos + 155;
        float renderY =
                this.topPos + 18 + MathUtils.map(itemScrollOffset, 0, getMaxScrollOffset(), 0, SCROLL_AREA_HEIGHT);

        RenderUtils.drawTexturedRect(poseStack, Texture.ITEM_RECORD_SCROLL, renderX, renderY);
    }

    private void addCategoryInput() {
        // Remove previous widget if it exists
        if (categoryInput != null) {
            this.removeWidget(categoryInput);
        }

        categoryInput = new TextInputBoxWidget(
                this.leftPos + 37, this.topPos + 5, 97, 16, null, (TextboxScreen) McUtils.mc().screen, categoryInput);

        // Get the current category name and add if we are editing
        if (editingCategory) {
            categoryInput.setTextBoxInput(currentCategory);
        }

        this.addRenderableWidget(categoryInput);
    }

    private void addCategory(boolean keepOriginals) {
        String newCategory = categoryInput.getTextBoxInput();

        // Remove the input widget
        this.removeWidget(categoryInput);
        categoryInput = null;

        if (newCategory.isEmpty()) return;

        if (addingCategory) {
            Services.ItemRecord.addCategory(newCategory, selectedItems, keepOriginals);

            if (!selectedItems.isEmpty()) {
                currentCategory = newCategory;
                selectedItems = new ArrayList<>();
                populateItems();
            }
        } else if (editingCategory && !currentCategory.equals(newCategory)) {
            Services.ItemRecord.renameCategory(currentCategory, newCategory);

            // Change to new category
            currentCategory = newCategory;

            populateItems();
        }
    }

    private void deleteCategory() {
        Services.ItemRecord.deleteCategory(currentCategory);

        if (categoryInput != null) {
            this.removeWidget(categoryInput);
            categoryInput = null;
        }

        // Reset to default category
        currentCategory = Services.ItemRecord.getDefaultCategory();

        populateItems();
    }

    private void moveSelectedItems() {
        Services.ItemRecord.moveSelectedItems(selectedItems, currentCategory, KeyboardUtils.isShiftDown());

        selectedItems = new ArrayList<>();

        populateItems();
    }

    private void deleteItem(String base64) {
        Services.ItemRecord.deleteItem(base64);

        // Scroll up if there is now an empty row
        if (((this.menu.getItems().size() - 1) % ITEMS_PER_ROW) == 0 && itemScrollOffset > 0) {
            itemScrollOffset--;
        }

        populateItems();
    }

    private void populateItems() {
        List<ItemStack> items = new ArrayList<>();
        List<ItemStack> selected = new ArrayList<>();
        encodedItems = new TreeMap<>();
        selectedSlots = new ArrayList<>();

        // Clear current items
        this.menu.clear();

        List<SavedItem> savedItems = Services.ItemRecord.savedItems.get().stream()
                .filter(savedItem -> savedItem.categories().contains(currentCategory))
                .toList();

        // No items in current category
        if (savedItems.isEmpty()) return;

        // Save all items in current category
        encodedItems = savedItems.stream()
                .collect(TreeMap::new, (map, savedItem) -> map.put(map.size(), savedItem), TreeMap::putAll);

        int rowOffset = ITEMS_PER_ROW * itemScrollOffset;

        for (int i = rowOffset; i < (MAX_ITEMS + rowOffset); i++) {
            if (i >= savedItems.size()) break;

            SavedItem savedItem = savedItems.get(i);

            ItemStack itemStack = savedItem.itemStack();

            itemStack = new FakeItemStack(
                    savedItem.wynnItem(), itemStack, "From " + McUtils.playerName() + "'s Item Record");

            for (Pair<String, String> selectedItem : selectedItems) {
                if (selectedItem.a().equals(currentCategory) && selectedItem.b().equals(savedItem.base64())) {
                    selected.add(itemStack);
                    break;
                }
            }

            items.add(itemStack);
        }

        // Update items in menu
        for (int i = 0; i < MAX_ITEMS; i++) {
            if (i > items.size() - 1) break;

            this.menu.setItem(i, 0, items.get(i));

            if (selected.contains(items.get(i))) {
                selectedSlots.add(i);
            }
        }
    }

    private void scrollItems(int scollDirection) {
        int maxValue = getMaxScrollOffset();

        itemScrollOffset = MathUtils.clamp(itemScrollOffset + scollDirection, 0, maxValue);

        populateItems();
    }

    private int getMaxScrollOffset() {
        int maxItemOffset = Math.max(
                0,
                Services.ItemRecord.savedItems.get().stream()
                                .filter(savedItem -> savedItem.categories().contains(currentCategory))
                                .toList()
                                .size()
                        - MAX_ITEMS);
        return maxItemOffset / ITEMS_PER_ROW + (maxItemOffset % ITEMS_PER_ROW > 0 ? 1 : 0);
    }
}
