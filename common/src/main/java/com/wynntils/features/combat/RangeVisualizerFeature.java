/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.PlayerRenderEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.buffered.CustomRenderType;
import com.wynntils.utils.type.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Position;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;

@ConfigCategory(Category.COMBAT)
public class RangeVisualizerFeature extends Feature {
    private static final MultiBufferSource.BufferSource BUFFER_SOURCE =
            MultiBufferSource.immediate(new ByteBufferBuilder(256));

    // number of straight lines to draw when rendering circle, higher = smoother but more expensive
    private static final int SEGMENTS = 128;
    private static final float HEIGHT = 0.1f;
    private static final int TRANSPARENCY = 95;

    private final Map<Player, List<Pair<CustomColor, Float>>> circlesToRender = new HashMap<>();
    private final Set<Player> detectedPlayers = new HashSet<>();

    @SubscribeEvent
    public void onPlayerRender(PlayerRenderEvent e) {
        AbstractClientPlayer player = e.getPlayer();
        detectedPlayers.add(player);

        List<Pair<CustomColor, Float>> circles = circlesToRender.get(player);
        if (circles == null) return;

        circles.forEach(circleType -> {
            float radius = circleType.b();
            int color = circleType.a().asInt();

            renderCircle(e.getPoseStack(), player.position(), radius, color);
        });
    }

    @SubscribeEvent
    public void onTick(TickEvent e) {
        // Once every tick, calculate circles for all players that were detected (i.e. rendered)
        // since the previous tick
        circlesToRender.clear();
        detectedPlayers.forEach(this::checkMajorIdCircles);
        detectedPlayers.clear();
    }

    private void checkMajorIdCircles(Player player) {
        // Don't render for ghost/npc
        if (!Models.Player.isLocalPlayer(player)) return;

        List<GearInfo> validGear;

        if (player == McUtils.player()) {
            // This is ourselves, rendered from outside

            // Don't render for preview in inventory or character selection screen
            if (McUtils.mc().screen != null && !(McUtils.mc().screen instanceof ChatScreen)) return;

            validGear = Models.CharacterStats.getWornGear();
        } else {
            // Other players must be in party
            if (!Models.Party.getPartyMembers()
                    .contains(StyledText.fromComponent(player.getName()).getStringWithoutFormatting())) return;

            validGear = new ArrayList<>();
            // Check main hand
            GearInfo mainHandGearInfo = getOtherPlayerGearInfo(player.getMainHandItem());
            if (mainHandGearInfo != null) {
                if (mainHandGearInfo.type().isWeapon()) {
                    // We cannot verify class or level :(
                    validGear.add(mainHandGearInfo);
                }
            }

            // Check armor slots
            player.getArmorSlots().forEach(itemStack -> {
                GearInfo armorGearInfo = getOtherPlayerGearInfo(player.getMainHandItem());
                if (armorGearInfo != null) {
                    if (armorGearInfo.type().isArmor()) {
                        validGear.add(armorGearInfo);
                    }
                }
            });

            // Accessory slots are not available to us :(
        }

        // For each valid gear, check all its major IDs, and store them as color/radius pairs
        // Offset the radius slightly so multiple circles can be shown for each player
        // Only a few major IDs can actually be applied at the same time, but we make this general
        List<Pair<CustomColor, Float>> circles = validGear.stream()
                .flatMap(gearInfo -> gearInfo.fixedStats().majorIds().stream().map(majorId -> switch (majorId.name()) {
                    case "Taunt" -> Pair.of(CommonColors.ORANGE.withAlpha(TRANSPARENCY), 12f);
                    case "Saviour’s Sacrifice" -> Pair.of(CommonColors.WHITE.withAlpha(TRANSPARENCY), 8f);
                    case "Heart of the Pack" -> Pair.of(CommonColors.PINK.withAlpha(TRANSPARENCY), 8.1f);
                    case "Guardian" -> Pair.of(CommonColors.RED.withAlpha(TRANSPARENCY), 7.9f);
                    default -> null;
                }))
                .filter(Objects::nonNull)
                .toList();

        if (!circles.isEmpty()) {
            circlesToRender.put(player, circles);
        }
    }

    private GearInfo getOtherPlayerGearInfo(ItemStack itemStack) {
        // This must specifically NOT be normalized; the ֎ is significant
        String gearName = StyledText.fromComponent(itemStack.getHoverName()).getStringWithoutFormatting();
        return Models.Gear.getGearInfoFromApiName(gearName);
    }

    /**
     * Renders a circle with the given radius. Some notes for future reference:<p>
     * - The circle is rendered at the player's feet, from the ground to HEIGHT blocks above the ground.<p>
     * - .color() takes floats from 0-1, but ints from 0-255<p>
     * - Increase SEGMENTS to make the circle smoother, but it will also increase the amount of vertices (and thus the amount of memory used and the amount of time it takes to render)<p>
     * - The order of the consumer.vertex() calls matter. Here, we draw a quad, so we do bottom left corner, top left corner, top right corner, bottom right corner. This is filled in with the color we set.<p>
     *
     * @param poseStack The pose stack to render with. This is supposed to be the pose stack from the event.
     *                  We do the translation here, so no need to do it before passing it in.
     * @param radius
     * @param color
     */
    private void renderCircle(PoseStack poseStack, Position position, float radius, int color) {
        // Circle must be rendered on both sides, otherwise it will be invisible when looking at
        // it from the outside
        RenderSystem.disableCull();

        poseStack.pushPose();
        poseStack.translate(-position.x(), -position.y(), -position.z());
        VertexConsumer consumer = BUFFER_SOURCE.getBuffer(CustomRenderType.POSITION_COLOR_QUAD);

        Matrix4f matrix4f = poseStack.last().pose();
        double angleStep = 2 * Math.PI / SEGMENTS;
        double startingAngle = -(System.currentTimeMillis() % 40000) * 2 * Math.PI / 40000.0;
        double angle = startingAngle;
        for (int i = 0; i < SEGMENTS; i++) {
            if (i % 4 > 2) {
                angle += angleStep;
                continue;
            }
            float x = (float) (position.x() + Math.sin(angle) * radius);
            float z = (float) (position.z() + Math.cos(angle) * radius);
            consumer.addVertex(matrix4f, x, (float) position.y(), z).setColor(color);
            consumer.addVertex(matrix4f, x, (float) position.y() + HEIGHT, z).setColor(color);
            angle += angleStep;
            float x2 = (float) (position.x() + Math.sin(angle) * radius);
            float z2 = (float) (position.z() + Math.cos(angle) * radius);
            consumer.addVertex(matrix4f, x2, (float) position.y() + HEIGHT, z2).setColor(color);
            consumer.addVertex(matrix4f, x2, (float) position.y(), z2).setColor(color);
        }

        BUFFER_SOURCE.endBatch();
        poseStack.popPose();
        RenderSystem.enableCull();
    }
}
