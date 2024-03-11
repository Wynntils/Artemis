/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays.custombars;

import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.overlays.BarOverlay;
import com.wynntils.core.consumers.overlays.CustomNameProperty;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.utils.type.ErrorOr;

public abstract class CustomBarOverlayBase extends BarOverlay implements CustomNameProperty {
    @Persisted(i18nKey = "feature.wynntils.customBarsOverlay.overlay.customBarBase.customName")
    public final Config<String> customName = new Config<>("");

    @Persisted(i18nKey = "feature.wynntils.customBarsOverlay.overlay.customBarBase.textTemplate")
    public final Config<String> textTemplate = new Config<>("");

    @Persisted(i18nKey = "feature.wynntils.customBarsOverlay.overlay.customBarBase.valueTemplate")
    public final Config<String> valueTemplate = new Config<>("");

    @Persisted(i18nKey = "feature.wynntils.customBarsOverlay.overlay.customBarBase.enabledTemplate")
    public final Config<String> enabledTemplate = new Config<>("string_equals(world_state;\"WORLD\")");

    protected CustomBarOverlayBase(int id, OverlaySize overlaySize) {
        super(id, overlaySize);
    }

    @Override
    public BarOverlayTemplatePair getTemplate() {
        return new BarOverlayTemplatePair(textTemplate.get(), valueTemplate.get());
    }

    @Override
    public BarOverlayTemplatePair getPreviewTemplate() {
        if (!valueTemplate.get().isEmpty()) {
            return getTemplate();
        }

        return getActualPreviewTemplate();
    }

    @Override
    public boolean isRendered() {
        if (valueTemplate.get().isEmpty()) return false;

        ErrorOr<Boolean> enabledOrError = Managers.Function.tryGetRawValueOfType(enabledTemplate.get(), Boolean.class);
        return !enabledOrError.hasError() && enabledOrError.getValue();
    }

    @Override
    public Config<String> getCustomName() {
        return customName;
    }

    protected abstract BarOverlayTemplatePair getActualPreviewTemplate();
}
