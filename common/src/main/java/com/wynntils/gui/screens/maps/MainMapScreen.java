/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.screens.maps;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.config.ConfigManager;
import com.wynntils.core.webapi.TerritoryManager;
import com.wynntils.features.user.map.MapFeature;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.Texture;
import com.wynntils.gui.widgets.BasicTexturedButton;
import com.wynntils.mc.objects.Location;
import com.wynntils.mc.utils.LocationUtils;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.sockets.model.HadesUserModel;
import com.wynntils.sockets.objects.HadesUser;
import com.wynntils.utils.BoundingBox;
import com.wynntils.utils.KeyboardUtils;
import com.wynntils.wynn.model.CompassModel;
import com.wynntils.wynn.model.map.MapModel;
import com.wynntils.wynn.model.map.poi.CustomPoi;
import com.wynntils.wynn.model.map.poi.IconPoi;
import com.wynntils.wynn.model.map.poi.MapLocation;
import com.wynntils.wynn.model.map.poi.PlayerMainMapPoi;
import com.wynntils.wynn.model.map.poi.Poi;
import com.wynntils.wynn.model.map.poi.TerritoryPoi;
import com.wynntils.wynn.model.map.poi.WaypointPoi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class MainMapScreen extends AbstractMapScreen {
    public MainMapScreen() {
        super();
        centerMapAroundPlayer();
    }

    public MainMapScreen(float mapCenterX, float mapCenterZ) {
        super(mapCenterX, mapCenterZ);
        updateMapCenter(mapCenterX, mapCenterZ);
    }

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(new BasicTexturedButton(
                width / 2 - Texture.MAP_BUTTONS_BACKGROUND.width() / 2 + 6 + 20 * 6,
                (int) (this.renderHeight
                        - this.renderedBorderYOffset
                        - Texture.MAP_BUTTONS_BACKGROUND.height() / 2
                        - 6),
                16,
                16,
                Texture.MAP_HELP_BUTTON,
                (b) -> {},
                List.of(
                        new TextComponent("[>] ")
                                .withStyle(ChatFormatting.YELLOW)
                                .append(new TranslatableComponent("screens.wynntils.map.help.name")),
                        new TextComponent("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(new TranslatableComponent("screens.wynntils.map.help.description1")),
                        new TextComponent("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(new TranslatableComponent("screens.wynntils.map.help.description2")),
                        new TextComponent("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(new TranslatableComponent("screens.wynntils.map.help.description3")),
                        new TextComponent("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(new TranslatableComponent("screens.wynntils.map.help.description4")),
                        new TextComponent("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(new TranslatableComponent("screens.wynntils.map.help.description5")),
                        new TextComponent("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(new TranslatableComponent("screens.wynntils.map.help.description6")),
                        new TextComponent("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(new TranslatableComponent("screens.wynntils.map.help.description7")),
                        new TextComponent("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(new TranslatableComponent("screens.wynntils.map.help.description8")),
                        new TextComponent("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(new TranslatableComponent("screens.wynntils.map.help.description9")))));

        this.addRenderableWidget(new BasicTexturedButton(
                width / 2 - Texture.MAP_BUTTONS_BACKGROUND.width() / 2 + 6 + 20 * 2,
                (int) (this.renderHeight
                        - this.renderedBorderYOffset
                        - Texture.MAP_BUTTONS_BACKGROUND.height() / 2
                        - 6),
                16,
                16,
                Texture.MAP_SHARE_BUTTON,
                this::shareLocationOrCompass,
                List.of(
                        new TextComponent("[>] ")
                                .withStyle(ChatFormatting.DARK_AQUA)
                                .append(new TranslatableComponent("screens.wynntils.map.share.name")),
                        new TranslatableComponent("screens.wynntils.map.share.description1"),
                        new TranslatableComponent("screens.wynntils.map.share.description2"),
                        new TranslatableComponent("screens.wynntils.map.share.description3"))));

        this.addRenderableWidget(new BasicTexturedButton(
                width / 2 - Texture.MAP_BUTTONS_BACKGROUND.width() / 2 + 6 + 20,
                (int) (this.renderHeight
                        - this.renderedBorderYOffset
                        - Texture.MAP_BUTTONS_BACKGROUND.height() / 2
                        - 6),
                16,
                16,
                Texture.MAP_WAYPOINT_FOCUS_BUTTON,
                (b) -> {
                    if (KeyboardUtils.isShiftDown()) {
                        centerMapAroundPlayer();
                        return;
                    }

                    if (CompassModel.getCompassLocation().isPresent()) {
                        Location location = CompassModel.getCompassLocation().get();
                        updateMapCenter((float) location.x, (float) location.z);
                    }
                },
                List.of(
                        new TextComponent("[>] ")
                                .withStyle(ChatFormatting.YELLOW)
                                .append(new TranslatableComponent("screens.wynntils.map.focus.name")),
                        new TextComponent("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(new TranslatableComponent("screens.wynntils.map.focus.description1")),
                        new TextComponent("- ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(new TranslatableComponent("screens.wynntils.map.focus.description2")))));

        this.addRenderableWidget(new BasicTexturedButton(
                width / 2 - Texture.MAP_BUTTONS_BACKGROUND.width() / 2 + 6,
                (int) (this.renderHeight
                        - this.renderedBorderYOffset
                        - Texture.MAP_BUTTONS_BACKGROUND.height() / 2
                        - 6),
                16,
                16,
                Texture.MAP_ADD_BUTTON,
                (b) -> McUtils.mc().setScreen(new PoiCreationScreen(this)),
                List.of(
                        new TextComponent("[>] ")
                                .withStyle(ChatFormatting.DARK_GREEN)
                                .append(new TranslatableComponent("screens.wynntils.map.waypoints.add.name")),
                        new TranslatableComponent("screens.wynntils.map.waypoints.add.description")
                                .withStyle(ChatFormatting.GRAY))));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (holdingMapKey && !MapFeature.INSTANCE.openMapKeybind.getKeyMapping().isDown()) {
            this.onClose();
            return;
        }

        updateMapCenterIfDragging(mouseX, mouseY);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        RenderSystem.enableDepthTest();

        renderMap(poseStack, MapFeature.INSTANCE.renderUsingLinear);

        RenderUtils.enableScissor(
                (int) (renderX + renderedBorderXOffset), (int) (renderY + renderedBorderYOffset), (int) mapWidth, (int)
                        mapHeight);

        renderPois(poseStack, mouseX, mouseY);

        // Cursor
        renderCursor(
                poseStack,
                MapFeature.INSTANCE.playerPointerScale,
                MapFeature.INSTANCE.pointerColor,
                MapFeature.INSTANCE.pointerType);

        RenderSystem.disableScissor();

        renderBackground(poseStack);

        renderCoordinates(poseStack, mouseX, mouseY);

        renderMapButtons(poseStack, mouseX, mouseY, partialTick);
    }

    private void renderPois(PoseStack poseStack, int mouseX, int mouseY) {
        List<Poi> pois = new ArrayList<>();

        pois.addAll(MapModel.getServicePois());
        pois.addAll(MapModel.getLabelPois());

        pois.addAll(MapFeature.INSTANCE.customPois);

        List<HadesUser> renderedPlayers = HadesUserModel.getHadesUserMap().values().stream()
                .filter(
                        hadesUser -> (hadesUser.isPartyMember() && MapFeature.INSTANCE.renderRemotePartyPlayers)
                                || (hadesUser.isMutualFriend() && MapFeature.INSTANCE.renderRemoteFriendPlayers)
                        /*|| (hadesUser.isGuildMember() && MapFeature.INSTANCE.renderRemoteGuildPlayers)*/ )
                .sorted(Comparator.comparing(
                        hadesUser -> hadesUser.getMapLocation().getY()))
                .toList();

        pois.sort(Comparator.comparing(poi -> poi.getLocation().getY()));

        // Make sure compass and player pois are on top
        pois.addAll(renderedPlayers.stream().map(PlayerMainMapPoi::new).toList());
        CompassModel.getCompassWaypoint().ifPresent(pois::add);
        if (KeyboardUtils.isControlDown()) {
            pois.addAll(TerritoryManager.getTerritoryPois());
        }

        renderPois(
                pois,
                poseStack,
                BoundingBox.centered(mapCenterX, mapCenterZ, width / currentZoom, height / currentZoom),
                MapFeature.INSTANCE.poiScale,
                mouseX,
                mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (McUtils.mc().player.isShiftKeyDown()
                    && CompassModel.getCompassLocation().isPresent()) {
                Location location = CompassModel.getCompassLocation().get();
                updateMapCenter((float) location.x, (float) location.z);
                return true;
            }

            centerMapAroundPlayer();
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (hovered instanceof WaypointPoi) {
                CompassModel.reset();
                return true;
            }

            if (hovered != null && !(hovered instanceof TerritoryPoi)) {
                McUtils.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP);
                if (hovered.hasStaticLocation()) {
                    if (hovered instanceof IconPoi iconPoi) {
                        CompassModel.setCompassLocation(new Location(hovered.getLocation()), iconPoi.getIcon());
                    } else {
                        CompassModel.setCompassLocation(new Location(hovered.getLocation()));
                    }
                } else {
                    final Poi finalHovered = hovered;
                    CompassModel.setDynamicCompassLocation(
                            () -> finalHovered.getLocation().asLocation());
                }
                return true;
            }

            super.mouseClicked(mouseX, mouseY, button);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            if (KeyboardUtils.isShiftDown()) {
                if (hovered instanceof CustomPoi customPoi) {
                    McUtils.mc().setScreen(new PoiCreationScreen(this, customPoi));
                } else {
                    int gameX = (int) ((mouseX - centerX) / currentZoom + mapCenterX);
                    int gameZ = (int) ((mouseY - centerZ) / currentZoom + mapCenterZ);

                    McUtils.mc().setScreen(new PoiCreationScreen(this, new MapLocation(gameX, 0, gameZ)));
                }
            } else if (KeyboardUtils.isAltDown()) {
                if (hovered instanceof CustomPoi customPoi) {
                    MapFeature.INSTANCE.customPois.remove(customPoi);
                    ConfigManager.saveConfig();
                }
            } else {
                setCompassToMouseCoords(mouseX, mouseY);
            }
        }

        return true;
    }

    private void setCompassToMouseCoords(double mouseX, double mouseY) {
        double gameX = (mouseX - centerX) / currentZoom + mapCenterX;
        double gameZ = (mouseY - centerZ) / currentZoom + mapCenterZ;
        Location compassLocation = new Location(gameX, 0, gameZ);
        CompassModel.setCompassLocation(compassLocation);

        McUtils.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP);
    }

    private void shareLocationOrCompass(int button) {
        boolean shareCompass =
                KeyboardUtils.isShiftDown() && CompassModel.getCompassLocation().isPresent();

        String target = null;

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            target = "guild";
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            target = "party";
        }

        if (target == null) return;

        if (shareCompass) {
            LocationUtils.shareCompass(target, CompassModel.getCompassLocation().get());
        } else {
            LocationUtils.shareLocation(target);
        }
    }

    public void setHovered(Poi hovered) {
        this.hovered = hovered;
    }
}
