/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilitytree.type;

import com.wynntils.core.WynntilsMod;
import com.wynntils.utils.type.Pair;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;

public enum AbilityTreeConnectionType {
    VERTICAL(
            41,
            Map.of(new boolean[] {true, false, true, false}, 42),
            new boolean[] {true, false, true, false},
            List.of()),
    HORIZONTAL(
            43,
            Map.of(new boolean[] {false, true, false, true}, 44),
            new boolean[] {false, true, false, true},
            List.of()),

    DOWN_LEFT_TURN(
            37,
            Map.of(new boolean[] {false, true, true, false}, 38),
            new boolean[] {false, true, true, false},
            List.of()),
    DOWN_RIGHT_TURN(
            39,
            Map.of(new boolean[] {false, false, true, true}, 40),
            new boolean[] {false, false, true, true},
            List.of()),

    UP_LEFT_TURN(
            33,
            Map.of(new boolean[] {true, false, false, true}, 34),
            new boolean[] {true, false, false, true},
            List.of()), // Due to the nature of the ability tree connections, this type is unused
    UP_RIGHT_TURN(
            35,
            Map.of(new boolean[] {true, true, false, false}, 36),
            new boolean[] {true, true, false, false},
            List.of()), // Due to the nature of the ability tree connections, this type is unused

    THREE_WAY_UP(
            13,
            Map.of(
                    new boolean[] {true, true, false, true}, 14,
                    new boolean[] {true, false, false, true}, 15,
                    new boolean[] {true, true, false, false}, 16,
                    new boolean[] {false, true, false, true}, 17),
            new boolean[] {true, true, false, true},
            List.of(
                    Pair.of(HORIZONTAL, UP_LEFT_TURN),
                    Pair.of(HORIZONTAL, UP_RIGHT_TURN),
                    Pair.of(UP_LEFT_TURN, UP_RIGHT_TURN))),

    THREE_WAY_RIGHT(
            18,
            Map.of(
                    new boolean[] {true, true, true, false}, 19,
                    new boolean[] {true, true, false, false}, 20,
                    new boolean[] {false, true, true, false}, 21,
                    new boolean[] {true, false, true, false}, 22),
            new boolean[] {true, true, true, false},
            List.of(
                    Pair.of(VERTICAL, DOWN_RIGHT_TURN),
                    Pair.of(VERTICAL, UP_RIGHT_TURN),
                    Pair.of(DOWN_LEFT_TURN, UP_LEFT_TURN))),
    THREE_WAY_DOWN(
            23,
            Map.of(
                    new boolean[] {false, true, true, true}, 24,
                    new boolean[] {false, false, true, true}, 25,
                    new boolean[] {false, true, true, false}, 26,
                    new boolean[] {false, true, false, true}, 27),
            new boolean[] {false, true, true, true},
            List.of(
                    Pair.of(HORIZONTAL, DOWN_LEFT_TURN),
                    Pair.of(HORIZONTAL, DOWN_RIGHT_TURN),
                    Pair.of(DOWN_LEFT_TURN, DOWN_RIGHT_TURN))),
    THREE_WAY_LEFT(
            28,
            Map.of(
                    new boolean[] {true, false, true, true}, 29,
                    new boolean[] {true, false, false, true}, 30,
                    new boolean[] {false, false, true, true}, 31,
                    new boolean[] {true, false, true, false}, 32),
            new boolean[] {true, false, true, true},
            List.of(
                    Pair.of(VERTICAL, DOWN_LEFT_TURN),
                    Pair.of(VERTICAL, UP_LEFT_TURN),
                    Pair.of(DOWN_RIGHT_TURN, UP_RIGHT_TURN))),

    FOUR_WAY(
            1,
            Map.ofEntries(
                    Map.entry(new boolean[] {true, true, true, true}, 2),
                    Map.entry(new boolean[] {true, true, false, true}, 3),
                    Map.entry(new boolean[] {true, true, true, false}, 4),
                    Map.entry(new boolean[] {false, true, true, true}, 5),
                    Map.entry(new boolean[] {true, false, true, true}, 6),
                    Map.entry(new boolean[] {true, false, false, true}, 7),
                    Map.entry(new boolean[] {true, true, false, false}, 8),
                    Map.entry(new boolean[] {false, true, true, false}, 9),
                    Map.entry(new boolean[] {false, false, true, true}, 10),
                    Map.entry(new boolean[] {true, false, true, false}, 11),
                    Map.entry(new boolean[] {false, true, false, true}, 12)),
            new boolean[] {true, true, true, true},
            List.of(
                    Pair.of(VERTICAL, HORIZONTAL),
                    Pair.of(DOWN_LEFT_TURN, UP_RIGHT_TURN),
                    Pair.of(DOWN_RIGHT_TURN, UP_LEFT_TURN),
                    Pair.of(VERTICAL, THREE_WAY_UP),
                    Pair.of(DOWN_LEFT_TURN, THREE_WAY_UP),
                    Pair.of(DOWN_RIGHT_TURN, THREE_WAY_UP),
                    Pair.of(HORIZONTAL, THREE_WAY_RIGHT),
                    Pair.of(DOWN_LEFT_TURN, THREE_WAY_RIGHT),
                    Pair.of(UP_LEFT_TURN, THREE_WAY_RIGHT),
                    Pair.of(VERTICAL, THREE_WAY_DOWN),
                    Pair.of(UP_LEFT_TURN, THREE_WAY_DOWN),
                    Pair.of(UP_RIGHT_TURN, THREE_WAY_DOWN),
                    Pair.of(HORIZONTAL, THREE_WAY_LEFT),
                    Pair.of(DOWN_RIGHT_TURN, THREE_WAY_LEFT),
                    Pair.of(UP_RIGHT_TURN, THREE_WAY_LEFT)));

    private final int baseDamage;

    // boolean[] is {up, right, down, left}
    private final Map<boolean[], Integer> activeDamageMap;

    // This is a list of all possible directions that this type can connect to.
    private final boolean[] possibleDirections;

    // This is a list of all possible merges between two AbilityTreeConnectionTypes. Self merges (this type + other
    // type) are not included.
    private final List<Pair<AbilityTreeConnectionType, AbilityTreeConnectionType>> possibleMerges;

    // A set of compatible types. If this type is merged with a type in this set, the result will be this type.
    private final Set<AbilityTreeConnectionType> selfMergeTypes;

    private final ItemStack baseItemStack;
    private final Map<Integer, ItemStack> itemStackMap;

    AbilityTreeConnectionType(
            int baseDamage,
            Map<boolean[], Integer> activeDamageMap,
            boolean[] possibleDirections,
            List<Pair<AbilityTreeConnectionType, AbilityTreeConnectionType>> possibleMerges) {
        this.baseDamage = baseDamage;
        this.activeDamageMap = activeDamageMap;
        this.possibleDirections = possibleDirections;
        this.possibleMerges = possibleMerges;

        this.itemStackMap = new HashMap<>();

        this.baseItemStack = generateItemStack(baseDamage);
        for (boolean[] active : activeDamageMap.keySet()) {
            this.itemStackMap.put(Arrays.hashCode(active), generateItemStack(activeDamageMap.get(active)));
        }

        this.selfMergeTypes = new HashSet<>();

        this.selfMergeTypes.add(this);

        for (Pair<AbilityTreeConnectionType, AbilityTreeConnectionType> merge : possibleMerges) {
            this.selfMergeTypes.add(merge.a());
            this.selfMergeTypes.add(merge.b());
        }
    }

    public static AbilityTreeConnectionType fromDamage(int damage) {
        for (AbilityTreeConnectionType type : values()) {
            if (type.baseDamage == damage) {
                return type;
            }

            for (int activeDamage : type.activeDamageMap.values()) {
                if (activeDamage == damage) {
                    return type;
                }
            }
        }

        return null;
    }

    public ItemStack getItemStack(boolean[] active) {
        return itemStackMap.getOrDefault(Arrays.hashCode(active), baseItemStack);
    }

    public boolean[] getPossibleDirections() {
        return possibleDirections;
    }

    public boolean isCompatible(AbilityTreeConnectionType other) {
        return selfMergeTypes.contains(other);
    }

    public static AbilityTreeConnectionType merge(AbilityTreeConnectionType first, AbilityTreeConnectionType second) {
        // If we are merging the same type, it does not change
        if (first == second) {
            return first;
        }

        // If the second type is compatible, the result is the first type
        if (first.selfMergeTypes.contains(second)) {
            return first;
        }

        // If the first type is compatible, the result is the second type
        if (second.selfMergeTypes.contains(first)) {
            return second;
        }

        // Swap the two variables so they are in order
        if (second.ordinal() < first.ordinal()) {
            AbilityTreeConnectionType temp = first;
            first = second;
            second = temp;
        }

        // Check if the two types can be merged
        for (AbilityTreeConnectionType type : values()) {
            for (Pair<AbilityTreeConnectionType, AbilityTreeConnectionType> pair : type.possibleMerges) {
                if (pair.a() == first && pair.b() == second) {
                    return type;
                }
            }
        }

        WynntilsMod.error(
                "Tried to merge two incompatbilty AbilityTreeConnectionTypes: " + first + " and " + second + ".");

        return first;
    }

    private ItemStack generateItemStack(int damage) {
        ItemStack itemStack = new ItemStack(Items.STONE_AXE);

        itemStack.setDamageValue(damage);

        itemStack.set(DataComponents.UNBREAKABLE, new Unbreakable(false));

        return itemStack;
    }
}
