/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.containers;

import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ContainerSetContentEvent;
import com.wynntils.mc.event.ScreenClosedEvent;
import com.wynntils.mc.event.ScreenInitEvent;
import com.wynntils.models.containers.containers.personal.PersonalStorageContainer;
import com.wynntils.models.containers.type.PersonalStorageType;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class BankModel extends Model {
    @Persisted
    private final Storage<Integer> finalAccountBankPage = new Storage<>(21);

    @Persisted
    private final Storage<Integer> finalBlockBankPage = new Storage<>(12);

    @Persisted
    private final Storage<Integer> finalBookshelfPage = new Storage<>(10);

    @Persisted
    private final Storage<Integer> finalMiscBucketPage = new Storage<>(10);

    @Persisted
    private final Storage<Map<String, Integer>> finalCharacterBankPages = new Storage<>(new TreeMap<>());

    @Persisted
    private final Storage<Map<Integer, String>> customAccountBankPageNames = new Storage<>(new TreeMap<>());

    @Persisted
    private final Storage<Map<Integer, String>> customBlockBankPageNames = new Storage<>(new TreeMap<>());

    @Persisted
    private final Storage<Map<Integer, String>> customBookshelfPageNames = new Storage<>(new TreeMap<>());

    @Persisted
    private final Storage<Map<Integer, String>> customMiscBucketPageNames = new Storage<>(new TreeMap<>());

    @Persisted
    private final Storage<Map<String, Map<Integer, String>>> customCharacterBankPagesNames =
            new Storage<>(new TreeMap<>());

    public static final int QUICK_JUMP_SLOT = 7;
    public static final String FINAL_PAGE_NAME = "\uDB3F\uDFFF";

    private static final int MAX_CHARACTER_BANK_PAGES = 10;
    private static final StyledText LAST_BANK_PAGE_STRING = StyledText.fromString(">§4>§c>§4>§c>");

    private boolean editingName;
    private int currentPage = 1;
    private PersonalStorageContainer personalStorageContainer = null;
    private PersonalStorageType storageContainerType = null;

    public BankModel() {
        super(List.of());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onScreenInit(ScreenInitEvent e) {
        storageContainerType = null;

        if (!(e.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (!(Models.Container.getCurrentContainer() instanceof PersonalStorageContainer container)) {
            return;
        }

        personalStorageContainer = container;

        storageContainerType = personalStorageContainer.getPersonalStorageType();

        editingName = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenClose(ScreenClosedEvent e) {
        storageContainerType = null;
        currentPage = 1;
        editingName = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onContainerSetContent(ContainerSetContentEvent.Pre event) {
        if (storageContainerType == null) return;

        ItemStack previousPageItem = event.getItems().get(personalStorageContainer.getPreviousItemSlot());
        Matcher previousPageMatcher = StyledText.fromComponent(previousPageItem.getHoverName())
                .getMatcher(personalStorageContainer.getPreviousItemPattern());

        if (previousPageMatcher.matches()) {
            currentPage = Integer.parseInt(previousPageMatcher.group(1)) + 1;
        }

        ItemStack nextPageItem = event.getItems().get(personalStorageContainer.getNextItemSlot());
        Matcher nextPageMatcher = StyledText.fromComponent(nextPageItem.getHoverName())
                .getMatcher(personalStorageContainer.getNextItemPattern());

        if (nextPageMatcher.matches()) {
            currentPage = Integer.parseInt(nextPageMatcher.group(1)) - 1;
        }

        if (isItemIndicatingLastBankPage(nextPageItem)) {
            updateFinalPage();
        }
    }

    public String getPageName(int page) {
        Map<Integer, String> pageNamesMap = getCurrentNameMap();

        if (pageNamesMap == null) return I18n.get("feature.wynntils.personalStorageUtilities.page", page);

        return pageNamesMap.getOrDefault(page, I18n.get("feature.wynntils.personalStorageUtilities.page", page));
    }

    public void saveCurrentPageName(String nameToSet) {
        switch (storageContainerType) {
            case ACCOUNT_BANK -> {
                customAccountBankPageNames.get().put(currentPage, nameToSet);
                customAccountBankPageNames.touched();
            }
            case BLOCK_BANK -> {
                customBlockBankPageNames.get().put(currentPage, nameToSet);
                customBlockBankPageNames.touched();
            }
            case BOOKSHELF -> {
                customBookshelfPageNames.get().put(currentPage, nameToSet);
                customBookshelfPageNames.touched();
            }
            case CHARACTER_BANK -> {
                customCharacterBankPagesNames.get().putIfAbsent(Models.Character.getId(), new TreeMap<>());

                Map<Integer, String> nameMap =
                        customCharacterBankPagesNames.get().get(Models.Character.getId());

                nameMap.put(currentPage, nameToSet);

                customCharacterBankPagesNames.get().put(Models.Character.getId(), nameMap);
                customCharacterBankPagesNames.touched();
            }
            case MISC_BUCKET -> {
                customMiscBucketPageNames.get().put(currentPage, nameToSet);
                customMiscBucketPageNames.touched();
            }
        }

        editingName = false;
    }

    public void resetCurrentPageName() {
        switch (storageContainerType) {
            case ACCOUNT_BANK -> {
                customAccountBankPageNames.get().remove(currentPage);
                customAccountBankPageNames.touched();
            }
            case BLOCK_BANK -> {
                customBlockBankPageNames.get().remove(currentPage);
                customBlockBankPageNames.touched();
            }
            case BOOKSHELF -> {
                customBookshelfPageNames.get().remove(currentPage);
                customBookshelfPageNames.touched();
            }
            case CHARACTER_BANK -> {
                customCharacterBankPagesNames
                        .get()
                        .getOrDefault(Models.Character.getId(), new TreeMap<>())
                        .remove(currentPage);
                customCharacterBankPagesNames.touched();
            }
            case MISC_BUCKET -> {
                customMiscBucketPageNames.get().remove(currentPage);
                customMiscBucketPageNames.touched();
            }
        }

        editingName = false;
    }

    public int getFinalPage() {
        return switch (storageContainerType) {
            case ACCOUNT_BANK -> finalAccountBankPage.get();
            case BLOCK_BANK -> finalBlockBankPage.get();
            case BOOKSHELF -> finalBookshelfPage.get();
            case CHARACTER_BANK -> finalCharacterBankPages
                    .get()
                    .getOrDefault(Models.Character.getId(), MAX_CHARACTER_BANK_PAGES);
            case MISC_BUCKET -> finalMiscBucketPage.get();
        };
    }

    public PersonalStorageType getStorageContainerType() {
        return storageContainerType;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public boolean isEditingName() {
        return editingName;
    }

    public void toggleEditingName(boolean editingName) {
        this.editingName = editingName;
    }

    private boolean isItemIndicatingLastBankPage(ItemStack item) {
        return StyledText.fromComponent(item.getHoverName()).endsWith(LAST_BANK_PAGE_STRING)
                || item.getHoverName().getString().equals(FINAL_PAGE_NAME);
    }

    private void updateFinalPage() {
        switch (storageContainerType) {
            case ACCOUNT_BANK -> {
                finalAccountBankPage.store(currentPage);
            }
            case BLOCK_BANK -> {
                if (currentPage > finalBlockBankPage.get()) {
                    finalBlockBankPage.store(currentPage);
                }
            }
            case BOOKSHELF -> {
                finalBookshelfPage.store(currentPage);
            }
            case CHARACTER_BANK -> {
                finalCharacterBankPages.get().put(Models.Character.getId(), currentPage);
                finalCharacterBankPages.touched();
            }
            case MISC_BUCKET -> {
                finalMiscBucketPage.store(currentPage);
            }
        }
    }

    private Map<Integer, String> getCurrentNameMap() {
        return switch (storageContainerType) {
            case ACCOUNT_BANK -> customAccountBankPageNames.get();
            case BLOCK_BANK -> customBlockBankPageNames.get();
            case BOOKSHELF -> customBookshelfPageNames.get();
            case CHARACTER_BANK -> customCharacterBankPagesNames
                    .get()
                    .getOrDefault(Models.Character.getId(), new TreeMap<>());
            case MISC_BUCKET -> customMiscBucketPageNames.get();
        };
    }
}
