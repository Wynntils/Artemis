/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.overlays.NpcDialogueFeature;
import com.wynntils.handlers.chat.type.NpcDialogueType;
import com.wynntils.models.npcdialogue.event.NpcDialogueProcessingEvent;
import com.wynntils.models.npcdialogue.event.NpcDialogueRemoved;
import com.wynntils.models.npcdialogue.type.NpcDialogue;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.Pair;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.SubscribeEvent;

public class NpcDialogueOverlay extends Overlay {
    private static final StyledText PRESS_SNEAK_TO_CONTINUE = StyledText.fromString("§cPress SNEAK to continue");

    @Persisted
    public final Config<TextShadow> textShadow = new Config<>(TextShadow.NORMAL);

    @Persisted
    public final Config<Float> backgroundOpacity = new Config<>(0.2f);

    @Persisted
    public final Config<Boolean> stripColors = new Config<>(false);

    @Persisted
    public final Config<Boolean> showHelperTexts = new Config<>(true);

    private TextRenderSetting renderSetting;

    private Component selectionComponents = null;

    public NpcDialogueOverlay() {
        super(
                new OverlayPosition(
                        0,
                        0,
                        VerticalAlignment.TOP,
                        HorizontalAlignment.CENTER,
                        OverlayPosition.AnchorSection.BOTTOM_MIDDLE),
                new OverlaySize(400, 50),
                HorizontalAlignment.CENTER,
                VerticalAlignment.MIDDLE);
        updateTextRenderSettings();
    }

    @SubscribeEvent
    public void onNpcDialoguePost(NpcDialogueProcessingEvent.Post event) {
        NpcDialogue dialogue = event.getDialogue();

        // This is specific to the overlay, so we don't want to handle it in the feature
        // (when we display the dialogues in the chat, we don't need to duplicate the message)
        if (dialogue.dialogueType() == NpcDialogueType.SELECTION) {
            // This is a bit of a workaround to be able to select the options
            MutableComponent clickMsg =
                    Component.literal("Select an option to continue:").withStyle(ChatFormatting.AQUA);
            event.getPostProcessedDialogue()
                    .forEach(line -> clickMsg.append(Component.literal("\n").append(line.getComponent())));
            McUtils.sendMessageToClient(clickMsg);

            // Save the selection components so we can remove it later
            selectionComponents = clickMsg;
        }
    }

    @SubscribeEvent
    public void onNpcDialogueRemoved(NpcDialogueRemoved event) {
        if (event.getRemovedDialogue().dialogueType() == NpcDialogueType.SELECTION) {
            // Remove the selection components if it exists
            if (selectionComponents != null) {
                McUtils.removeMessageFromChat(selectionComponents);
                selectionComponents = null;
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        NpcDialogue currentDialogue = Models.NpcDialogue.getCurrentDialogue();
        List<NpcDialogue> confirmationlessDialogues = Models.NpcDialogue.getConfirmationlessDialogues();

        if (currentDialogue.isEmpty() && confirmationlessDialogues.isEmpty()) return;

        List<Pair<Long, List<StyledText>>> unsortedDialogues = new LinkedList<>();

        if (!currentDialogue.isEmpty()) {
            unsortedDialogues.add(Pair.of(currentDialogue.addTime(), currentDialogue.currentDialogue()));
        }

        confirmationlessDialogues.forEach(d -> unsortedDialogues.add(Pair.of(d.addTime(), d.currentDialogue())));

        // Sort the dialogues by their add time
        unsortedDialogues.sort(Comparator.comparingLong(Pair::a));

        LinkedList<StyledText> allDialogues = new LinkedList<>();
        unsortedDialogues.forEach(pair -> {
            allDialogues.addAll(pair.b());
            allDialogues.add(StyledText.EMPTY);
        });

        // Remove the last empty line
        allDialogues.removeLast();

        renderDialogue(
                poseStack, bufferSource, allDialogues, currentDialogue.dialogueType(), currentDialogue.isProtected());
    }

    @Override
    public void renderPreview(
            PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        List<StyledText> fakeDialogue = List.of(
                StyledText.fromString(
                        "§7[1/2] §2Random Citizen: §aDid you know that Wynntils is the best Wynncraft mod you'll probably find?"),
                StyledText.EMPTY,
                StyledText.fromString(
                        "§7[2/2] §2Random Citizen: §aIt's got so many features, it's hard to keep track of them all!"));
        // we have to force update every time
        updateTextRenderSettings();

        renderDialogue(poseStack, bufferSource, fakeDialogue, NpcDialogueType.NORMAL, true);
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {
        updateTextRenderSettings();
    }

    private void updateTextRenderSettings() {
        renderSetting = TextRenderSetting.DEFAULT
                .withMaxWidth(this.getWidth() - 5)
                .withHorizontalAlignment(this.getRenderHorizontalAlignment())
                .withTextShadow(textShadow.get());
    }

    private void renderDialogue(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            List<StyledText> currentDialogue,
            NpcDialogueType dialogueType,
            boolean isProtected) {
        List<TextRenderTask> dialogueRenderTasks = currentDialogue.stream()
                .map(s -> new TextRenderTask(s, renderSetting))
                .toList();

        if (stripColors.get()) {
            dialogueRenderTasks.forEach(dialogueRenderTask ->
                    dialogueRenderTask.setText(dialogueRenderTask.getText().getStringWithoutFormatting()));
        }

        float textHeight = (float) dialogueRenderTasks.stream()
                .map(dialogueRenderTask -> FontRenderer.getInstance()
                        .calculateRenderHeight(
                                dialogueRenderTask.getText(),
                                dialogueRenderTask.getSetting().maxWidth()))
                .mapToDouble(f -> f)
                .sum();

        // Draw a translucent background
        float rectHeight = textHeight + 10;
        float rectRenderY =
                switch (this.getRenderVerticalAlignment()) {
                    case TOP -> this.getRenderY();
                    case MIDDLE -> this.getRenderY() + (this.getHeight() - rectHeight) / 2f;
                    case BOTTOM -> this.getRenderY() + this.getHeight() - rectHeight;
                };
        int colorAlphaRect = Math.round(MathUtils.clamp(255 * backgroundOpacity.get(), 0, 255));
        BufferedRenderUtils.drawRect(
                poseStack,
                bufferSource,
                CommonColors.BLACK.withAlpha(colorAlphaRect),
                this.getRenderX(),
                rectRenderY,
                0,
                this.getWidth(),
                rectHeight);

        // Render the message
        BufferedFontRenderer.getInstance()
                .renderTextsWithAlignment(
                        poseStack,
                        bufferSource,
                        this.getRenderX(),
                        this.getRenderY(),
                        dialogueRenderTasks,
                        this.getWidth(),
                        this.getHeight(),
                        this.getRenderHorizontalAlignment(),
                        this.getRenderVerticalAlignment());

        if (showHelperTexts.get()) {
            // Render "To continue" message
            List<TextRenderTask> helperRenderTasks = new LinkedList<>();
            StyledText protection = isProtected ? StyledText.fromString("§f<protected> §r") : StyledText.EMPTY;
            if (dialogueType == NpcDialogueType.NORMAL) {
                TextRenderTask pressSneakMessage =
                        new TextRenderTask(PRESS_SNEAK_TO_CONTINUE.prepend(protection), renderSetting);
                helperRenderTasks.add(pressSneakMessage);
            } else if (dialogueType == NpcDialogueType.SELECTION) {
                String msg;
                if (isProtected) {
                    msg = "Select an option to continue (Press the number key to select it)";
                } else {
                    msg = "Open chat and click on the option to select it";
                }

                TextRenderTask pressSneakMessage = new TextRenderTask(protection.append("§c" + msg), renderSetting);
                helperRenderTasks.add(pressSneakMessage);
            }

            NpcDialogueFeature feature = Managers.Feature.getFeatureInstance(NpcDialogueFeature.class);
            if (feature.getScheduledAutoProgressKeyPress() != null
                    && !feature.getScheduledAutoProgressKeyPress().isCancelled()) {
                long timeUntilProgress =
                        feature.getScheduledAutoProgressKeyPress().getDelay(TimeUnit.MILLISECONDS);
                TextRenderTask autoProgressMessage = new TextRenderTask(
                        ChatFormatting.GREEN + "Auto-progress: "
                                + Math.max(0, Math.round(timeUntilProgress / 1000f))
                                + " seconds (Press "
                                + StyledText.fromComponent(feature.cancelAutoProgressKeybind
                                                .getKeyMapping()
                                                .getTranslatedKeyMessage())
                                        .getStringWithoutFormatting()
                                + " to cancel)",
                        renderSetting);
                helperRenderTasks.add(autoProgressMessage);
            }

            BufferedFontRenderer.getInstance()
                    .renderTextsWithAlignment(
                            poseStack,
                            bufferSource,
                            this.getRenderX(),
                            this.getRenderY() + 20 + textHeight,
                            helperRenderTasks,
                            this.getWidth(),
                            this.getHeight() - 20,
                            this.getRenderHorizontalAlignment(),
                            this.getRenderVerticalAlignment());
        }
    }
}
