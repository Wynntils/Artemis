/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.questbook;

import com.wynntils.core.WynntilsMod;
import com.wynntils.mc.utils.ItemUtils;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;

public class QuestInfo {
    private static final Pattern QUEST_NAME_MATCHER = Pattern.compile("^§.§l(.*)֎?À $");
    private static final Pattern STATUS_MATCHER = Pattern.compile("^§.(.*)(?:\\.\\.\\.|!)$");
    private static final Pattern LENGTH_MATCHER = Pattern.compile("^§a-§r§7 Length: §r§f(.*)$");
    private static final Pattern LEVEL_MATCHER = Pattern.compile("^\"§a.§r§7 Combat Lv. Min: §r§f(\\d+)\"$");

    private final String name;
    private final QuestStatus status;
    private final QuestLength length;
    private final String minLevel;
    private final String nextTask;

    public QuestInfo(String name, QuestStatus status, QuestLength length, String minLevel, String nextTask) {
        this.name = name;
        this.status = status;
        this.length = length;
        this.minLevel = minLevel;
        this.nextTask = nextTask;
    }

    public String getName() {
        return name;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public QuestLength getLength() {
        return length;
    }

    public String getMinLevel() {
        return minLevel;
    }

    public String getNextTask() {
        return nextTask;
    }

    @Override
    public String toString() {
        return "QuestInfo[" + "name=\""
                + name + "\", " + "status="
                + status + ", " + "length="
                + length + ", " + "minLevel="
                + minLevel + ", " + "nextTask=\""
                + nextTask + "\"]";
    }

    public static QuestInfo fromItem(ItemStack item) {
        String name = getQuestName(item);
        if (name == null) return null;

        LinkedList<String> lore = ItemUtils.getLore(item);

        QuestStatus status = getQuestStatus(lore);
        if (status == null) return null;

        if (!skipEmptyLine(lore)) return null;

        String combatLevel = getRequirements(lore);

        QuestLength questLength = getQuestLength(lore);
        if (questLength == null) return null;

        if (!skipEmptyLine(lore)) return null;

        String description = getDescription(lore);

        QuestInfo questInfo = new QuestInfo(name, status, questLength, combatLevel, description);
        return questInfo;
    }

    private static String getQuestName(ItemStack item) {
        String rawName = item.getHoverName().getString();
        if (rawName.trim().isEmpty()) {
            return null;
        }
        Matcher m = QUEST_NAME_MATCHER.matcher(rawName);
        if (!m.find()) {
            WynntilsMod.warn("Non-matching quest name: " + rawName);
            return null;
        }
        return m.group(1);
    }

    private static QuestStatus getQuestStatus(LinkedList<String> lore) {
        String rawStatus = lore.pop();
        Matcher m = STATUS_MATCHER.matcher(rawStatus);
        if (!m.find()) {
            WynntilsMod.warn("Non-matching status value: " + rawStatus);
            return null;
        }
        return QuestStatus.fromString(m.group(1));
    }

    private static boolean skipEmptyLine(LinkedList<String> lore) {
        String loreLine = lore.pop();
        if (!loreLine.isEmpty()) {
            WynntilsMod.warn("Unexpected value in quest: " + loreLine);
            return false;
        }
        return true;
    }

    private static String getRequirements(LinkedList<String> lore) {
        // FIXME: not done
        String loreLine;
        String combatLevel = "";
        loreLine = lore.getFirst();
        while (loreLine.contains("Lv. Min")) {
            lore.pop();
            if (loreLine.contains("Combat Lv. Min")) {
            //    System.out.println("got  level req:" + loreLine);
                combatLevel = loreLine;
                // §a✔§r§7 Combat Lv. Min: §r§f4
                // §c✖§r§7 Combat Lv. Min: §r§f54
            } else {
       //         System.out.println("####### GOT OTHER REQ:" + loreLine);
                // §a✔§r§7 Fishing Lv. Min: §r§f1
                // §c✖§r§7 Mining Lv. Min: §r§f15
                // §c✖§r§7 Farming Lv. Min: §r§f20

                // ####### GOT OTHER REQ:§c✖§r§7 Mining Lv. Min: §r§f20
                // ####### GOT OTHER REQ:§c✖§r§7 Woodcutting Lv. Min: §r§f20
                // ####### GOT OTHER REQ:§c✖§r§7 Fishing Lv. Min: §r§f20
                // Note: one quest can have multiple!!!
            }
            loreLine = lore.getFirst();
        }
        return combatLevel;
    }

    private static QuestLength getQuestLength(LinkedList<String> lore) {
        String lengthRaw = lore.pop();

        Matcher m = LENGTH_MATCHER.matcher(lengthRaw);
        if (!m.find()) {
            WynntilsMod.warn("Non-matching quest length: " + lengthRaw);
            return null;
        }
        return QuestLength.fromString(m.group(1));
    }

    private static String getDescription(LinkedList<String> lore) {
        // The last two lines is an empty line and "RIGHT-CLICK TO TRACK"; skip those
        List<String> descriptionLines = lore.subList(0, lore.size() - 2);
        // Every line begins with a format code of length 2 ("§7"), skip that
        // and join everything together, trying to avoid excess whitespace
        String description = String.join(
                        " ",
                        descriptionLines.stream().map(line -> line.substring(2)).toList())
                .replaceAll("  ", " ")
                .trim();
        return description;
    }
}
