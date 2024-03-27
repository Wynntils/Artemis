/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gear;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.models.elements.type.Powder;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.gear.type.GearInstance;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatListOrdering;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Encodes and decodes an item, as long as it is a standard gear item, into the following format
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
 */
public class GearChatEncoding {
    // private-use unicode chars
    private static final String START = new String(Character.toChars(0xF5FF0));
    private static final String END = new String(Character.toChars(0xF5FF1));
    private static final String SEPARATOR = new String(Character.toChars(0xF5FF2));
    private static final String RANGE =
            "[" + new String(Character.toChars(0xF5000)) + "-" + new String(Character.toChars(0xF5F00)) + "]";
    private static final Pattern ENCODED_PATTERN = Pattern.compile(START + "(?<Name>.+?)" + SEPARATOR + "(?<Ids>"
            + RANGE + "*)(?:" + SEPARATOR + "(?<Powders>" + RANGE + "+))?(?<Rerolls>" + RANGE + ")" + END);
    private static final int OFFSET = 0xF5000;
    private static final boolean ENCODE_NAME = false;

    public String toEncodedString(GearItem gearItem) {
        String itemName = gearItem.getItemInfo().name();
        Optional<GearInstance> gearInstanceOpt = gearItem.getItemInstance();
        if (gearInstanceOpt.isEmpty()) {
            WynntilsMod.error("Internal error: toEncodedString called with unidentified gear");
            return "";
        }
        GearInstance gearInstance = gearInstanceOpt.get();

        // We must use Legacy ordering for compatibility reasons
        List<StatType> sortedStats = Models.Stat.getSortedStats(gearItem.getItemInfo(), StatListOrdering.LEGACY);

        // name
        StringBuilder encoded = new StringBuilder(START);
        encoded.append(ENCODE_NAME ? encodeString(itemName) : itemName);
        encoded.append(SEPARATOR);

        // ids
        for (StatType statType : sortedStats) {
            StatActualValue actualValue = gearInstance.getActualValue(statType);
            StatPossibleValues possibleValues = gearItem.getItemInfo().getPossibleValues(statType);
            if (actualValue == null || possibleValues == null) {
                return "<mismatched stats: cannot encode item>";
            }

            if (possibleValues.isPreIdentified()) continue;

            int valueToEncode;
            if (Math.abs(possibleValues.baseValue()) > 100) {
                // Express value as percent
                valueToEncode = (int) Math.round((actualValue.value() * 100.0 / possibleValues.baseValue()) - 30);
            } else {
                // Express value as raw value shifted so lowest possible is 0
                valueToEncode = actualValue.value() - possibleValues.range().low();
            }

            // stars
            int stars = actualValue.stars();

            // encode value + stars in one character
            encoded.append(encodeNumber(valueToEncode * 4 + stars));
        }

        // powders
        List<Powder> powders = gearInstance.powders();
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
        encoded.append(encodeNumber(gearInstance.rerolls()));

        encoded.append(END);
        return encoded.toString();
    }

    public GearItem fromEncodedString(String encoded) {
        Matcher m = ENCODED_PATTERN.matcher(encoded);
        if (!m.matches()) return null;

        String name = ENCODE_NAME ? decodeString(m.group("Name")) : m.group("Name");
        int[] ids = decodeNumbers(m.group("Ids"));
        int[] powders = m.group("Powders") != null ? decodeNumbers(m.group("Powders")) : new int[0];
        int rerolls = decodeNumbers(m.group("Rerolls"))[0];

        GearInfo gearInfo = Models.Gear.getGearInfoFromDisplayName(name);
        if (gearInfo == null) return null;

        // ids
        List<StatActualValue> identifications = new ArrayList<>();

        List<StatType> sortedStats = Models.Stat.getSortedStats(gearInfo, StatListOrdering.LEGACY);

        int counter = 0; // for id value array
        for (StatType statType : sortedStats) {
            StatPossibleValues possibleValues = gearInfo.getPossibleValues(statType);

            // Pre-identified stats are not encoded, but need to be added
            if (possibleValues.isPreIdentified()) {
                identifications.add(
                        Models.Stat.buildActualValue(statType, possibleValues.baseValue(), 0, possibleValues));
                continue;
            }

            if (counter >= ids.length) return null; // some kind of mismatch, abort

            // id value
            int value;

            int encodedValue = ids[counter] / 4;
            if (Math.abs(possibleValues.baseValue()) > 100) {
                // using bigdecimal here for precision when rounding
                value = new BigDecimal(encodedValue + 30)
                        .movePointLeft(2)
                        .multiply(new BigDecimal(possibleValues.baseValue()))
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
            } else {
                value = encodedValue + possibleValues.range().low();
            }

            // stars
            int stars = ids[counter] % 4;

            counter++;

            // create ID and append to list
            identifications.add(Models.Stat.buildActualValue(statType, value, stars, possibleValues));
        }

        // powders
        List<Powder> powderList = new ArrayList<>();
        if (gearInfo.powderSlots() > 0 && powders.length > 0) {
            ArrayUtils.reverse(powders); // must reverse powders so they are read in reverse order
            for (int powderNum : powders) {
                // once powderNum is 0, all the powders have been read
                int currentPowder = powderNum;
                while (currentPowder > 0) {
                    Powder p = Powder.values()[currentPowder % 6 - 1];
                    powderList.add(0, p); // prepend powders because they are decoded in reverse

                    currentPowder /= 6;
                }
            }
        }

        // create chat gear stack
        // Note that the chat procotol does not allow for shiny stats to be transferred
        GearInstance gearInstance =
                GearInstance.create(gearInfo, identifications, powderList, rerolls, Optional.empty());
        return new GearItem(
                gearInfo,
                gearInstance,
                Optional.empty(),
                Optional.empty()); // TODO: populate setInfo but not setInstance
    }

    public Matcher gearChatEncodingMatcher(String str) {
        return ENCODED_PATTERN.matcher(str);
    }

    private String encodeString(String text) {
        StringBuilder encoded = new StringBuilder();
        for (char c : text.toCharArray()) {
            int value = c - 32; // offset by 32 to ignore ascii control characters
            encoded.append(new String(Character.toChars(value + OFFSET))); // get encoded representation
        }
        return encoded.toString();
    }

    private String encodeNumber(int value) {
        return new String(Character.toChars(value + OFFSET));
    }

    private String decodeString(String text) {
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < text.length(); i += 2) {
            int value = text.codePointAt(i) - OFFSET + 32;
            decoded.append((char) value);
        }
        return decoded.toString();
    }

    private int[] decodeNumbers(String text) {
        int[] decoded = new int[text.length() / 2];
        for (int i = 0; i < text.length(); i += 2) {
            decoded[i / 2] = text.codePointAt(i) - OFFSET;
        }
        return decoded;
    }
}
