/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.containers.containers.personal;

import com.wynntils.models.containers.Container;
import com.wynntils.models.containers.type.ContainerBounds;
import com.wynntils.models.containers.type.PersonalStorageType;
import com.wynntils.models.containers.type.SearchableContainerProperty;
import com.wynntils.services.itemfilter.type.ItemProviderType;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public abstract class PersonalStorageContainer extends Container implements SearchableContainerProperty {
    private static final Pattern NEXT_PAGE_PATTERN = Pattern.compile("§f§lPage (\\d+)§a >§2>§a>§2>§a>");
    private static final Pattern PREVIOUS_PAGE_PATTERN = Pattern.compile("§f§lPage (\\d+)§a <§2<§a<§2<§a<");

    private final PersonalStorageType personalStorageType;
    private final int finalPage;
    private final List<Integer> quickJumpDestinations;

    protected PersonalStorageContainer(
            Pattern titlePattern, PersonalStorageType storageType, int finalPage, List<Integer> quickJumpDestinations) {
        super(titlePattern);

        this.personalStorageType = storageType;
        this.finalPage = finalPage;
        this.quickJumpDestinations = quickJumpDestinations;
    }

    public PersonalStorageType getPersonalStorageType() {
        return personalStorageType;
    }

    public int getFinalPage() {
        return finalPage;
    }

    public List<Integer> getQuickJumpDestinations() {
        return Collections.unmodifiableList(quickJumpDestinations);
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
        return 52;
    }

    @Override
    public int getPreviousItemSlot() {
        return 51;
    }

    @Override
    public ContainerBounds getBounds() {
        return new ContainerBounds(0, 0, 4, 8);
    }

    @Override
    public List<ItemProviderType> supportedProviderTypes() {
        return ItemProviderType.normalTypes();
    }
}
