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
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.item.event.ItemRenamedEvent;
import com.wynntils.models.spells.event.SpellEvent;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.wynn.ItemUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.SubscribeEvent;

public class SpellCastMessageOverlay extends Overlay {
    private static final int SPELL_MESSAGE_TICKS = 40;

    private StyledText spellMessage;
    private int spellMessageTimer;

    public SpellCastMessageOverlay() {
        super(
                new OverlayPosition(
                        -100,
                        0,
                        VerticalAlignment.BOTTOM,
                        HorizontalAlignment.CENTER,
                        OverlayPosition.AnchorSection.BOTTOM_MIDDLE),
                new OverlaySize(200, 20),
                HorizontalAlignment.CENTER,
                VerticalAlignment.BOTTOM);
    }

    @SubscribeEvent
    public void onItemRename(ItemRenamedEvent event) {
        if (!ItemUtils.isWeapon(event.getItemStack())) return;

        // Hide vanilla item rename popup
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onSpellCast(SpellEvent.Cast event) {
        int manaCost = event.getManaCost();
        spellMessage = StyledText.fromString(
                "§7" + event.getSpellType().getName() + " spell cast! §3[§b-" + manaCost + " ✺§3]");

        spellMessageTimer = SPELL_MESSAGE_TICKS;
    }

    @SubscribeEvent
    public void onSpellFailed(SpellEvent.Failed event) {
        spellMessage = event.getFailureReason().getMessage();
        spellMessageTimer = SPELL_MESSAGE_TICKS;
    }

    @Override
    public void tick() {
        if (spellMessageTimer <= 0) return;

        spellMessageTimer--;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        if (spellMessageTimer <= 0) return;

        // Render it the same way vanilla renders item changes
        int alpha = (int) Math.min((float) spellMessageTimer * 256.0F / 10.0F, 255.0F);
        if (alpha <= 0) return;

        BufferedFontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        bufferSource,
                        spellMessage,
                        this.getRenderX(),
                        this.getRenderX() + this.getWidth(),
                        this.getRenderY(),
                        this.getRenderY() + this.getHeight(),
                        this.getWidth(),
                        CommonColors.WHITE.withAlpha(alpha),
                        this.getRenderHorizontalAlignment(),
                        this.getRenderVerticalAlignment(),
                        TextShadow.NORMAL);
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {}
}
