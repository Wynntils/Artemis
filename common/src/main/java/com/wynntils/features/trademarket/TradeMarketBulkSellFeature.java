/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.trademarket;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.mc.event.ContainerSetSlotEvent;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.TRADEMARKET)
public class TradeMarketBulkSellFeature extends Feature {
    private static final Pattern ITEM_NAME_PATTERN =
            Pattern.compile("§6Selling §f(\\d+|\\d+,\\d+) ([^À]*)À*(:?§6)? for §f[\\d,]*§7² Each");
    private static final String CLICK_TO_SELL_ITEM = "§6Click an Item to Sell";
    private static final String CLICK_TO_SET_AMOUNT = "Click to Set Amount";
    private static final String SELL_DIALOGUE_TITLE = "What would you like to sell?";
    private static final String TYPE_SELL_AMOUNT = "Type the amount you wish to sell or type 'cancel' to cancel:";

    private static final int SELLABLE_ITEM_SLOT = 10;
    private static final int AMOUNT_ITEM_SLOT = 11;

    @Persisted
    public final Config<Integer> bulkSell1Amount = new Config<>(64);

    @Persisted
    public final Config<Integer> bulkSell2Amount = new Config<>(0);

    @Persisted
    public final Config<Integer> bulkSell3Amount = new Config<>(0);

    private boolean sendAmountMessage = false;
    private int amountToSend = 0;

    @SubscribeEvent
    public void onSellDialogueUpdated(ContainerSetSlotEvent.Pre e) {
        if (!(McUtils.mc().screen instanceof ContainerScreen containerScreen)) return;

        StyledText title = StyledText.fromComponent(containerScreen.getTitle());
        if (!title.equalsString(SELL_DIALOGUE_TITLE, PartStyle.StyleType.NONE)) return;

        StyledText amountItemName = StyledText.fromComponent(
                containerScreen.getMenu().getSlot(AMOUNT_ITEM_SLOT).getItem().getHoverName());
        if (!amountItemName.equalsString(CLICK_TO_SET_AMOUNT, PartStyle.StyleType.NONE)) return;

        String soldItemName = getSoldItemName(containerScreen);

        removeSellButtons(containerScreen);

        if (soldItemName == null) return;

        addSellButtons(containerScreen, soldItemName);
    }

    @SubscribeEvent
    public void onChatMessage(ChatMessageReceivedEvent e) {
        if (!sendAmountMessage) return;
        if (!e.getOriginalStyledText().equalsString(TYPE_SELL_AMOUNT, PartStyle.StyleType.NONE)) return;

        WynntilsMod.info("Trying to bulk sell " + amountToSend + " items");

        McUtils.sendChat(String.valueOf(amountToSend));

        sendAmountMessage = false;
    }

    private String getSoldItemName(MenuAccess<ChestMenu> cs) {
        ItemStack itemStack = cs.getMenu().getSlot(SELLABLE_ITEM_SLOT).getItem();
        if (itemStack == ItemStack.EMPTY) return null;

        StyledText itemStackName = StyledText.fromComponent(itemStack.getHoverName());
        if (itemStackName.getString().equals(CLICK_TO_SELL_ITEM)) return null;

        Matcher m = itemStackName.getMatcher(ITEM_NAME_PATTERN);
        if (!m.matches()) return null;

        return m.group(2);
    }

    private int getAmountInInventory(String name) {
        int amount = 0;

        for (ItemStack itemStack : McUtils.inventory().items) {
            StyledText itemName = StyledText.fromComponent(itemStack.getHoverName())
                    .getNormalized()
                    .trim();
            if (itemName.getString().endsWith(name)) {
                amount += itemStack.getCount();
            }
        }

        return amount;
    }

    private void addSellButtons(ContainerScreen containerScreen, String soldItemName) {
        containerScreen.addRenderableWidget(new SellButton(
                containerScreen.leftPos - SellButton.BUTTON_WIDTH,
                containerScreen.topPos,
                () -> getAmountInInventory(soldItemName),
                true));

        if (bulkSell1Amount.get() > 0) {
            containerScreen.addRenderableWidget(new SellButton(
                    containerScreen.leftPos - SellButton.BUTTON_WIDTH,
                    containerScreen.topPos + 21,
                    bulkSell1Amount::get,
                    false));
        }
        if (bulkSell2Amount.get() > 0) {
            containerScreen.addRenderableWidget(new SellButton(
                    containerScreen.leftPos - SellButton.BUTTON_WIDTH,
                    containerScreen.topPos + 42,
                    bulkSell2Amount::get,
                    false));
        }
        if (bulkSell3Amount.get() > 0) {
            containerScreen.addRenderableWidget(new SellButton(
                    containerScreen.leftPos - SellButton.BUTTON_WIDTH,
                    containerScreen.topPos + 63,
                    bulkSell3Amount::get,
                    false));
        }
    }

    private void removeSellButtons(ContainerScreen containerScreen) {
        containerScreen.children.stream()
                .filter(child -> child instanceof SellButton)
                .toList()
                .forEach(containerScreen::removeWidget);
    }

    private final class SellButton extends WynntilsButton {
        private static final int BUTTON_WIDTH = 60;
        private static final int BUTTON_HEIGHT = 20;

        private final Supplier<Integer> amountSupplier;

        private SellButton(int x, int y, Supplier<Integer> amountSupplier, boolean isAll) {
            super(
                    x,
                    y,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    isAll
                            ? Component.translatable("feature.wynntils.tradeMarketBulkSell.sellAll")
                            : Component.translatable(
                                    "feature.wynntils.tradeMarketBulkSell.sell", amountSupplier.get()));

            this.amountSupplier = amountSupplier;
        }

        @Override
        public void onPress() {
            amountToSend = amountSupplier.get();
            sendAmountMessage = true;

            ContainerUtils.clickOnSlot(
                    AMOUNT_ITEM_SLOT,
                    McUtils.containerMenu().containerId,
                    0,
                    McUtils.containerMenu().getItems());
        }
    }
}
