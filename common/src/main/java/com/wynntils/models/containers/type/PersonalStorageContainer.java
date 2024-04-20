/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.containers.type;

import java.util.regex.Pattern;

public class PersonalStorageContainer extends WynncraftContainer implements SearchableContainerProperty {
    private static final Pattern NEXT_PAGE_PATTERN = Pattern.compile("§f§lPage \\d+§a >§2>§a>§2>§a>");
    private static final Pattern PREVIOUS_PAGE_PATTERN = Pattern.compile("§f§lPage \\d+§a <§2<§a<§2<§a<");

    private final PersonalStorageType personalStorageType;

    protected PersonalStorageContainer(Pattern titlePattern, PersonalStorageType storageType) {
        super(titlePattern);

        this.personalStorageType = storageType;
    }

    public PersonalStorageType getPersonalStorageType() {
        return personalStorageType;
    }

    @Override
    public Pattern getNextItemPattern() {
        return NEXT_PAGE_PATTERN;
    }

    @Override
    public Pattern getPreviousItemPattern() {
        return PREVIOUS_PAGE_PATTERN;
    }

    @Override
    public int getNextItemSlot() {
        return 8;
    }

    @Override
    public int getPreviousItemSlot() {
        return 17;
    }

    @Override
    public ContainerBounds getBounds() {
        return new ContainerBounds(0, 0, 5, 6);
    }

    @Override
    public boolean supportsAdvancedSearch() {
        return true;
    }
}
