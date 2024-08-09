/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.statuseffects.event.StatusEffectsChangedEvent;
import com.wynntils.models.statuseffects.type.StatusEffect;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.SubscribeEvent;

public class StatusEffectsOverlay extends Overlay {
    @Persisted
    public final Config<TextShadow> textShadow = new Config<>(TextShadow.OUTLINE);

    @Persisted
    public final Config<Float> fontScale = new Config<>(1.0f);

    @Persisted
    public final Config<StackingBehaviour> stackingBehaviour = new Config<>(StackingBehaviour.GROUP);

    @Persisted
    public final Config<Boolean> sortEffects = new Config<>(true);

    private List<TextRenderTask> renderCache = List.of();
    private TextRenderSetting textRenderSetting;

    public StatusEffectsOverlay() {
        super(
                new OverlayPosition(
                        55,
                        -5,
                        VerticalAlignment.TOP,
                        HorizontalAlignment.RIGHT,
                        OverlayPosition.AnchorSection.TOP_RIGHT),
                new OverlaySize(250, 110));

        updateTextRenderSetting();
    }

    @SubscribeEvent
    public void onStatusChange(StatusEffectsChangedEvent event) {
        recalculateRenderCache();
    }

    private void recalculateRenderCache() {
        List<StatusEffect> effects = Models.StatusEffect.getStatusEffects();
        Stream<RenderedStatusEffect> effectWithProperties;

        if (stackingBehaviour.get() != StackingBehaviour.NONE) {
            effectWithProperties = stackEffects(effects);
        } else {
            effectWithProperties = effects.stream().map(RenderedStatusEffect::new);
        }

        if (sortEffects.get()) {
            // Sort effects based on their prefix and their name
            effectWithProperties = effectWithProperties.sorted(Comparator.comparing(e -> e.effect));
        }

        renderCache = effectWithProperties
                .map(statusTimer -> new TextRenderTask(statusTimer.getRenderedText(), getTextRenderSetting()))
                .toList();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        BufferedFontRenderer.getInstance()
                .renderTextsWithAlignment(
                        poseStack,
                        bufferSource,
                        this.getRenderX(),
                        this.getRenderY(),
                        renderCache,
                        this.getWidth(),
                        this.getHeight(),
                        this.getRenderHorizontalAlignment(),
                        this.getRenderVerticalAlignment(),
                        fontScale.get());
    }

    @Override
    public void renderPreview(
            PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        BufferedFontRenderer.getInstance()
                .renderTextsWithAlignment(
                        poseStack,
                        bufferSource,
                        this.getRenderX(),
                        this.getRenderY(),
                        List.of(
                                new TextRenderTask(
                                        StyledText.fromString("§8⬤ §7 Purification 00:02"), textRenderSetting),
                                new TextRenderTask(StyledText.fromString("§8⬤ §7 Exploding 01:12"), textRenderSetting),
                                new TextRenderTask(StyledText.fromString("§8⬤ §7 Thorns 00:12"), textRenderSetting),
                                new TextRenderTask(
                                        StyledText.fromString("§8⬤ §7 Soul Point Regen 00:12"), textRenderSetting)),
                        this.getWidth(),
                        this.getHeight(),
                        this.getRenderHorizontalAlignment(),
                        this.getRenderVerticalAlignment(),
                        fontScale.get());
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {
        updateTextRenderSetting();
        recalculateRenderCache();
    }

    private void updateTextRenderSetting() {
        textRenderSetting = TextRenderSetting.DEFAULT
                .withMaxWidth(this.getWidth())
                .withHorizontalAlignment(this.getRenderHorizontalAlignment())
                .withTextShadow(textShadow.get());
    }

    protected TextRenderSetting getTextRenderSetting() {
        return textRenderSetting;
    }

    private Stream<RenderedStatusEffect> stackEffects(List<StatusEffect> effects) {
        Map<String, RenderedStatusEffect> effectsToRender = new LinkedHashMap<>();

        for (StatusEffect effect : effects) {
            String key = getEffectsKey(effect);
            RenderedStatusEffect entry = effectsToRender.get(key);

            if (entry == null) {
                entry = new RenderedStatusEffect(effect);
                effectsToRender.put(key, entry);
            }

            entry.setCount(entry.getCount() + 1);

            if (effect.hasModifierValue()) {
                entry.addModifier(effect.getModifierValue());
            }
        }

        return effectsToRender.values().stream();
    }

    private String getEffectsKey(StatusEffect effect) {
        return switch (stackingBehaviour.get()) {
            case NONE, GROUP -> effect.asString().getString();
            case SUM -> effect.getPrefix().getString()
                    + effect.getName().getString()
                    + effect.getModifierSuffix().getString()
                    + effect.getDisplayedTime().getString();
        };
    }

    private final class RenderedStatusEffect {
        private final StatusEffect effect;

        private int count = 0;
        private final List<Double> modifierList = new ArrayList<>();

        private RenderedStatusEffect(StatusEffect effect) {
            this.effect = effect;
        }

        private StyledText getRenderedText() {
            if (this.count <= 1) {
                // Terminate early if there's nothing to do
                return this.effect.asString();
            }

            StyledText modifierText = StyledText.EMPTY;

            switch (stackingBehaviour.get()) {
                case SUM -> {
                    if (!this.modifierList.isEmpty()) {
                        // SUM modifiers
                        double modifierValue = 0.0;
                        for (double modifier : modifierList) {
                            modifierValue += modifier;
                        }
                        // Eliminate .0 when the modifier needs trailing decimals. This is the case for powder specials
                        // on armor.
                        String numberString = (Math.round(modifierValue) == modifierValue)
                                ? String.format("%+d", (long) modifierValue)
                                : String.format("%+.1f", modifierValue);
                        modifierText = StyledText.fromString(ChatFormatting.GRAY + numberString);
                    }
                }
                case GROUP -> {
                    String modifierString = this.effect.getModifier().getString();

                    // look for either a - or a +
                    int minusIndex = modifierString.indexOf('-');
                    int plusIndex = modifierString.indexOf('+');
                    int index = Math.max(minusIndex, plusIndex);

                    if (index == -1) {
                        // We can simply put the count string at the start
                        modifierText = StyledText.fromString(ChatFormatting.GRAY + (this.count + "x"))
                                .append(this.effect.getModifier());
                    } else {
                        // The count string is inserted between the +/- and the number
                        index += 1;
                        modifierText = StyledText.fromString(ChatFormatting.GRAY
                                + modifierString.substring(0, index)
                                + (this.count + "x")
                                + modifierString.substring(index));
                    }
                }
                    // This shouldn't be reached
                default -> {}
            }

            return this.effect
                    .getPrefix()
                    .append(StyledText.fromString(" "))
                    .append(modifierText)
                    .append(this.effect.getModifierSuffix())
                    .append(StyledText.fromString(" "))
                    .append(this.effect.getName())
                    .append(StyledText.fromString(" "))
                    .append(this.effect.getDisplayedTime());
        }

        public int getCount() {
            return this.count;
        }

        public void setCount(int c) {
            this.count = c;
        }

        public StatusEffect getEffect() {
            return this.effect;
        }

        public void addModifier(double modifier) {
            this.modifierList.add(modifier);
        }
    }

    private enum StackingBehaviour {
        NONE,
        GROUP,
        SUM
    }
}
