/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model;

import com.wynntils.wynn.netresources.ItemProfilesManager;
import com.wynntils.wynn.netresources.profiles.item.IdentificationProfile;
import com.wynntils.wynn.netresources.profiles.item.ItemProfile;
import com.wynntils.mc.mixin.accessors.ItemStackInfoAccessor;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.wynn.item.GearItemStack;
import com.wynntils.wynn.item.IdentificationOrderer;
import com.wynntils.wynn.objects.ItemIdentificationContainer;
import com.wynntils.wynn.objects.Powder;
import com.wynntils.wynn.utils.WynnItemUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import org.apache.commons.lang3.ArrayUtils;

public final class ChatItemModel {
    // private-use unicode chars
    private static final String START = new String(Character.toChars(0xF5FF0));
    private static final String END = new String(Character.toChars(0xF5FF1));
    private static final String SEPARATOR = new String(Character.toChars(0xF5FF2));
    private static final String RANGE =
            "[" + new String(Character.toChars(0xF5000)) + "-" + new String(Character.toChars(0xF5F00)) + "]";
    private static final int OFFSET = 0xF5000;

    private static final boolean ENCODE_NAME = false;

    private static final Pattern ENCODED_PATTERN = Pattern.compile(START + "(?<Name>.+?)" + SEPARATOR + "(?<Ids>"
            + RANGE + "*)(?:" + SEPARATOR + "(?<Powders>" + RANGE + "+))?(?<Rerolls>" + RANGE + ")" + END);

    /**
     * Encodes the given item, as long as it is a standard gear item, into the following format
     *
     * START character (U+F5FF0)
     * Item name (optionally encoded)
     * SEPARATOR character (U+F5FF2)
     * Identifications/stars (encoded)
     * SEPARATOR (only if powdered)
     * Powders (encoded) (only if powdered)
     * Rerolls (encoded)
     * END character (U+F5FF1)
     *
     * Any encoded "value" is added to the OFFSET character value U+F5000 and then converted into the corresponding Unicode character:
     *
     * The name is encoded based on the ASCII value of each character minus 32
     *
     * Identifications are encoded either as the raw value minus the minimum value of that ID, or if the range is larger than 100,
     * the percent value 0 to 100 of the given roll.
     * Regardless of either case, this number is multiplied by 4, and the number of stars present on that ID is added.
     * This ensures that the value and star count can be encoded into a single character and be decoded later.
     *
     * Powders are encoded as numerical values 1-5. Up to 4 powders are encoded into a single character - for each new powder,
     * the running total is multiplied by 6 before the new powder value is added. Thus, each individual powder can be decoded.
     *
     * Rerolls are simply encoded as a raw number.
     *
     * This format is identical to that used in Wynntils 1.12, for compatibility across versions. It should not be
     * modified without also changing the encoding in legacy.
     *
     */
    public static String encodeItem(GearItemStack item) {
        String itemName = item.getSimpleName();

        // get identification data - ordered for consistency
        List<ItemIdentificationContainer> sortedIds = item.getOrderedIdentifications();

        // name
        StringBuilder encoded = new StringBuilder(START);
        encoded.append(ENCODE_NAME ? encodeString(itemName) : itemName);
        encoded.append(SEPARATOR);

        // ids
        for (ItemIdentificationContainer id : sortedIds) {
            if (id.identification().isFixed()) continue; // don't care about these

            int idValue = id.value();
            IdentificationProfile idProfile = id.identification();

            int translatedValue;
            if (Math.abs(idProfile.getBaseValue()) > 100) { // calculate percent
                translatedValue = (int) Math.round((idValue * 100.0 / idProfile.getBaseValue()) - 30);
            } else { // raw value
                // min/max must be flipped for inverted IDs to avoid negative values
                translatedValue = idProfile.isInverted() ? idValue - idProfile.getMax() : idValue - idProfile.getMin();
            }

            // stars
            int stars = id.stars();

            // encode value + stars in one character
            encoded.append(encodeNumber(translatedValue * 4 + stars));
        }

        // powders
        List<Powder> powders = item.getPowders();
        if (powders != null && !powders.isEmpty()) {
            encoded.append(SEPARATOR);

            int counter = 0;
            int encodedPowders = 0;
            for (Powder p : powders) {
                encodedPowders *= 6; // shift left
                encodedPowders += p.ordinal() + 1; // 0 represents no more powders
                counter++;

                if (counter == 4) { // max # of powders encoded in a single char
                    encoded.append(encodeNumber(encodedPowders));
                    encodedPowders = 0;
                    counter = 0;
                }
            }
            if (encodedPowders != 0) encoded.append(encodeNumber(encodedPowders)); // catch any leftover powders
        }

        // rerolls
        encoded.append(encodeNumber(item.getRerolls()));

        encoded.append(END);
        return encoded.toString();
    }

    private static GearItemStack decodeItem(String encoded) {
        Matcher m = ENCODED_PATTERN.matcher(encoded);
        if (!m.matches()) return null;

        String name = ENCODE_NAME ? decodeString(m.group("Name")) : m.group("Name");
        int[] ids = decodeNumbers(m.group("Ids"));
        int[] powders = m.group("Powders") != null ? decodeNumbers(m.group("Powders")) : new int[0];
        int rerolls = decodeNumbers(m.group("Rerolls"))[0];

        ItemProfile item =
                ItemProfilesManager.getItemsMap() != null ? ItemProfilesManager.getItemsMap().get(name) : null;
        if (item == null) return null;

        // ids
        List<ItemIdentificationContainer> idContainers = new ArrayList<>();

        List<String> sortedIds = new ArrayList<>(item.getStatuses().keySet());
        sortedIds.sort(Comparator.comparingInt(IdentificationOrderer.INSTANCE::getOrder));

        int counter = 0; // for id value array
        for (String shortIdName : sortedIds) {
            IdentificationProfile status = item.getStatuses().get(shortIdName);

            int value;
            int stars = 0;
            if (status.isFixed()) {
                value = status.getBaseValue();
            } else {
                if (counter >= ids.length) return null; // some kind of mismatch, abort

                // id value
                int encodedValue = ids[counter] / 4;
                if (Math.abs(status.getBaseValue()) > 100) {
                    // using bigdecimal here for precision when rounding
                    value = new BigDecimal(encodedValue + 30)
                            .movePointLeft(2)
                            .multiply(new BigDecimal(status.getBaseValue()))
                            .setScale(0, RoundingMode.HALF_UP)
                            .intValue();
                } else {
                    // min/max must be flipped for inverted IDs due to encoding
                    value = status.isInverted() ? encodedValue + status.getMax() : encodedValue + status.getMin();
                }

                // stars
                stars = ids[counter] % 4;

                counter++;
            }

            // name
            String longIdName = IdentificationProfile.getAsLongName(shortIdName);

            // create ID and append to list
            ItemIdentificationContainer idContainer =
                    WynnItemUtils.identificationFromValue(null, item, longIdName, shortIdName, value, stars);
            if (idContainer != null) idContainers.add(idContainer);
        }

        // powders
        List<Powder> powderList = new ArrayList<>();
        if (item.getPowderAmount() > 0 && powders.length > 0) {
            ArrayUtils.reverse(powders); // must reverse powders so they are read in reverse order
            for (int powderNum : powders) {
                // once powderNum is 0, all the powders have been read
                while (powderNum > 0) {
                    Powder p = Powder.values()[powderNum % 6 - 1];
                    powderList.add(0, p); // prepend powders because they are decoded in reverse

                    powderNum /= 6;
                }
            }
        }

        // create chat gear stack
        return new GearItemStack(item, idContainers, powderList, rerolls);
    }

    public static Matcher chatItemMatcher(String text) {
        return ENCODED_PATTERN.matcher(text);
    }

    public static Component insertItemComponents(Component message) {
        // no item tooltips to insert
        if (!ENCODED_PATTERN.matcher(ComponentUtils.getCoded(message)).find()) return message;

        List<MutableComponent> components =
                message.getSiblings().stream().map(Component::copy).collect(Collectors.toList());
        components.add(0, message.plainCopy().withStyle(message.getStyle()));

        MutableComponent temp = new TextComponent("");

        for (Component comp : components) {
            Matcher m = ENCODED_PATTERN.matcher(ComponentUtils.getCoded(comp));
            if (!m.find()) {
                Component newComponent = comp.copy();
                temp.append(newComponent);
                continue;
            }

            do {
                String text = ComponentUtils.getCoded(comp);
                Style style = comp.getStyle();

                GearItemStack item = decodeItem(m.group());
                if (item == null) { // couldn't decode, skip
                    comp = comp.copy();
                    continue;
                }

                MutableComponent preText = new TextComponent(text.substring(0, m.start()));
                preText.withStyle(style);
                temp.append(preText);

                // create hover-able text component for the item
                Component itemComponent = createItemComponent(item);
                temp.append(itemComponent);

                comp = new TextComponent(ComponentUtils.getLastPartCodes(ComponentUtils.getCoded(preText))
                                + text.substring(m.end()))
                        .withStyle(style);
                m = ENCODED_PATTERN.matcher(ComponentUtils.getCoded(comp)); // recreate matcher for new substring
            } while (m.find()); // search for multiple items in the same message

            temp.append(comp); // leftover text after item(s)
        }

        return temp;
    }

    private static Component createItemComponent(GearItemStack item) {
        MutableComponent itemComponent = new TextComponent(item.getItemProfile().getDisplayName())
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(item.getItemProfile().getTier().getChatFormatting());

        HoverEvent.ItemStackInfo itemHoverEvent = new HoverEvent.ItemStackInfo(item);
        ((ItemStackInfoAccessor) itemHoverEvent).setItemStack(item);
        itemComponent.withStyle(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, itemHoverEvent)));

        return itemComponent;
    }

    private static String encodeString(String text) {
        StringBuilder encoded = new StringBuilder();
        for (char c : text.toCharArray()) {
            int value = c - 32; // offset by 32 to ignore ascii control characters
            encoded.append(new String(Character.toChars(value + OFFSET))); // get encoded representation
        }
        return encoded.toString();
    }

    private static String encodeNumber(int value) {
        return new String(Character.toChars(value + OFFSET));
    }

    private static String decodeString(String text) {
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < text.length(); i += 2) {
            int value = text.codePointAt(i) - OFFSET + 32;
            decoded.append((char) value);
        }
        return decoded.toString();
    }

    private static int[] decodeNumbers(String text) {
        int[] decoded = new int[text.length() / 2];
        for (int i = 0; i < text.length(); i += 2) {
            decoded[i / 2] = text.codePointAt(i) - OFFSET;
        }
        return decoded;
    }
}
