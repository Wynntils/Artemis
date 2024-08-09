/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.overlays.ContainerOverlay;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.scoreboard.event.ScoreboardSegmentAdditionEvent;
import com.wynntils.models.players.event.HadesRelationsUpdateEvent;
import com.wynntils.models.players.event.PartyEvent;
import com.wynntils.models.players.scoreboard.PartyScoreboardPart;
import com.wynntils.services.hades.HadesUser;
import com.wynntils.services.hades.event.HadesUserAddedEvent;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.SkinUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HealthTexture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.ManaTexture;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.CappedValue;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class PartyMembersOverlay extends ContainerOverlay<PartyMembersOverlay.PartyMemberOverlay> {
    private static final HadesUser DUMMY_USER_1 =
            new HadesUser("Player 1", new CappedValue(12432, 13120), new CappedValue(65, 123));
    private static final HadesUser DUMMY_USER_2 =
            new HadesUser("Player 2", new CappedValue(4561, 9870), new CappedValue(98, 170));

    @Persisted
    public final Config<Boolean> disablePartyMembersOnScoreboard = new Config<>(false);

    @Persisted
    public final Config<Integer> maxPartyMembers = new Config<>(4);

    @Persisted
    public final Config<HealthTexture> healthTexture = new Config<>(HealthTexture.A);

    @Persisted
    public final Config<ManaTexture> manaTexture = new Config<>(ManaTexture.A);

    public PartyMembersOverlay() {
        super(
                new OverlayPosition(
                        70,
                        5,
                        VerticalAlignment.TOP,
                        HorizontalAlignment.LEFT,
                        OverlayPosition.AnchorSection.MIDDLE_LEFT),
                new OverlaySize(162, 50),
                ContainerOverlay.GrowDirection.DOWN,
                HorizontalAlignment.LEFT,
                VerticalAlignment.TOP);
    }

    @Override
    protected List<PartyMemberOverlay> getPreviewChildren() {
        return List.of(new PartyMemberOverlay(DUMMY_USER_1), new PartyMemberOverlay(DUMMY_USER_2));
    }

    @SubscribeEvent
    public void onHadesUserAdded(HadesUserAddedEvent event) {
        updateChildren();
    }

    @SubscribeEvent
    public void onPartyChange(HadesRelationsUpdateEvent.PartyList event) {
        updateChildren();
    }

    @SubscribeEvent
    public void onPartyChange(PartyEvent.Invited event) {
        updateChildren();
    }

    @SubscribeEvent
    public void onPartyChange(PartyEvent.Listed event) {
        updateChildren();
    }

    @SubscribeEvent
    public void onPartyChange(PartyEvent.PriorityChanged event) {
        updateChildren();
    }

    @SubscribeEvent
    public void onPartyChange(PartyEvent.OtherDisconnected event) {
        updateChildren();
    }

    @SubscribeEvent
    public void onPartyChange(PartyEvent.OtherJoined event) {
        updateChildren();
    }

    @SubscribeEvent
    public void onPartyChange(PartyEvent.OtherLeft event) {
        updateChildren();
    }

    @SubscribeEvent
    public void onPartyChange(PartyEvent.OtherReconnected event) {
        updateChildren();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScoreboardSegmentChange(ScoreboardSegmentAdditionEvent event) {
        if (disablePartyMembersOnScoreboard.get()
                && event.getSegment().getScoreboardPart() instanceof PartyScoreboardPart) {
            event.setCanceled(true);
        }
    }

    private void updateChildren() {
        this.clearChildren();

        // War users take priority
        List<HadesUser> warUsers = Models.War.getHadesUsers();

        if (!warUsers.isEmpty()) {
            warUsers.forEach(hadesUser -> this.addChild(new PartyMemberOverlay(hadesUser)));
            return;
        }

        List<HadesUser> hadesUsers = Services.Hades.getHadesUsers().toList();
        List<String> partyMembers = Models.Party.getPartyMembers();

        List<HadesUser> hadesUsingPartyMembers = hadesUsers.stream()
                .filter(hadesUser -> partyMembers.contains(hadesUser.getName()))
                .sorted(Comparator.comparing(element -> partyMembers.indexOf(element.getName())))
                .collect(Collectors.toList());
        hadesUsingPartyMembers =
                hadesUsingPartyMembers.subList(0, Math.min(hadesUsingPartyMembers.size(), maxPartyMembers.get()));

        for (HadesUser hadesUser : hadesUsingPartyMembers) {
            this.addChild(new PartyMemberOverlay(hadesUser));
        }
    }

    public final class PartyMemberOverlay extends Overlay {
        private static final int HEAD_SIZE = 26;

        private final HadesUser hadesUser;

        private PartyMemberOverlay(HadesUser hadesUser) {
            super(
                    new OverlayPosition(
                            0,
                            0,
                            VerticalAlignment.TOP,
                            HorizontalAlignment.LEFT,
                            OverlayPosition.AnchorSection.TOP_LEFT),
                    new OverlaySize(162, 50));
            this.hadesUser = hadesUser;
        }

        @Override
        public void render(
                PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
            poseStack.pushPose();

            ResourceLocation skin = SkinUtils.getSkin(hadesUser.getUuid());

            float renderX = getRenderX();
            float renderY = getRenderY();

            poseStack.translate(renderX, renderY, 0);

            // head
            BufferedRenderUtils.drawTexturedRect(
                    poseStack, bufferSource, skin, 0, 0, 0, HEAD_SIZE, HEAD_SIZE, 8, 8, 8, 8, 64, 64);

            // hat
            BufferedRenderUtils.drawTexturedRect(
                    poseStack, bufferSource, skin, 0, 0, 1, HEAD_SIZE, HEAD_SIZE, 40, 8, 8, 8, 64, 64);

            poseStack.translate(HEAD_SIZE, 0, 0);

            poseStack.translate(3, 0, 0);

            BufferedFontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            bufferSource,
                            StyledText.fromString(hadesUser.getName()),
                            0,
                            0,
                            CommonColors.WHITE,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.TOP,
                            TextShadow.NORMAL);

            poseStack.translate(0, 12, 0);

            double healthProgress = hadesUser.getHealth().getProgress();
            double manaProgress = hadesUser.getMana().getProgress();

            // health
            HealthTexture healthTexture = PartyMembersOverlay.this.healthTexture.get();
            BufferedRenderUtils.drawProgressBar(
                    poseStack,
                    bufferSource,
                    Texture.HEALTH_BAR,
                    0,
                    0,
                    81 * 0.85f,
                    healthTexture.getHeight() * 0.85f,
                    0,
                    healthTexture.getTextureY1(),
                    81,
                    healthTexture.getTextureY2(),
                    (float) healthProgress);

            poseStack.translate(0, healthTexture.getHeight() * 0.85f, 0);

            // mana
            ManaTexture manaTexture = PartyMembersOverlay.this.manaTexture.get();
            BufferedRenderUtils.drawProgressBar(
                    poseStack,
                    bufferSource,
                    Texture.MANA_BAR,
                    0,
                    2,
                    81 * 0.85f,
                    2 + manaTexture.getHeight() * 0.85f,
                    0,
                    manaTexture.getTextureY1(),
                    81,
                    manaTexture.getTextureY2(),
                    (float) manaProgress);

            poseStack.popPose();
        }

        @Override
        protected void onConfigUpdate(Config<?> config) {}
    }
}
