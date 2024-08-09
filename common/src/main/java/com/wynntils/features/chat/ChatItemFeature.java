/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.chat;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.mc.event.KeyInputEvent;
import com.wynntils.mc.mixin.accessors.ChatScreenAccessor;
import com.wynntils.mc.mixin.accessors.ItemStackInfoAccessor;
import com.wynntils.models.items.FakeItemStack;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.encoding.type.EncodingSettings;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.items.properties.GearTierItemProperty;
import com.wynntils.models.items.properties.IdentifiableItemProperty;
import com.wynntils.models.items.properties.NamedItemProperty;
import com.wynntils.models.items.properties.ShinyItemProperty;
import com.wynntils.screens.itemsharing.ItemSharingScreen;
import com.wynntils.screens.itemsharing.SavedItemsScreen;
import com.wynntils.utils.EncodedByteBuffer;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.ErrorOr;
import com.wynntils.utils.type.IterationDecision;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.CHAT)
public class ChatItemFeature extends Feature {
    @RegisterKeyBind
    private final KeyBind shareItemKeybind =
            new KeyBind("Share Item", GLFW.GLFW_KEY_F3, true, null, slot -> shareItem(slot, true));

    @RegisterKeyBind
    private final KeyBind saveItemKeybind =
            new KeyBind("Save Item to Item Record", GLFW.GLFW_KEY_F6, true, null, slot -> shareItem(slot, false));

    @RegisterKeyBind
    private final KeyBind itemRecordKeybind = new KeyBind(
            "Open Item Record", GLFW.GLFW_KEY_UNKNOWN, true, () -> McUtils.mc().setScreen(SavedItemsScreen.create()));

    @Persisted
    public final Config<Boolean> showSharingScreen = new Config<>(true);

    @Persisted
    public final Config<Boolean> showPerfectOrDefective = new Config<>(true);

    private final Map<String, String> chatItems = new HashMap<>();

    @SubscribeEvent
    public void onKeyTyped(KeyInputEvent e) {
        if (!Models.WorldState.onWorld()) return;
        if (!(McUtils.mc().screen instanceof ChatScreen chatScreen)) return;

        EditBox chatInput = ((ChatScreenAccessor) chatScreen).getChatInput();

        if (!chatItems.isEmpty() && (e.getKey() == GLFW.GLFW_KEY_ENTER || e.getKey() == GLFW.GLFW_KEY_KP_ENTER)) {
            // replace the placeholder strings with the actual encoded strings
            for (Map.Entry<String, String> item : chatItems.entrySet()) {
                chatInput.setValue(chatInput.getValue().replace("<" + item.getKey() + ">", item.getValue()));
            }
            chatItems.clear();
            return;
        }

        // replace encoded strings with placeholders for less confusion

        // check for new chat item encoding
        Matcher matcher = Models.ItemEncoding.getEncodedDataPattern().matcher(chatInput.getValue());
        while (matcher.find()) {
            EncodedByteBuffer encodedByteBuffer = EncodedByteBuffer.fromUtf16String(matcher.group());
            ErrorOr<WynnItem> errorOrDecodedItem = Models.ItemEncoding.decodeItem(encodedByteBuffer);

            String name;
            if (errorOrDecodedItem.hasError()) {
                name = "encoding_error";
            } else {
                WynnItem decodedItem = errorOrDecodedItem.getValue();

                name = decodedItem.getClass().getSimpleName();

                if (decodedItem instanceof NamedItemProperty namedItemProperty) {
                    name = namedItemProperty.getName();
                }
            }

            while (chatItems.containsKey(name)) { // avoid overwriting entries
                name += "_";
            }

            chatInput.setValue(chatInput.getValue().replace(matcher.group(), "<" + name + ">"));
            chatItems.put(name, matcher.group());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChatReceived(ChatMessageReceivedEvent e) {
        if (!Models.WorldState.onWorld()) return;

        StyledText styledText = e.getStyledText();

        // Decode old chat item encoding
        StyledText modified = styledText.iterate((part, changes) -> {
            decodeChatEncoding(changes, part);
            return IterationDecision.CONTINUE;
        });

        if (modified.equals(styledText)) return;

        e.setMessage(modified);
    }

    private void shareItem(Slot hoveredSlot, boolean share) {
        if (hoveredSlot == null) return;

        // Special case for unidentified gear
        Optional<GearItem> gearItemOpt = Models.Item.asWynnItem(hoveredSlot.getItem(), GearItem.class);
        if (gearItemOpt.isPresent() && gearItemOpt.get().isUnidentified()) {
            // We can only send chat encoded gear of identified gear
            WynntilsMod.warn("Cannot make chat link of unidentified gear");
            McUtils.sendErrorToClient(I18n.get("feature.wynntils.chatItem.chatItemUnidentifiedError"));
            return;
        }

        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(hoveredSlot.getItem());
        if (wynnItemOpt.isEmpty()) return;

        // Don't try to encode unsupported items
        if (!Models.ItemEncoding.canEncodeItem(wynnItemOpt.get())) return;

        if (share) {
            if (showSharingScreen.get()) {
                McUtils.mc().setScreen(ItemSharingScreen.create(wynnItemOpt.get(), hoveredSlot.getItem()));
            } else {
                makeChatPrompt(wynnItemOpt.get());
            }
        } else {
            ItemStack itemStackToSave = hoveredSlot.getItem();

            // Gear items can have their item changed by cosmetics so we need to get their original item
            // FIXME: Does not work for crafted gear
            if (wynnItemOpt.get() instanceof GearItem gearItem) {
                itemStackToSave = new FakeItemStack(gearItem, "From " + McUtils.playerName() + "'s Item Record");
            }

            // Item name is passed in since it is lost in the instanceof check above and looks nicer
            // saying "Saved Gale's Force to your Item Record" than "Saved Bow to your Item Record"
            Services.ItemRecord.saveItem(
                    wynnItemOpt.get(), itemStackToSave, hoveredSlot.getItem().getHoverName());
        }
    }

    private void decodeChatEncoding(List<StyledTextPart> changes, StyledTextPart partToReplace) {
        Matcher matcher = Models.ItemEncoding.getEncodedDataPattern()
                .matcher(partToReplace.getString(null, PartStyle.StyleType.NONE));

        while (matcher.find()) {
            EncodedByteBuffer encodedByteBuffer = EncodedByteBuffer.fromUtf16String(matcher.group());
            ErrorOr<WynnItem> errorOrDecodedItem = Models.ItemEncoding.decodeItem(encodedByteBuffer);

            String unformattedString = partToReplace.getString(null, PartStyle.StyleType.NONE);

            String firstPart = unformattedString.substring(0, matcher.start());
            String lastPart = unformattedString.substring(matcher.end());

            PartStyle partStyle = partToReplace.getPartStyle();

            StyledTextPart first = new StyledTextPart(firstPart, partStyle.getStyle(), null, Style.EMPTY);
            List<StyledTextPart> replacedParts = errorOrDecodedItem.hasError()
                    ? List.of(createErrorPart(matcher.group(), errorOrDecodedItem.getError()))
                    : createItemPart(errorOrDecodedItem.getValue());
            StyledTextPart last = new StyledTextPart(lastPart, partStyle.getStyle(), null, Style.EMPTY);

            changes.remove(partToReplace);
            changes.add(first);
            changes.addAll(replacedParts);
            changes.add(last);

            partToReplace = last;
            matcher = Models.ItemEncoding.getEncodedDataPattern().matcher(lastPart);
        }
    }

    private StyledTextPart createErrorPart(String originalString, String error) {
        // Display the original string with a red underline and a tooltip with the error message
        Style style = Style.EMPTY.applyFormat(ChatFormatting.UNDERLINE).withColor(ChatFormatting.RED);

        HoverEvent hoverEvent = new HoverEvent(
                HoverEvent.Action.SHOW_TEXT, Component.literal(error).withStyle(ChatFormatting.RED));
        style = style.withHoverEvent(hoverEvent);

        return new StyledTextPart(originalString, style, null, Style.EMPTY);
    }

    private List<StyledTextPart> createItemPart(WynnItem wynnItem) {
        List<StyledTextPart> parts = new ArrayList<>();

        StyledText nameText = StyledText.fromString(wynnItem.getClass().getSimpleName());

        if (wynnItem instanceof NamedItemProperty namedItemProperty) {
            nameText = StyledText.fromString(namedItemProperty.getName());

            if (showPerfectOrDefective.get()) {
                if (wynnItem instanceof IdentifiableItemProperty<?, ?> identifiableItemProperty) {
                    if (identifiableItemProperty.isPerfect()) {
                        nameText = StyledText.fromComponent(
                                ComponentUtils.makeRainbowStyle("Perfect " + nameText.getString()));
                    } else if (identifiableItemProperty.isDefective()) {
                        nameText = StyledText.fromComponent(
                                ComponentUtils.makeObfuscated("Defective " + nameText.getString(), 0, 0));
                    }
                }
            }
        }

        if (wynnItem instanceof ShinyItemProperty shinyItemProperty
                && shinyItemProperty.getShinyStat().isPresent()) {
            parts.add(new StyledTextPart("⬡ ", Style.EMPTY.withColor(ChatFormatting.WHITE), null, Style.EMPTY));
        }

        Style style = Style.EMPTY.applyFormat(ChatFormatting.UNDERLINE).withColor(ChatFormatting.GOLD);

        if (wynnItem instanceof GearTierItemProperty tierItemProperty) {
            style = style.withColor(tierItemProperty.getGearTier().getChatFormatting());
        }

        ItemStack itemStack = new FakeItemStack(wynnItem, "From chat");
        HoverEvent.ItemStackInfo itemHoverEvent = new HoverEvent.ItemStackInfo(itemStack);
        ((ItemStackInfoAccessor) itemHoverEvent).setItemStack(itemStack);
        style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, itemHoverEvent));

        // Add the item name
        StyledText appenedNameText =
                StyledText.fromComponent(Component.empty().withStyle(style).append(nameText.getComponent()));
        parts.addAll(Arrays.stream(appenedNameText.getPartsAsTextArray())
                .map(StyledText::getFirstPart)
                .map(part -> part.withStyle(part.getPartStyle().withUnderlined(true)))
                .toList());

        return parts;
    }

    private void makeChatPrompt(WynnItem wynnItem) {
        EncodingSettings encodingSettings = new EncodingSettings(
                Models.ItemEncoding.extendedIdentificationEncoding.get(), Models.ItemEncoding.shareItemName.get());
        ErrorOr<EncodedByteBuffer> errorOrEncodedByteBuffer =
                Models.ItemEncoding.encodeItem(wynnItem, encodingSettings);
        if (errorOrEncodedByteBuffer.hasError()) {
            WynntilsMod.error("Failed to encode item: " + errorOrEncodedByteBuffer.getError());
            McUtils.sendErrorToClient(
                    I18n.get("feature.wynntils.chatItem.chatItemErrorEncode", errorOrEncodedByteBuffer.getError()));
            return;
        }

        if (WynntilsMod.isDevelopmentEnvironment()) {
            WynntilsMod.info("Encoded item: " + errorOrEncodedByteBuffer.getValue());
            WynntilsMod.info("Encoded item UTF-16: "
                    + errorOrEncodedByteBuffer.getValue().toUtf16String());
        }

        McUtils.sendMessageToClient(Component.translatable("feature.wynntils.chatItem.chatItemMessage")
                .withStyle(ChatFormatting.DARK_GREEN)
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(s -> s.withClickEvent(new ClickEvent(
                        ClickEvent.Action.COPY_TO_CLIPBOARD,
                        errorOrEncodedByteBuffer.getValue().toUtf16String())))
                .withStyle(s -> s.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("feature.wynntils.chatItem.chatItemTooltip")
                                .withStyle(ChatFormatting.DARK_AQUA)))));
    }
}
