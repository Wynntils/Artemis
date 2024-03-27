/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.game;

import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemAnnotator;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.gear.type.GearInstance;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.gear.type.SetInfo;
import com.wynntils.models.gear.type.SetInstance;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.InventoryUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class GearAnnotator implements ItemAnnotator {
    private static final Pattern GEAR_PATTERN =
            Pattern.compile("^(?:§f⬡ )?(?<rarity>§[5abcdef])(?<unidentified>Unidentified )?(?:Shiny )?(?<name>.+)$");
    private static final Pattern SET_PATTERN = Pattern.compile("§a(.+) Set §7\\((\\d)/\\d\\)");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, StyledText name) {
        Matcher matcher = name.getMatcher(GEAR_PATTERN);
        if (!matcher.matches()) return null;

        // Lookup Gear Profile
        String itemName = matcher.group("name");
        GearInfo gearInfo = Models.Gear.getGearInfoFromDisplayName(itemName);
        if (gearInfo == null) return null;

        // Verify that rarity matches
        if (!matcher.group("rarity").equals(gearInfo.tier().getChatFormatting().toString())) return null;

        GearInstance gearInstance =
                matcher.group("unidentified") != null ? null : Models.Gear.parseInstance(gearInfo, itemStack);

        Optional<SetInfo> setInfo = Optional.empty();
        Optional<SetInstance> setInstance = Optional.empty();
        if (gearInfo.tier() == GearTier.SET) {
            setInfo = Optional.of(Models.Set.getSetInfoForItem(gearInfo.name()));
            setInstance = Optional.of(new SetInstance(
                    setInfo.get(),
                    getActiveItems(setInfo.get().name()),
                    getTrueCount(setInfo.get().name())));
        }
        return new GearItem(gearInfo, gearInstance, setInfo, setInstance);
    }

    /**
     * Goes through user's equipped gear (all armour, accessories, held weapon) and returns count of specified
     * @return <wynncraft count, true count>
     */
    private Map<String, Boolean> getActiveItems(String setName) {
        Map<String, Boolean> activeItems = new HashMap<>();

        int[] accessorySlots = {9, 10, 11, 12};
        if (McUtils.player().hasContainerOpen()) {
            // Scale according to server chest size
            // Eg. 3 row chest size = 27 (ends on i=26 since 0-index), we would get accessory slots {27, 28, 29, 30}
            int baseSize = McUtils.player().containerMenu.getItems().size();
            accessorySlots = new int[] {baseSize, baseSize + 1, baseSize + 2, baseSize + 3};
        }

        for (String itemName : Models.Set.getSetInfoForId(setName).items()) {
            boolean armorActive = McUtils.inventory().armor.stream()
                    .anyMatch(itemStack -> itemStack.getHoverName().getString().equals(itemName));
            boolean accessoryActive = Arrays.stream(accessorySlots).anyMatch(i -> McUtils.inventory()
                    .getItem(i)
                    .getHoverName()
                    .getString()
                    .equals(itemName));
            boolean heldActive =
                    InventoryUtils.itemRequirementsMet(McUtils.player().getItemInHand(InteractionHand.MAIN_HAND))
                            && McUtils.player()
                                    .getItemInHand(InteractionHand.MAIN_HAND)
                                    .getHoverName()
                                    .getString()
                                    .equals(itemName);

            boolean isActive = armorActive || accessoryActive || heldActive;

            activeItems.put(itemName, isActive);
        }
        return activeItems;
    }

    /**
     * @return true count of specified set
     */
    private int getTrueCount(String setName) {
        int count = 0;

        for (ItemStack itemStack : McUtils.inventory().armor) {
            for (StyledText line : LoreUtils.getLore(itemStack)) {
                Matcher nameMatcher = SET_PATTERN.matcher(line.getString());
                if (nameMatcher.matches() && nameMatcher.group(1).equals(setName)) {
                    count++;
                    break;
                }
            }
        }

        int[] accessorySlots = {9, 10, 11, 12};
        if (McUtils.player().hasContainerOpen()) {
            // Scale according to server chest size
            // Eg. 3 row chest size = 27 (ends on i=26 since 0-index), we would get accessory slots {27, 28, 29, 30}
            int baseSize = McUtils.player().containerMenu.getItems().size();
            accessorySlots = new int[] {baseSize, baseSize + 1, baseSize + 2, baseSize + 3};
        }

        for (int i : accessorySlots) {
            for (StyledText line : LoreUtils.getLore(McUtils.inventory().getItem(i))) {
                Matcher nameMatcher = SET_PATTERN.matcher(line.getString());
                if (nameMatcher.matches() && nameMatcher.group(1).equals(setName)) {
                    count++;
                    break;
                }
            }
        }

        if (InventoryUtils.itemRequirementsMet(McUtils.player().getItemInHand(InteractionHand.MAIN_HAND))) {
            for (StyledText line : LoreUtils.getLore(McUtils.player().getItemInHand(InteractionHand.MAIN_HAND))) {
                Matcher nameMatcher = SET_PATTERN.matcher(line.getString());
                if (nameMatcher.matches() && nameMatcher.group(1).equals(setName)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }
}
