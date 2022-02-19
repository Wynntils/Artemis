/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.utils;

import java.util.*;

public class IdentificationOrderer {

    public static IdentificationOrderer INSTANCE = new IdentificationOrderer(null, null, null);

    Map<String, Integer> order = new HashMap<>();
    ArrayList<String> groups = new ArrayList<>();
    ArrayList<String> inverted = new ArrayList<>();

    transient Map<Integer, Integer> organizedGroups = null;

    public IdentificationOrderer(
            Map<String, Integer> idOrders,
            ArrayList<String> groupRanges,
            ArrayList<String> inveverted) {}

    /**
     * @param id the identification "short" name. Ex: rawMainAttackNeutralDamage
     * @return the priority level, if not present returns -1
     */
    public int getOrder(String id) {
        return order.getOrDefault(id, -1);
    }

    /**
     * @param id the identification "short" name. Ex: rawMainAttackNeutralDamage
     * @return the group id, if not present returns -1
     */
    public int getGroup(String id) {
        if (organizedGroups == null) organizeGroups();

        return organizedGroups.getOrDefault(getOrder(id), -1);
    }

    /**
     * @param id the identification "short" name. Ex: rawMainAttackNeutralDamage
     * @return if the provided identification status is inverted (negative values are positive)
     */
    public boolean isInverted(String id) {
        return inverted.contains(id);
    }

    /**
     * Order and returns a list of string based on the provided ids
     *
     * @param holder a map containing as key the "short" id name and as value the id lore
     * @param groups if ids should be grouped
     * @return a list with the ordered lore
     */
    public List<String> order(Map<String, String> holder, boolean groups) {
        List<String> result = new ArrayList<>();
        if (holder.isEmpty()) return result;

        // order based on the priority first
        List<Map.Entry<String, String>> ordered =
                holder.entrySet().stream()
                        .sorted(Comparator.comparingInt(c -> getOrder(c.getKey())))
                        .toList();

        if (groups) {
            int lastGroup =
                    getGroup(ordered.get(0).getKey()); // first key group to avoid wrong spaces
            for (Map.Entry<String, String> keys : ordered) {
                int currentGroup = getGroup(keys.getKey()); // next key group

                if (currentGroup != lastGroup)
                    result.add(" "); // adds a space before if the group is different

                result.add(keys.getValue());
                lastGroup = currentGroup;
            }

            return result;
        }

        ordered.forEach(c -> result.add(c.getValue()));
        return result;
    }

    private void organizeGroups() {
        organizedGroups = new HashMap<>();
        for (int id = 0; id < groups.size(); id++) {
            String groupRange = groups.get(id);

            String[] split = groupRange.split("-");

            int min = Integer.parseInt(split[0]);
            int max = Integer.parseInt(split[1]);

            // register each range into a reference
            for (int i = min; i <= max; i++) {
                organizedGroups.put(i, id);
            }
        }
    }
}
