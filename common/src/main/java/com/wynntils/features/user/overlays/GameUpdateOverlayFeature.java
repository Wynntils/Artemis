/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.core.features.overlays.OverlayPosition;
import com.wynntils.core.features.overlays.annotations.OverlayInfo;
import com.wynntils.core.features.overlays.sizes.GuiScaledOverlaySize;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.objects.MessageContainer;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.render.FontRenderer;
import com.wynntils.mc.render.HorizontalAlignment;
import com.wynntils.mc.render.TextRenderSetting;
import com.wynntils.mc.render.TextRenderTask;
import com.wynntils.mc.render.VerticalAlignment;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.utils.objects.CommonColors;
import com.wynntils.wc.utils.WynnUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

@FeatureInfo(category = "Overlays")
public class GameUpdateOverlayFeature extends UserFeature {
    private static GameUpdateOverlayFeature INSTANCE;

    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    public final GameUpdateOverlay GameUpdateOverlay = new GameUpdateOverlay();

    private static final List<MessageContainer> messageQueue = new LinkedList<>();

    public static MessageContainer queueMessage(String message) {
        return queueMessage(new TextRenderTask(message, TextRenderSetting.DEFAULT));
    }

    public static MessageContainer queueMessage(TextRenderTask message) {
        if (!WynnUtils.onWorld()) return null;

        WynntilsMod.info("Message Queued: " + message);
        MessageContainer msgContainer = new MessageContainer(message);
        McUtils.mc().doRunTask(() -> {
            messageQueue.add(msgContainer);

            if (GameUpdateOverlayFeature.INSTANCE.GameUpdateOverlay.overrideNewMessages
                    && messageQueue.size() > GameUpdateOverlayFeature.INSTANCE.GameUpdateOverlay.messageLimit)
                messageQueue.remove(0);
        });
        return msgContainer;
    }

    public static void editMessage(MessageContainer msgContainer, String newMessage) {
        msgContainer.editMessage(newMessage);
        msgContainer.resetRemainingTime();
    }

    public static void resetMessages() {
        McUtils.mc().doRunTask(messageQueue::clear);
    }

    public static class GameUpdateOverlay extends Overlay {

        @Config
        public int messageLimit = 5;

        @Config
        public float messageTimeLimit = 10f;

        @Config
        public boolean invertGrowth = true;

        @Config
        public int messageMaxLength = 0;

        @Config
        public FontRenderer.TextShadow textShadow = FontRenderer.TextShadow.OUTLINE;

        @Config
        public boolean overrideNewMessages = true;

        private TextRenderSetting textRenderSetting;

        public GameUpdateOverlay() {
            super(
                    new OverlayPosition(
                            0,
                            0,
                            VerticalAlignment.Top,
                            HorizontalAlignment.Right,
                            OverlayPosition.AnchorSection.BottomRight),
                    new GuiScaledOverlaySize(250, 110));

            updateTextRenderSetting();
        }

        @Override
        public void render(PoseStack poseStack, float partialTicks, Window window) {
            List<MessageContainer> toRender = new ArrayList<>();

            ListIterator<MessageContainer> messages = messageQueue.listIterator(messageQueue.size());
            while (messages.hasPrevious()) {
                MessageContainer message = messages.previous();

                if (message.getRemainingTime() <= 0.0f) {
                    messages.remove(); // remove the message if the time has come
                    continue;
                }

                TextRenderTask messageTask = message.getRenderTask();

                if (messageMaxLength == 0 || messageTask.getText().length() < messageMaxLength) {
                    toRender.add(message);
                } else {
                    MessageContainer first = new MessageContainer(
                            messageTask.getText().substring(0, messageMaxLength), message.getEndTime());
                    MessageContainer second = new MessageContainer(
                            messageTask.getText().substring(messageMaxLength), message.getEndTime());
                    if (this.invertGrowth) {
                        toRender.add(first);
                        toRender.add(second);
                    } else {
                        toRender.add(second);
                        toRender.add(first);
                    }
                }
            }

            if (toRender.isEmpty()) return;

            List<MessageContainer> renderedValues = this.overrideNewMessages
                    ? toRender.subList(0, Math.min(toRender.size(), this.messageLimit))
                    : toRender.subList(Math.max(toRender.size() - this.messageLimit, 0), toRender.size());

            Collections.reverse(renderedValues);

            if (this.invertGrowth) {
                while (renderedValues.size() < messageLimit) {
                    renderedValues.add(0, new MessageContainer(""));
                }
            }

            FontRenderer.getInstance()
                    .renderTextsWithAlignment(
                            poseStack,
                            this.getRenderX(),
                            this.getRenderY(),
                            renderedValues.stream()
                                    .map(messageContainer -> messageContainer
                                            .getRenderTask()
                                            .setSetting(textRenderSetting.withCustomColor(messageContainer
                                                    .getRenderTask()
                                                    .getSetting()
                                                    .customColor()
                                                    .withAlpha(messageContainer.getRemainingTime() / 1000f))))
                                    .toList(),
                            this.getRenderedWidth(),
                            this.getRenderedHeight(),
                            this.getRenderHorizontalAlignment(),
                            this.getRenderVerticalAlignment());
        }

        @Override
        protected void onConfigUpdate(ConfigHolder configHolder) {
            updateTextRenderSetting();
        }

        private void updateTextRenderSetting() {
            textRenderSetting = TextRenderSetting.getWithHorizontalAlignment(
                            this.getWidth(), CommonColors.WHITE, this.getRenderHorizontalAlignment())
                    .withTextShadow(textShadow);
        }
    }

    public static GameUpdateOverlayFeature getInstance() {
        return INSTANCE;
    }
}
