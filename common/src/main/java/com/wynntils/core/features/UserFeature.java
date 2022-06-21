/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.features;

import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigHolder;

/**
 * A feature that is enabled & disabled by the user.
 */
public abstract class UserFeature extends Feature {
    @Config(displayName = "Enabled", description = "Should this feature be enabled?")
    protected boolean userEnabled = true;

    @Override
    public final void updateConfigOption(ConfigHolder configHolder) {
        // if user toggle was changed, enable/disable feature accordingly
        if (configHolder.getFieldName().equals("userEnabled")) {
            tryUserToggle();
            return;
        }

        // otherwise, trigger regular config update
        onConfigUpdate(configHolder);
    }

    /** Updates the feature's enabled/disabled state to match the user's setting, if necessary */
    public final void tryUserToggle() {
        if (userEnabled) {
            tryEnable();
        } else {
            tryDisable();
        }
    }

    public void setUserEnabled(boolean newState) {
        this.userEnabled = newState;
    }
}
