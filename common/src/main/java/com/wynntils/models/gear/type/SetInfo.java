package com.wynntils.models.gear.type;

import com.wynntils.models.stats.type.StatType;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Map;

public record SetInfo(String name, List<Map<StatType, Integer>> bonuses, List<String> items) {
    /**
     * @param numberOfItems The number of items equipped to get the set bonus for
     *                      (clamped to the number of items in the set)
     * @return A map of stat names to the bonus value for that stat
     */
    public Map<StatType, Integer> getBonusForItems(int numberOfItems) {
        return bonuses.get(Mth.clamp(numberOfItems, 1, items.size()) - 1);
    }

    @Override
    public String toString() {
        return "SetInfo{" +
                "name='" + name + '\'' +
                ", bonuses=" + bonuses +
                ", items=" + items +
                '}';
    }
}
