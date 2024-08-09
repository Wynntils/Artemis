/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.mc.event.ScreenInitEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.models.containers.Container;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import com.wynntils.models.containers.containers.personal.PersonalStorageContainer;
import com.wynntils.models.containers.type.HighlightableProfessionProperty;
import com.wynntils.models.items.properties.ProfessionItemProperty;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.UI)
public class ProfessionHighlightFeature extends Feature {
    @Persisted
    private final Config<CustomColor> highlightColor = new Config<>(new CustomColor(20, 89, 204));

    @Persisted
    private final Config<HighlightSelectionMode> selectionMode = new Config<>(HighlightSelectionMode.PER_CONTAINER);

    @Persisted
    private final Storage<ProfessionType> selectedProfession = new Storage<>(null);

    @Persisted
    private final Storage<Map<String, ProfessionType>> selectionPerContainer = new Storage<>(new TreeMap<>());

    @SubscribeEvent
    public void onScreenInit(ScreenInitEvent event) {
        Screen screen = event.getScreen();

        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;

        if (!(Models.Container.getCurrentContainer() instanceof HighlightableProfessionProperty)) {
            return;
        }

        // When in global mode, if any prof was selected and a crafting station is opened, then set the selected type to
        // that crafting station type
        if (selectedProfession.get() != null
                && selectionMode.get() == HighlightSelectionMode.GLOBAL
                && Models.Container.getCurrentContainer() instanceof CraftingStationContainer craftingStation) {
            setSelectedProfession(craftingStation.getProfessionType());
        }

        // This is screen.topPos and screen.leftPos, but they are not calculated yet when this is called
        int renderX = (screen.width - containerScreen.imageWidth) / 2;
        int renderY = (screen.height - containerScreen.imageHeight) / 2;

        addWidgets(containerScreen, renderX, renderY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderSlot(SlotRenderEvent.Pre event) {
        Screen screen = event.getScreen();

        if (!(screen instanceof AbstractContainerScreen<?>)) return;

        if (!(Models.Container.getCurrentContainer()
                instanceof HighlightableProfessionProperty highlightableProfessionProperty)) {
            return;
        }

        Slot slot = event.getSlot();

        if (!highlightableProfessionProperty.getBounds().getSlots().contains(slot.index)) {
            return;
        }

        Optional<ProfessionItemProperty> professionItemPropertyOpt =
                Models.Item.asWynnItemProperty(slot.getItem(), ProfessionItemProperty.class);
        if (professionItemPropertyOpt.isEmpty()) return;

        ProfessionType selectedProfession = getSelectedProfession();

        // Return if the profession is not selected, or the profession is not in the list of professions
        if (selectedProfession == null) return;
        if (!professionItemPropertyOpt.get().getProfessionTypes().contains(selectedProfession)) return;

        RenderSystem.enableDepthTest();

        RenderUtils.drawTexturedRectWithColor(
                event.getPoseStack(),
                Texture.HIGHLIGHT.resource(),
                highlightColor.get(),
                slot.x - 1,
                slot.y - 1,
                201,
                18,
                18,
                Texture.HIGHLIGHT.width(),
                Texture.HIGHLIGHT.height());

        RenderSystem.disableDepthTest();
    }

    public void setSelectedProfession(ProfessionType professionType) {
        if (selectionMode.get() == HighlightSelectionMode.GLOBAL) {
            selectedProfession.store(professionType);
            return;
        }

        Container currentContainer = Models.Container.getCurrentContainer();
        if (currentContainer == null) return;

        Map<String, ProfessionType> containerSelection = selectionPerContainer.get();
        containerSelection.put(currentContainer.getContainerName(), professionType);

        selectionPerContainer.touched();
    }

    public ProfessionType getSelectedProfession() {
        if (selectionMode.get() == HighlightSelectionMode.GLOBAL) {
            return selectedProfession.get();
        }

        Container currentContainer = Models.Container.getCurrentContainer();
        if (currentContainer == null) return null;

        Map<String, ProfessionType> containerSelection = selectionPerContainer.get();
        return containerSelection.get(currentContainer.getContainerName());
    }

    private void addWidgets(AbstractContainerScreen<?> containerScreen, int renderX, int renderY) {
        renderX += containerScreen.imageWidth + 2;

        if (Models.Container.getCurrentContainer() instanceof PersonalStorageContainer) {
            // Personal storage container textures extend past the normal renderX
            // so the widgets need to be shifted more to the right
            renderX += 10;
        }

        // Add the button to the top right side of the container
        containerScreen.addRenderableWidget(new ProfessionHighlightButton(renderX, renderY, 20, 20));
    }

    private static final class ProfessionHighlightButton extends WynntilsButton {
        private ProfessionHighlightButton(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        @Override
        public Component getMessage() {
            ProfessionHighlightFeature feature = Managers.Feature.getFeatureInstance(ProfessionHighlightFeature.class);

            ProfessionType profession = feature.getSelectedProfession();

            if (profession == null) {
                return Component.literal("-");
            } else {
                return Component.literal(profession.getProfessionIconChar());
            }
        }

        @Override
        public void setFocused(boolean focused) {
            // We don't want the button to be focused, ever
            // (this is weird quirk of the vanilla button system)
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY)) return false;

            ProfessionHighlightFeature feature = Managers.Feature.getFeatureInstance(ProfessionHighlightFeature.class);

            // Shift click resets the profession type
            if (KeyboardUtils.isShiftDown()) {
                feature.setSelectedProfession(null);
                return true;
            }

            ProfessionType profession = feature.getSelectedProfession();

            // For crafting stations, the only option should be the profession type for the station
            if (Models.Container.getCurrentContainer() instanceof CraftingStationContainer craftingStation) {
                if (profession == null) {
                    profession = craftingStation.getProfessionType();
                } else {
                    profession = null;
                }

                feature.setSelectedProfession(profession);

                return true;
            }

            // Left click increases the profession type
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (profession == null) {
                    profession = ProfessionType.craftingProfessionTypes().getFirst();
                } else {
                    int nextIndex = (ProfessionType.craftingProfessionTypes().indexOf(profession) + 1)
                            % ProfessionType.craftingProfessionTypes().size();

                    if (nextIndex == 0) {
                        profession = null;
                    } else {
                        profession = ProfessionType.craftingProfessionTypes().get(nextIndex);
                    }
                }

                feature.setSelectedProfession(profession);
                return true;
            }

            // Right click decreases the profession type
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                if (profession == null) {
                    profession = ProfessionType.craftingProfessionTypes().getLast();
                } else {
                    int nextIndex = (ProfessionType.craftingProfessionTypes().indexOf(profession) - 1)
                            % ProfessionType.craftingProfessionTypes().size();

                    if (nextIndex == -1) {
                        profession = null;
                    } else {
                        profession = ProfessionType.craftingProfessionTypes().get(nextIndex);
                    }
                }

                feature.setSelectedProfession(profession);
                return true;
            }

            return false;
        }

        @Override
        public void onPress() {}
    }

    private enum HighlightSelectionMode {
        PER_CONTAINER,
        GLOBAL
    }
}
