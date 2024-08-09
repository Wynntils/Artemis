/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.notifications.MessageContainer;
import com.wynntils.core.notifications.TimedMessageContainer;
import com.wynntils.core.notifications.event.NotificationEvent;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.SubscribeEvent;

public class GameNotificationOverlay extends Overlay {
    @Persisted
    public final Config<Float> messageTimeLimit = new Config<>(12f);

    @Persisted
    public final Config<Integer> messageLimit = new Config<>(8);

    @Persisted
    public final Config<Boolean> invertGrowth = new Config<>(true);

    @Persisted
    public final Config<Integer> messageMaxLength = new Config<>(0);

    @Persisted
    public final Config<TextShadow> textShadow = new Config<>(TextShadow.OUTLINE);

    @Persisted
    public final Config<Boolean> overrideNewMessages = new Config<>(true);

    private TextRenderSetting textRenderSetting;
    private static final List<TimedMessageContainer> messageQueue = new LinkedList<>();

    public GameNotificationOverlay() {
        super(
                new OverlayPosition(
                        -20,
                        -5,
                        VerticalAlignment.TOP,
                        HorizontalAlignment.RIGHT,
                        OverlayPosition.AnchorSection.BOTTOM_RIGHT),
                new OverlaySize(250, 110));

        updateTextRenderSetting();
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        messageQueue.clear();
    }

    @SubscribeEvent
    public void onGameNotification(NotificationEvent.Queue event) {
        messageQueue.add(new TimedMessageContainer(event.getMessageContainer(), getMessageDisplayLength()));

        if (overrideNewMessages.get() && messageQueue.size() > messageLimit.get()) {
            messageQueue.removeFirst();
        }
    }

    @SubscribeEvent
    public void onGameNotification(NotificationEvent.Edit event) {
        // On edit, we want to reset the display time of the message, for the overlay
        MessageContainer newContainer = event.getMessageContainer();

        messageQueue.stream()
                .filter(timedMessageContainer ->
                        timedMessageContainer.getMessageContainer().equals(newContainer))
                .findFirst()
                .ifPresent(
                        timedMessageContainer -> timedMessageContainer.resetRemainingTime(getMessageDisplayLength()));
    }

    @SubscribeEvent
    public void onGameNotification(NotificationEvent.Remove event) {
        messageQueue.removeIf(timedMessageContainer ->
                timedMessageContainer.getMessageContainer().equals(event.getMessageContainer()));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        List<TimedMessageContainer> toRender = new ArrayList<>();

        ListIterator<TimedMessageContainer> messages = messageQueue.listIterator(messageQueue.size());
        while (messages.hasPrevious()) {
            TimedMessageContainer message = messages.previous();

            if (message.getRemainingTime() <= 0.0f) {
                messages.remove(); // remove the message if the time has come
                continue;
            }

            TextRenderTask messageTask = message.getRenderTask();

            if (messageMaxLength.get() == 0 || messageTask.getText().getString().length() < messageMaxLength.get()) {
                toRender.add(message);
            } else {
                TimedMessageContainer first = new TimedMessageContainer(
                        new MessageContainer(messageTask.getText().getString().substring(0, messageMaxLength.get())),
                        message.getEndTime());
                TimedMessageContainer second = new TimedMessageContainer(
                        new MessageContainer(messageTask.getText().getString().substring(messageMaxLength.get())),
                        message.getEndTime());
                if (this.invertGrowth.get()) {
                    toRender.add(first);
                    toRender.add(second);
                } else {
                    toRender.add(second);
                    toRender.add(first);
                }
            }
        }

        if (toRender.isEmpty()) return;

        List<TimedMessageContainer> renderedValues = this.overrideNewMessages.get()
                ? toRender.subList(0, Math.min(toRender.size(), this.messageLimit.get()))
                : toRender.subList(Math.max(toRender.size() - this.messageLimit.get(), 0), toRender.size());

        Collections.reverse(renderedValues);

        if (this.invertGrowth.get()) {
            while (renderedValues.size() < messageLimit.get()) {
                renderedValues.addFirst(new TimedMessageContainer(
                        new MessageContainer(""), (long) (this.messageTimeLimit.get() * 1000)));
            }
        }

        BufferedFontRenderer.getInstance()
                .renderTextsWithAlignment(
                        poseStack,
                        bufferSource,
                        this.getRenderX(),
                        this.getRenderY(),
                        renderedValues.stream()
                                .map(messageContainer -> messageContainer
                                        .getRenderTask()
                                        .setSetting(textRenderSetting.withCustomColor(messageContainer
                                                .getRenderTask()
                                                .getSetting()
                                                .customColor()
                                                // A minimum alpha is required, otherwise too small values render with
                                                // max alpha
                                                .withAlpha(messageContainer.getRemainingTime() / 1000f + 0.01f))))
                                .toList(),
                        this.getWidth(),
                        this.getHeight(),
                        this.getRenderHorizontalAlignment(),
                        this.getRenderVerticalAlignment());
    }

    @Override
    public void renderPreview(
            PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        BufferedFontRenderer.getInstance()
                .renderTextWithAlignment(
                        poseStack,
                        bufferSource,
                        this.getRenderX(),
                        this.getRenderY(),
                        new TextRenderTask(
                                StyledText.fromString("§r§a→ §r§2Player [§r§aWC1/Archer§r§2]"), textRenderSetting),
                        this.getWidth(),
                        this.getHeight(),
                        this.getRenderHorizontalAlignment(),
                        this.getRenderVerticalAlignment());
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {
        updateTextRenderSetting();
    }

    private long getMessageDisplayLength() {
        return (long) (messageTimeLimit.get() * 1000);
    }

    private void updateTextRenderSetting() {
        textRenderSetting = TextRenderSetting.DEFAULT
                .withMaxWidth(this.getWidth())
                .withHorizontalAlignment(this.getRenderHorizontalAlignment())
                .withVerticalAlignment(this.getRenderVerticalAlignment())
                .withTextShadow(textShadow.get());
    }
}
