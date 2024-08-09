/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.overlays.BarOverlay;
import com.wynntils.core.consumers.overlays.ContainerOverlay;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.models.token.event.TokenGatekeeperEvent;
import com.wynntils.utils.colors.ColorChatFormatting;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;

public class TokenBarsOverlay extends ContainerOverlay<TokenBarsOverlay.TokenBarOverlay> {
    public TokenBarsOverlay() {
        super(
                new OverlayPosition(
                        70,
                        -5,
                        VerticalAlignment.TOP,
                        HorizontalAlignment.RIGHT,
                        OverlayPosition.AnchorSection.TOP_RIGHT),
                new OverlaySize(81, 84),
                ContainerOverlay.GrowDirection.DOWN,
                HorizontalAlignment.RIGHT,
                VerticalAlignment.TOP);
    }

    @SubscribeEvent
    public void onGatekeeperAdded(TokenGatekeeperEvent.Added event) {
        this.addChild(new TokenBarOverlay(this.size() + 1));
    }

    @SubscribeEvent
    public void onGatekeeperRemoved(TokenGatekeeperEvent.Removed event) {
        this.clearChildren();
        for (int i = 1; i <= Models.Token.getGatekeepers().size(); i++) {
            this.addChild(new TokenBarOverlay(i));
        }
    }

    @Override
    protected List<TokenBarOverlay> getPreviewChildren() {
        return List.of(new TokenBarOverlay(0), new TokenBarOverlay(1));
    }

    protected static final class TokenBarOverlay extends BarOverlay {
        @Persisted
        public final Config<ColorChatFormatting> color = new Config<>(ColorChatFormatting.GOLD);

        private TokenBarOverlay(int id) {
            super(id, new OverlaySize(81, 21));
            horizontalAlignmentOverride.store(HorizontalAlignment.RIGHT);
            verticalAlignmentOverride.store(VerticalAlignment.TOP);
        }

        @Override
        public BarOverlayTemplatePair getTemplate() {
            return new BarOverlayTemplatePair(
                    color.get().getChatFormatting() + "{token_type(" + getId() + ")}: {token(" + getId() + ")}",
                    "token(" + getId() + ")");
        }

        @Override
        public BarOverlayTemplatePair getPreviewTemplate() {
            return new BarOverlayTemplatePair(color.get().getChatFormatting() + "Tokens: 3/10", "capped(3; 10)");
        }

        @Override
        public boolean isRendered() {
            return true;
        }

        @Override
        public CustomColor getRenderColor() {
            return CustomColor.fromChatFormatting(color.get().getChatFormatting());
        }

        @Override
        public Texture getTexture() {
            return Texture.UNIVERSAL_BAR;
        }

        @Override
        protected float getTextureHeight() {
            return Texture.UNIVERSAL_BAR.height() / 2f;
        }
    }
}
