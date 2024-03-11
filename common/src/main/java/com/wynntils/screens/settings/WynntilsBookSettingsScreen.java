/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Configurable;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.overlays.CustomNameProperty;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.screens.WynntilsScreen;
import com.wynntils.core.persisted.Translatable;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.base.TextboxScreen;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.screens.base.widgets.SearchWidget;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.settings.widgets.CategoryButton;
import com.wynntils.screens.settings.widgets.ConfigTile;
import com.wynntils.screens.settings.widgets.ConfigurableButton;
import com.wynntils.screens.settings.widgets.SettingsCategoryTabButton;
import com.wynntils.screens.settings.widgets.SettingsPageTabButton;
import com.wynntils.screens.settings.widgets.SettingsSideTabButton;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class WynntilsBookSettingsScreen extends WynntilsScreen implements TextboxScreen {
    // Constants
    private static final int CONFIGURABLES_PER_PAGE = 17;
    private static final int CONFIGS_PER_PAGE = 4;
    private static final int MAX_DISPLAYED_CATEGORIES = 7;

    // Collections
    private final List<Category> sortedCategories;
    private final List<WynntilsButton> configurables = new ArrayList<>();
    private final List<WynntilsButton> configs = new ArrayList<>();
    private List<Configurable> configurableList;
    private List<SettingsCategoryTabButton> categoryButtons = new ArrayList<>();

    // Renderables
    private final SearchWidget searchWidget;
    private SettingsCategoryTabButton allCategoriesButton;
    private SettingsCategoryTabButton selectedCategoryButton;
    private TextInputBoxWidget focusedTextInput;

    // UI size, positions, etc
    private boolean draggingConfigurableScroll = false;
    private boolean draggingConfigScroll = false;
    private double currentUnusedConfigurableScroll = 0;
    private double currentUnusedConfigScroll = 0;
    private float configurableScrollRenderY;
    private float configScrollRenderY;
    private float translationX;
    private float translationY;
    private int categoriesScrollOffset = 0;
    private int configurablesScrollOffset = 0;
    private int configScrollOffset = 0;
    private int displayedHeaders = 0;

    // Settings display
    private Category selectedCategory;
    private Configurable selectedConfigurable;

    private WynntilsBookSettingsScreen() {
        super(Component.translatable("screens.wynntils.settingsScreen.name"));

        searchWidget = new SearchWidget(
                60,
                Texture.CONFIG_BOOK_BACKGROUND.height() + 5,
                100,
                20,
                (s) -> {
                    configurablesScrollOffset = 0;
                    getFilteredConfigurables();
                    populateConfigurables();
                },
                this);

        setFocusedTextInput(searchWidget);

        // Get all categories, sort a-z
        sortedCategories = Arrays.asList(Category.values());
        sortedCategories.sort(Comparator.comparing(Enum::name));
    }

    public static Screen create() {
        return new WynntilsBookSettingsScreen();
    }

    @Override
    protected void doInit() {
        populateCategories();
        getFilteredConfigurables();
        populateConfigurables();
        // Render position for the book background
        translationX = (this.width - Texture.CONFIG_BOOK_BACKGROUND.width()) / 2f;
        translationY = (this.height - Texture.CONFIG_BOOK_BACKGROUND.height()) / 2f;

        int yPos = Texture.TAG_BLUE.height() / 2;

        // region Side tags
        this.addRenderableWidget(new SettingsSideTabButton(
                (int) -(Texture.TAG_BLUE.width() * 0.75f),
                yPos,
                Texture.TAG_BLUE.width(),
                Texture.TAG_BLUE.height(),
                this::importSettings,
                ComponentUtils.wrapTooltips(
                        List.of(
                                Component.translatable("screens.wynntils.settingsScreen.import")
                                        .withStyle(ChatFormatting.YELLOW),
                                Component.translatable("screens.wynntils.settingsScreen.import.all")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.translatable("screens.wynntils.settingsScreen.import.selected")
                                        .withStyle(ChatFormatting.GRAY)),
                        150),
                Texture.TAG_BLUE,
                Texture.ADD_ICON));

        yPos += 15 + Texture.TAG_BLUE.height() / 2;

        this.addRenderableWidget(new SettingsSideTabButton(
                (int) -(Texture.TAG_BLUE.width() * 0.75f),
                yPos,
                Texture.TAG_BLUE.width(),
                Texture.TAG_BLUE.height(),
                this::exportSettings,
                ComponentUtils.wrapTooltips(
                        List.of(
                                Component.translatable("screens.wynntils.settingsScreen.export")
                                        .withStyle(ChatFormatting.BLUE),
                                Component.translatable("screens.wynntils.settingsScreen.export.all")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.translatable("screens.wynntils.settingsScreen.export.selected")
                                        .withStyle(ChatFormatting.GRAY)),
                        150),
                Texture.TAG_BLUE,
                Texture.SHARE_ICON));

        yPos += 15 + Texture.TAG_BLUE.height() / 2;

        this.addRenderableWidget(new SettingsSideTabButton(
                (int) -(Texture.TAG_BLUE.width() * 0.75f),
                yPos,
                Texture.TAG_BLUE.width(),
                Texture.TAG_BLUE.height(),
                (b) -> {
                    Managers.Config.saveConfig();
                    onClose();
                },
                ComponentUtils.wrapTooltips(
                        List.of(
                                Component.translatable("screens.wynntils.settingsScreen.apply")
                                        .withStyle(ChatFormatting.GREEN),
                                Component.translatable("screens.wynntils.settingsScreen.apply.description")
                                        .withStyle(ChatFormatting.GRAY)),
                        150),
                Texture.TAG_BLUE,
                Texture.SAVE));

        yPos += 15 + Texture.TAG_BLUE.height() / 2;

        this.addRenderableWidget(new SettingsSideTabButton(
                (int) -(Texture.TAG_BLUE.width() * 0.75f),
                yPos,
                Texture.TAG_BLUE.width(),
                Texture.TAG_BLUE.height(),
                (b) -> onClose(),
                ComponentUtils.wrapTooltips(
                        List.of(
                                Component.translatable("screens.wynntils.settingsScreen.close")
                                        .withStyle(ChatFormatting.RED),
                                Component.translatable("screens.wynntils.settingsScreen.close.description")
                                        .withStyle(ChatFormatting.GRAY)),
                        150),
                Texture.TAG_BLUE,
                Texture.CLOSE));
        // endregion

        // region Category tags
        int xPos = 18;

        allCategoriesButton = this.addRenderableWidget(new SettingsCategoryTabButton(
                18,
                (int) -(Texture.TAG_RED.height() * 0.75f),
                Texture.TAG_RED.width(),
                Texture.TAG_RED.height(),
                (b) -> changeCategory(null),
                List.of(Component.literal("All")),
                selectedCategory == null));

        if (selectedCategory == null) {
            selectedCategoryButton = allCategoriesButton;
        }

        xPos += Texture.TAG_RED.width() + Texture.TAG_RED.width() * 0.75;

        this.addRenderableWidget(new SettingsPageTabButton(
                xPos,
                (int) -(Texture.TAG_RED.height() * 0.75f),
                Texture.TAG_RED.width(),
                Texture.TAG_RED.height(),
                (b) -> scrollCategorories(-1),
                List.of(Component.translatable("screens.wynntils.settingsScreen.previous")),
                false));

        xPos += (Texture.TAG_RED.width() + Texture.TAG_RED.width() * 0.75) * (MAX_DISPLAYED_CATEGORIES + 1);

        this.addRenderableWidget(new SettingsPageTabButton(
                xPos,
                (int) -(Texture.TAG_RED.height() * 0.75f),
                Texture.TAG_RED.width(),
                Texture.TAG_RED.height(),
                (b) -> scrollCategorories(1),
                List.of(Component.translatable("screens.wynntils.settingsScreen.next")),
                true));

        this.addRenderableWidget(searchWidget);
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();
        poseStack.translate(translationX, translationY, 0);

        renderSearchBar(poseStack);

        renderTags(guiGraphics, mouseX, mouseY, partialTick);

        renderBg(poseStack);

        String categoryName = selectedCategory == null
                ? I18n.get("screens.wynntils.settingsScreen.all")
                : I18n.get(selectedCategory.toString());

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(categoryName),
                        Texture.CONFIG_BOOK_BACKGROUND.width() * 0.25f,
                        McUtils.mc().font.lineHeight + 5,
                        CommonColors.LIGHT_GRAY,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);

        RenderUtils.drawLine(
                poseStack, CommonColors.GRAY, 11, 19, Texture.CONFIG_BOOK_BACKGROUND.width() / 2f - 6, 19, 0, 1);

        if (selectedConfigurable != null) {
            String textToRender = selectedConfigurable.getTranslatedName();

            // Show the custom name for info boxes/custom bars if given
            if (selectedConfigurable instanceof CustomNameProperty customNameProperty) {
                if (!customNameProperty.getCustomName().get().isEmpty()) {
                    textToRender = customNameProperty.getCustomName().get();
                }
            }

            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromString(textToRender),
                            Texture.CONFIG_BOOK_BACKGROUND.width() * 0.75f,
                            McUtils.mc().font.lineHeight + 5,
                            CommonColors.LIGHT_GRAY,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            TextShadow.NORMAL);

            RenderUtils.drawLine(
                    poseStack,
                    CommonColors.GRAY,
                    Texture.CONFIG_BOOK_BACKGROUND.width() / 2f + 6,
                    19,
                    Texture.CONFIG_BOOK_BACKGROUND.width() - 11,
                    19,
                    0,
                    1);
        } else {
            FontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            StyledText.fromComponent(
                                    Component.translatable("screens.wynntils.settingsScreen.unselectedConfig")),
                            Texture.CONFIG_BOOK_BACKGROUND.width() / 2f,
                            Texture.CONFIG_BOOK_BACKGROUND.width(),
                            Texture.CONFIG_BOOK_BACKGROUND.height() * 0.25f,
                            Texture.CONFIG_BOOK_BACKGROUND.height() * 0.75f,
                            Texture.CONFIG_BOOK_BACKGROUND.width() / 3f,
                            CommonColors.WHITE,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.TOP,
                            TextShadow.NORMAL);
        }

        if ((configurableList.size() + displayedHeaders) > CONFIGURABLES_PER_PAGE) {
            renderConfigurableScroll(poseStack);
        }

        if (selectedConfigurable != null
                && selectedConfigurable.getVisibleConfigOptions().size() > CONFIGS_PER_PAGE) {
            renderConfigScroll(poseStack);
        }

        renderConfigs(guiGraphics, mouseX, mouseY, partialTick);

        renderConfigurables(guiGraphics, mouseX, mouseY, partialTick);

        renderTooltips(guiGraphics, mouseX, mouseY);

        poseStack.popPose();
    }

    @Override
    public void added() {
        searchWidget.opened();
        super.added();
    }

    @Override
    public void onClose() {
        Managers.Config.reloadConfiguration();
        super.onClose();
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        double adjustedMouseX = mouseX - translationX;
        double adjustedMouseY = mouseY - translationY;

        if (!draggingConfigurableScroll && (configurableList.size() + displayedHeaders) > CONFIGURABLES_PER_PAGE) {
            if (MathUtils.isInside(
                    (int) adjustedMouseX,
                    (int) adjustedMouseY,
                    (int) (Texture.CONFIG_BOOK_BACKGROUND.width() / 2f - 12),
                    (int) ((Texture.CONFIG_BOOK_BACKGROUND.width() / 2f - 12)
                            + Texture.CONFIG_BOOK_SCROLL_BUTTON.width()),
                    (int) configurableScrollRenderY,
                    (int) (configurableScrollRenderY + Texture.CONFIG_BOOK_SCROLL_BUTTON.height() / 2f))) {
                draggingConfigurableScroll = true;

                return true;
            }
        }

        if (!draggingConfigScroll
                && selectedConfigurable != null
                && selectedConfigurable.getVisibleConfigOptions().size() > CONFIGS_PER_PAGE) {
            if (MathUtils.isInside(
                    (int) adjustedMouseX,
                    (int) adjustedMouseY,
                    Texture.CONFIG_BOOK_BACKGROUND.width() - 17,
                    (Texture.CONFIG_BOOK_BACKGROUND.width() - 17) + Texture.CONFIG_BOOK_SCROLL_BUTTON.width(),
                    (int) configScrollRenderY,
                    (int) (configScrollRenderY + Texture.CONFIG_BOOK_SCROLL_BUTTON.height() / 2f))) {
                draggingConfigScroll = true;

                return true;
            }
        }

        for (GuiEventListener listener : getWidgetsForIteration().toList()) {
            if (listener.isMouseOver(adjustedMouseX, adjustedMouseY)) {
                // Tags have a slight bit rendered underneath the book but we don't want that part to be
                // clickable
                if (listener instanceof SettingsCategoryTabButton || listener instanceof SettingsSideTabButton) {
                    if (adjustedMouseX >= 0 && adjustedMouseY >= 0) {
                        return false;
                    }
                }

                return listener.mouseClicked(adjustedMouseX, adjustedMouseY, button);
            }
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double adjustedMouseX = mouseX - translationX;
        double adjustedMouseY = mouseY - translationY;

        if (draggingConfigurableScroll) {
            int renderY = 21;
            int scrollAreaStartY = renderY + 7;

            int newValue = Math.round(MathUtils.map(
                    (float) adjustedMouseY,
                    scrollAreaStartY,
                    scrollAreaStartY + CONFIGURABLES_PER_PAGE * 11 - Texture.CONFIG_BOOK_SCROLL_BUTTON.height() / 2f,
                    0,
                    Math.max(0, getMaxScrollConfigurablesOffset())));

            scrollConfigurables(newValue - configurablesScrollOffset);

            return super.mouseDragged(adjustedMouseX, adjustedMouseY, button, dragX, dragY);
        } else if (draggingConfigScroll) {
            int renderY = 21;
            int scrollAreaStartY = renderY + 7;

            int newValue = Math.round(MathUtils.map(
                    (float) adjustedMouseY,
                    scrollAreaStartY,
                    scrollAreaStartY + CONFIGURABLES_PER_PAGE * 11 - Texture.CONFIG_BOOK_SCROLL_BUTTON.height() / 2f,
                    0,
                    Math.max(0, selectedConfigurable.getVisibleConfigOptions().size() - CONFIGS_PER_PAGE)));

            scrollConfigs(newValue - configScrollOffset);

            return super.mouseDragged(adjustedMouseX, adjustedMouseY, button, dragX, dragY);
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double adjustedMouseX = mouseX - translationX;
        double adjustedMouseY = mouseY - translationY;

        for (GuiEventListener listener : getWidgetsForIteration().toList()) {
            if (listener.isMouseOver(adjustedMouseX, adjustedMouseY)) {
                listener.mouseReleased(adjustedMouseX, adjustedMouseY, button);
            }
        }

        draggingConfigurableScroll = false;
        draggingConfigScroll = false;

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double adjustedMouseX = mouseX - translationX;
        double scrollValue = -Math.signum(deltaY);

        // When mouse above the book, scroll the categories.
        // When below top of book and left side scroll configurables
        // Otherwise scroll configs
        if (mouseY <= translationY) {
            scrollCategorories((int) scrollValue);
        } else if (adjustedMouseX <= Texture.CONFIG_BOOK_BACKGROUND.width() / 2f) {
            if (Math.abs(deltaY) == 1.0) {
                scrollConfigurables((int) -deltaY);
                return true;
            }

            // Account for scrollpad
            currentUnusedConfigurableScroll -= deltaY / 5d;

            if (Math.abs(currentUnusedConfigurableScroll) < 1) return true;

            int scroll = (int) (currentUnusedConfigurableScroll);
            currentUnusedConfigurableScroll = currentUnusedConfigurableScroll % 1;

            scrollConfigurables(scroll);
        } else if (selectedConfigurable != null
                && selectedConfigurable.getVisibleConfigOptions().size() > CONFIGS_PER_PAGE) {
            if (Math.abs(scrollValue) == 1.0) {
                scrollConfigs((int) -deltaY);
                return true;
            }

            // Account for scrollpad
            currentUnusedConfigScroll -= deltaY / 5d;

            if (Math.abs(currentUnusedConfigScroll) < 1) return true;

            int scroll = (int) (currentUnusedConfigScroll);
            currentUnusedConfigScroll = currentUnusedConfigScroll % 1;

            scrollConfigs(scroll);
        }

        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return focusedTextInput != null && focusedTextInput.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        return focusedTextInput != null && focusedTextInput.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public TextInputBoxWidget getFocusedTextInput() {
        return focusedTextInput;
    }

    @Override
    public void setFocusedTextInput(TextInputBoxWidget focusedTextInput) {
        this.focusedTextInput = focusedTextInput;
    }

    public void populateConfigurables() {
        configurables.clear();

        Category currentCategory = selectedCategory;
        int currentConfigurable;
        int renderY = 21;

        displayedHeaders = 0;

        for (int i = 0; (i + displayedHeaders) < CONFIGURABLES_PER_PAGE; i++) {
            currentConfigurable = i + configurablesScrollOffset;

            if (currentConfigurable > configurableList.size() - 1) {
                break;
            }

            Configurable configurable = configurableList.get(currentConfigurable);
            Category category = getCategory(configurable);

            if (category != currentCategory) {
                configurables.add(new CategoryButton(12, renderY, 140, 10, category));
                renderY += 11;
                currentCategory = category;
                displayedHeaders++;

                if ((i + displayedHeaders) >= CONFIGURABLES_PER_PAGE) {
                    break;
                }
            }

            int matchingConfigs = 0;

            for (Config<?> config : configurable.getVisibleConfigOptions()) {
                if (configOptionContains(config)) {
                    matchingConfigs++;
                }
            }

            configurables.add(new ConfigurableButton(
                    12,
                    renderY,
                    (int) (Texture.CONFIG_BOOK_BACKGROUND.width() / 2f - 29),
                    10,
                    configurable,
                    matchingConfigs,
                    translationX,
                    translationY));

            renderY += 11;
        }

        if (selectedConfigurable != null) {
            Stream<Configurable> configurablesList = Stream.concat(
                    Managers.Feature.getFeatures().stream(),
                    Managers.Feature.getFeatures().stream()
                            .map(Managers.Overlay::getFeatureOverlays)
                            .flatMap(Collection::stream)
                            .map(overlay -> (Configurable) overlay));

            Configurable newSelected = configurablesList
                    .filter(configurable -> configurable.getJsonName().equals(selectedConfigurable.getJsonName()))
                    .findFirst()
                    .orElse(null);

            setSelected(newSelected);
        }
    }

    public void populateConfigs() {
        configs.clear();

        if (selectedConfigurable == null) return;

        List<Config<?>> configsOptions = selectedConfigurable.getVisibleConfigOptions().stream()
                .sorted(Comparator.comparing(config -> !Objects.equals(config.getFieldName(), "userEnabled")))
                .toList();

        int currentConfig;
        int renderY = 21;

        for (int i = 0; i < CONFIGS_PER_PAGE; i++) {
            currentConfig = i + configScrollOffset;

            if (currentConfig > configsOptions.size() - 1) {
                break;
            }

            Config<?> config = configsOptions.get(currentConfig);

            configs.add(new ConfigTile(
                    Texture.CONFIG_BOOK_BACKGROUND.width() / 2 + 10,
                    renderY,
                    160,
                    45,
                    this,
                    config,
                    translationX,
                    translationY));

            renderY += 46;
        }
    }

    public boolean configOptionContains(Config<?> config) {
        return !searchWidget.getTextBoxInput().isEmpty()
                && StringUtils.containsIgnoreCase(config.getDisplayName(), searchWidget.getTextBoxInput());
    }

    private void populateCategories() {
        for (AbstractWidget widget : categoryButtons) {
            this.removeWidget(widget);
        }

        int xPos = (int) (18 + Texture.TAG_RED.width() + Texture.TAG_RED.width() * 0.75);

        categoryButtons = new ArrayList<>();

        for (int i = 0; i < MAX_DISPLAYED_CATEGORIES; i++) {
            xPos += Texture.TAG_RED.width() + Texture.TAG_RED.width() * 0.75;

            int categoryIndex;

            if (i + categoriesScrollOffset < 0) {
                categoryIndex = (i + categoriesScrollOffset) + sortedCategories.size();
            } else if (i + categoriesScrollOffset > sortedCategories.size() - 1) {
                categoryIndex = (i + categoriesScrollOffset) - sortedCategories.size();
            } else {
                categoryIndex = (i + categoriesScrollOffset);
            }

            Category category = sortedCategories.get(categoryIndex);

            categoryButtons.add(this.addRenderableWidget(new SettingsCategoryTabButton(
                    xPos,
                    (int) -(Texture.TAG_RED.height() * 0.75f),
                    Texture.TAG_RED.width(),
                    Texture.TAG_RED.height(),
                    (b) -> changeCategory(category),
                    List.of(Component.literal(I18n.get(category.toString()))),
                    category,
                    selectedCategory == category)));
        }
    }

    private void getFilteredConfigurables() {
        configurableList = new ArrayList<>();

        List<Configurable> filteredConfigurables;

        // Add all configurables for selected category
        if (selectedCategory != null) {
            filteredConfigurables = Managers.Feature.getFeatures().stream()
                    .filter(feature -> searchMatches(feature)
                            || feature.getVisibleConfigOptions().stream().anyMatch(this::configOptionContains))
                    .map(feature -> (Configurable) feature)
                    .sorted()
                    .filter(configurable -> isCategoryMatching(configurable, selectedCategory))
                    .collect(Collectors.toList());

            filteredConfigurables.addAll(Managers.Overlay.getOverlays().stream()
                    .filter(overlay -> !filteredConfigurables.contains(Managers.Overlay.getOverlayParent(overlay)))
                    .filter(overlay -> searchMatches(overlay)
                            || overlay.getVisibleConfigOptions().stream().anyMatch(this::configOptionContains))
                    .sorted()
                    .filter(configurable -> isCategoryMatching(configurable, selectedCategory))
                    .toList());

            // If there is a search query, add all matches from every other category
            if (!searchWidget.getTextBoxInput().isEmpty()) {
                filteredConfigurables.addAll(Managers.Feature.getFeatures().stream()
                        .filter(feature -> searchMatches(feature)
                                || feature.getVisibleConfigOptions().stream().anyMatch(this::configOptionContains))
                        .map(feature -> (Configurable) feature)
                        .sorted()
                        .filter(configurable -> !isCategoryMatching(configurable, selectedCategory))
                        .toList());

                filteredConfigurables.addAll(Managers.Overlay.getOverlays().stream()
                        .filter(overlay -> !filteredConfigurables.contains(Managers.Overlay.getOverlayParent(overlay)))
                        .filter(overlay -> searchMatches(overlay)
                                || overlay.getVisibleConfigOptions().stream().anyMatch(this::configOptionContains))
                        .sorted()
                        .filter(configurable -> !isCategoryMatching(configurable, selectedCategory))
                        .toList());
            }
        } else { // All tab, add all configurables
            filteredConfigurables = Managers.Feature.getFeatures().stream()
                    .filter(feature -> searchMatches(feature)
                            || feature.getVisibleConfigOptions().stream().anyMatch(this::configOptionContains))
                    .map(feature -> (Configurable) feature)
                    .sorted()
                    .collect(Collectors.toList());

            filteredConfigurables.addAll(Managers.Overlay.getOverlays().stream()
                    .filter(overlay -> !filteredConfigurables.contains(Managers.Overlay.getOverlayParent(overlay)))
                    .filter(overlay -> searchMatches(overlay)
                            || overlay.getVisibleConfigOptions().stream().anyMatch(this::configOptionContains))
                    .sorted()
                    .toList());
        }

        for (Configurable configurable : filteredConfigurables) {
            configurableList.add(configurable);

            if (configurable instanceof Feature feature) {
                configurableList.addAll(Managers.Overlay.getFeatureOverlays(feature).stream()
                        .sorted()
                        .toList());
            }
        }
    }

    private Stream<GuiEventListener> getWidgetsForIteration() {
        return Stream.concat(children.stream(), Stream.concat(configurables.stream(), configs.stream()));
    }

    private void scrollConfigurables(int delta) {
        configurablesScrollOffset =
                MathUtils.clamp(configurablesScrollOffset + delta, 0, Math.max(0, getMaxScrollConfigurablesOffset()));

        populateConfigurables();
    }

    private int getMaxScrollConfigurablesOffset() {
        int maxValue = configurableList.size() - CONFIGURABLES_PER_PAGE + displayedHeaders;

        // If there are less than CONFIGURABLES_PER_PAGE configurables rendered (headers included)
        // then that means we have an empty space at the bottom and need scrolling to work
        // nicely with that
        if (configurables.size() < CONFIGURABLES_PER_PAGE) {
            maxValue += CONFIGURABLES_PER_PAGE - configurables.size();
        }

        return maxValue;
    }

    private void scrollConfigs(int delta) {
        configScrollOffset = MathUtils.clamp(
                configScrollOffset + delta,
                0,
                Math.max(0, selectedConfigurable.getVisibleConfigOptions().size() - CONFIGS_PER_PAGE));

        populateConfigs();
    }

    private void scrollCategorories(int direction) {
        if (Math.abs(categoriesScrollOffset + direction) == sortedCategories.size()) {
            categoriesScrollOffset = 0;
        } else {
            categoriesScrollOffset = MathUtils.clamp(
                    categoriesScrollOffset + direction, -(sortedCategories.size() - 1), (sortedCategories.size() - 1));
        }

        populateCategories();
    }

    private boolean isCategoryMatching(Configurable configurable, Category selectedCategory) {
        return getCategory(configurable) == selectedCategory;
    }

    private void scrollToMatchingConfig() {
        List<Config<?>> configsOptions = selectedConfigurable.getVisibleConfigOptions().stream()
                .sorted(Comparator.comparing(config -> !Objects.equals(config.getFieldName(), "userEnabled")))
                .toList();

        // Find a config that matches current search query and get scroll offset to make that config visible
        for (Config<?> config : configsOptions) {
            if (StringUtils.containsIgnoreCase(config.getDisplayName(), searchWidget.getTextBoxInput())) {
                configScrollOffset = Math.max(0, configsOptions.indexOf(config) - (CONFIGS_PER_PAGE - 1));
                return;
            }
        }
    }

    private Category getCategory(Configurable configurable) {
        if (configurable instanceof Feature feature) {
            return feature.getCategory();
        } else if (configurable instanceof Overlay overlay) {
            return Managers.Overlay.getOverlayParent(overlay).getCategory();
        } else {
            throw new IllegalStateException("Unknown configurable type: " + configurable.getClass());
        }
    }

    private boolean searchMatches(Translatable translatable) {
        // For info boxes and custom bars, we want to search for their custom name if given
        // if there is no match, then check the translated name
        if (translatable instanceof CustomNameProperty customNameProperty) {
            if (StringUtils.partialMatch(customNameProperty.getCustomName().get(), searchWidget.getTextBoxInput())) {
                return true;
            }
        }

        return StringUtils.partialMatch(translatable.getTranslatedName(), searchWidget.getTextBoxInput());
    }

    private void renderBg(PoseStack poseStack) {
        RenderUtils.drawTexturedRect(poseStack, Texture.CONFIG_BOOK_BACKGROUND, 0, 0);
    }

    private void renderSearchBar(PoseStack poseStack) {
        RenderUtils.drawTexturedRect(poseStack, Texture.TAG_SEARCH, 30, Texture.CONFIG_BOOK_BACKGROUND.height() - 2);
    }

    private void renderTags(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int adjustedMouseX = mouseX - (int) translationX;
        int adjustedMouseY = mouseY - (int) translationY;

        for (Renderable renderable : renderables) {
            renderable.render(guiGraphics, adjustedMouseX, adjustedMouseY, partialTick);
        }
    }

    private void renderConfigurables(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int adjustedMouseX = mouseX - (int) translationX;
        int adjustedMouseY = mouseY - (int) translationY;

        for (WynntilsButton configurable : configurables) {
            configurable.render(guiGraphics, adjustedMouseX, adjustedMouseY, partialTick);
        }
    }

    private void renderConfigs(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int adjustedMouseX = mouseX - (int) translationX;
        int adjustedMouseY = mouseY - (int) translationY;

        for (WynntilsButton config : configs) {
            config.render(guiGraphics, adjustedMouseX, adjustedMouseY, partialTick);
        }
    }

    private void renderConfigurableScroll(PoseStack poseStack) {
        RenderUtils.drawRect(
                poseStack,
                CommonColors.GRAY,
                (Texture.CONFIG_BOOK_BACKGROUND.width() / 2f - 12),
                21,
                0,
                Texture.CONFIG_BOOK_SCROLL_BUTTON.width(),
                CONFIGURABLES_PER_PAGE * 11);

        configurableScrollRenderY = 21
                + MathUtils.map(
                        configurablesScrollOffset,
                        0,
                        getMaxScrollConfigurablesOffset(),
                        0,
                        CONFIGURABLES_PER_PAGE * 11 - Texture.CONFIG_BOOK_SCROLL_BUTTON.height() / 2f);

        RenderUtils.drawHoverableTexturedRect(
                poseStack,
                Texture.CONFIG_BOOK_SCROLL_BUTTON,
                (Texture.CONFIG_BOOK_BACKGROUND.width() / 2f - 12),
                configurableScrollRenderY,
                draggingConfigurableScroll);
    }

    private void renderConfigScroll(PoseStack poseStack) {
        RenderUtils.drawRect(
                poseStack,
                CommonColors.GRAY,
                (Texture.CONFIG_BOOK_BACKGROUND.width() - 17),
                21,
                0,
                Texture.CONFIG_BOOK_SCROLL_BUTTON.width(),
                CONFIGURABLES_PER_PAGE * 11);

        configScrollRenderY = 21
                + MathUtils.map(
                        configScrollOffset,
                        0,
                        selectedConfigurable.getVisibleConfigOptions().size() - CONFIGS_PER_PAGE,
                        0,
                        CONFIGURABLES_PER_PAGE * 11 - Texture.CONFIG_BOOK_SCROLL_BUTTON.height() / 2f);

        RenderUtils.drawHoverableTexturedRect(
                poseStack,
                Texture.CONFIG_BOOK_SCROLL_BUTTON,
                (Texture.CONFIG_BOOK_BACKGROUND.width() - 17),
                configScrollRenderY,
                draggingConfigScroll);
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int adjustedMouseX = mouseX - (int) translationX;
        int adjustedMouseY = mouseY - (int) translationY;

        // The tags have a slight bit rendered underneath the book, we don't want to render the tooltip
        // when hovering that bit.
        if (adjustedMouseX >= 0 && adjustedMouseY >= 0) return;

        for (GuiEventListener child : children()) {
            if (child instanceof TooltipProvider tooltipProvider && child.isMouseOver(adjustedMouseX, adjustedMouseY)) {
                guiGraphics.renderComponentTooltip(
                        FontRenderer.getInstance().getFont(),
                        tooltipProvider.getTooltipLines(),
                        adjustedMouseX,
                        adjustedMouseY);
                break;
            }
        }
    }

    public void setSelected(Configurable selected) {
        if (this.selectedConfigurable == selected) return;

        this.selectedConfigurable = selected;

        // If there is a search query, scroll the configs list so that matching config
        // is found unless the configurable itself matches the search query
        if (!searchWidget.getTextBoxInput().isEmpty()) {
            if (searchMatches(selectedConfigurable)) {
                configScrollOffset = 0;
            } else {
                scrollToMatchingConfig();
            }
        } else {
            configScrollOffset = 0;
        }

        populateConfigs();
    }

    public Configurable getSelectedConfigurable() {
        return selectedConfigurable;
    }

    private void changeCategory(Category category) {
        configurablesScrollOffset = 0;

        // Deselect old category, reset texture to default
        if (selectedCategoryButton != null) {
            selectedCategoryButton.setSelectedCategory(false);
        }

        selectedCategory = category;

        // If null, the all categories button was selected.
        // Otherwise find which was selected and update that
        if (category == null) {
            selectedCategoryButton = allCategoriesButton;
            allCategoriesButton.setSelectedCategory(true);
        } else {
            for (SettingsCategoryTabButton settingsTabButton : categoryButtons) {
                if (settingsTabButton.getCategory() == selectedCategory) {
                    selectedCategoryButton = settingsTabButton;
                    settingsTabButton.setSelectedCategory(true);
                    break;
                }
            }
        }

        getFilteredConfigurables();
        populateConfigurables();
        populateCategories();
    }

    private void importSettings(int clicked) {
        String clipboard = McUtils.mc().keyboardHandler.getClipboard();

        if (clicked == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            List<Configurable> configsToImport = Managers.Feature.getFeatures().stream()
                    .map(feature -> (Configurable) feature)
                    .collect(Collectors.toList());

            configsToImport.addAll(Managers.Overlay.getOverlays().stream()
                    .filter(overlay -> !configsToImport.contains(Managers.Overlay.getOverlayParent(overlay)))
                    .toList());

            boolean imported = Managers.Config.importConfig(clipboard, configsToImport);

            if (imported) {
                McUtils.sendMessageToClient(Component.translatable("screens.wynntils.settingsScreen.importedAll")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                McUtils.sendMessageToClient(Component.translatable("screens.wynntils.settingsScreen.import.failed")
                        .withStyle(ChatFormatting.RED));
            }
        } else if (clicked == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (selectedConfigurable != null) {
                boolean imported = Managers.Config.importConfig(clipboard, List.of(selectedConfigurable));

                if (imported) {
                    McUtils.sendMessageToClient(Component.translatable(
                                    "screens.wynntils.settingsScreen.imported",
                                    selectedConfigurable.getTranslatedName())
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    McUtils.sendMessageToClient(Component.translatable("screens.wynntils.settingsScreen.import.failed")
                            .withStyle(ChatFormatting.RED));
                }
            } else {
                McUtils.sendMessageToClient(Component.translatable("screens.wynntils.settingsScreen.needToSelect")
                        .withStyle(ChatFormatting.RED));
            }
        }

        // Repopulate the configurables and configs after importing
        populateConfigurables();
        populateConfigs();
    }

    private void exportSettings(int clicked) {
        String exportedSettings = "";

        if (clicked == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Get all features and overlays into a list
            List<Configurable> featuresToExport = Managers.Feature.getFeatures().stream()
                    .map(feature -> (Configurable) feature)
                    .collect(Collectors.toList());

            featuresToExport.addAll(Managers.Overlay.getOverlays().stream()
                    .filter(overlay -> !featuresToExport.contains(Managers.Overlay.getOverlayParent(overlay)))
                    .toList());

            exportedSettings = Managers.Config.exportConfig(featuresToExport);

            McUtils.sendMessageToClient(Component.translatable("screens.wynntils.settingsScreen.exportedAll")
                    .withStyle(ChatFormatting.GREEN));
        } else if (clicked == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (selectedConfigurable != null) {
                exportedSettings = Managers.Config.exportConfig(List.of(selectedConfigurable));

                McUtils.sendMessageToClient(Component.translatable(
                                "screens.wynntils.settingsScreen.exported", selectedConfigurable.getTranslatedName())
                        .withStyle(ChatFormatting.GREEN));
            } else {
                McUtils.sendMessageToClient(Component.translatable("screens.wynntils.settingsScreen.needToSelect")
                        .withStyle(ChatFormatting.RED));
            }
        }

        // Save to clipboard
        McUtils.mc().keyboardHandler.setClipboard(exportedSettings);
    }
}
