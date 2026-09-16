# Creating a New Feature

This guide walks you through adding a new feature to Wynntils. Features are the primary way to add user-facing functionality — they handle game events, expose configuration options, register keybinds, and integrate with the rest of the mod.

## Overview

Every feature is a Java class that:
- Extends `Feature`
- Is annotated with `@ConfigCategory` to place it in the right settings category
- Is manually registered in `FeatureManager.init()`
- Has locale keys in `en_us.json` for all user-visible strings

> **Important:** Features are **not** auto-discovered. If you forget to register your feature in `FeatureManager`, it will silently do nothing.

---

## Step 1 — Create the Feature Class

Create a new file in the appropriate subdirectory of `common/src/main/java/com/wynntils/features/`:

| Category | Directory |
|----------|-----------|
| `COMBAT` | `features/combat/` |
| `UI` | `features/ui/` |
| `INVENTORY` | `features/inventory/` |
| `MAP` | `features/map/` |
| `OVERLAYS` | `features/overlays/` |
| `PLAYERS` | `features/players/` |
| `CHAT` | `features/chat/` |
| `UTILITIES` | `features/utilities/` |
| `EMBELLISHMENTS` | `features/embellishments/` |
| `DEBUG` | `features/debug/` |

**Minimal feature:**

```java
/*
 * Copyright © Wynntils 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.ProfileDefault;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.COMBAT)
public class MyFeature extends Feature {

    public MyFeature() {
        super(ProfileDefault.ENABLED);
    }

    @SubscribeEvent
    public void onSomeEvent(SomeEvent event) {
        if (!Models.WorldState.onWorld()) return;
        // your logic here
    }
}
```

Key points:
- `@ConfigCategory` determines which section the feature appears in under Wynntils settings.
- The `super(ProfileDefault.ENABLED)` constructor argument controls which config profiles enable this feature by default (see [Profile Defaults](#profile-defaults) below).
- `Models.WorldState.onWorld()` guards event handlers so they only run while on a Wynncraft world.

---

## Step 2 — Add Configuration Fields

Use `@Persisted` + `Config<T>` for any user-configurable settings. The config system automatically generates UI controls and persists values across sessions.

```java
@Persisted
private final Config<Boolean> showNotification = new Config<>(true);

@Persisted
private final Config<Integer> maxCount = new Config<>(5);

@Persisted
private final Config<Float> opacity = new Config<>(0.8f);

@Persisted
private final Config<CustomColor> glowColor = new Config<>(CommonColors.RED);
```

**Rules:**
- Every `Config<>` field **must** have `@Persisted`. Without it the value won't be saved or shown in settings.
- Don't add a `featureEnabled` config — the base `Feature` class already provides `userEnabled` for toggling the feature on/off.
- Add only the settings the feature actually needs (YAGNI).

**Common config types:**

| Use case | Type | Example default |
|----------|------|-----------------|
| Toggle | `Config<Boolean>` | `new Config<>(true)` |
| Integer count | `Config<Integer>` | `new Config<>(3)` |
| Float (0–1 range) | `Config<Float>` | `new Config<>(0.5f)` |
| Color | `Config<CustomColor>` | `new Config<>(CommonColors.RED)` |
| Fixed choices | `Config<YourEnum>` | `new Config<>(YourEnum.DEFAULT)` |

Access the value at runtime with `.get()`:

```java
if (showNotification.get()) { ... }
int limit = maxCount.get();
```

---

## Step 3 — Add Keybinds (optional)

Keybinds require two changes: a definition entry and a field in your feature.

### 3a — Add the definition

Open `common/src/main/java/com/wynntils/core/keybinds/KeyBindDefinition.java` and add a constant:

```java
public static final KeyBindDefinition TOGGLE_MY_FEATURE = register(
        "toggleMyFeature",            // internal ID (camelCase)
        "Toggle My Feature",          // display name in keybind settings
        Managers.KeyBind.COMBAT_CATEGORY,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,        // unbound by default — users set their own key
        true);
```

Always use `GLFW_KEY_UNKNOWN` as the default — never hard-code a key. Users assign their own bindings.

`KeyMapping.Category` options (pick the one matching your feature's domain):

| Category constant | Use for |
|-------------------|---------|
| `Managers.KeyBind.CHAT_CATEGORY` | Chat features |
| `Managers.KeyBind.COMBAT_CATEGORY` | Combat features |
| `Managers.KeyBind.COMMANDS_CATEGORY` | Command features |
| `Managers.KeyBind.INVENTORY_CATEGORY` | Inventory/item features |
| `Managers.KeyBind.MAP_CATEGORY` | Map features |
| `Managers.KeyBind.OVERLAYS_CATEGORY` | Overlay features |
| `Managers.KeyBind.PLAYERS_CATEGORY` | Player/party/guild features |
| `Managers.KeyBind.TOOLTIPS_CATEGORY` | Tooltip features |
| `Managers.KeyBind.UI_CATEGORY` | UI/HUD features |
| `Managers.KeyBind.UTILITIES_CATEGORY` | Utility features |
| `Managers.KeyBind.DEBUG_CATEGORY` | Debug features |

### 3b — Add the field to your feature

```java
@RegisterKeyBind
private final KeyBind toggleMyFeatureKeybind =
        KeyBindDefinition.TOGGLE_MY_FEATURE.create(this::handleToggle);
```

Add a handler method in the same class:

```java
private void handleToggle() {
    // runs when the user presses the keybind
}
```

Required imports (only when using keybinds):
```java
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.keybinds.KeyBindDefinition;
```

---

## Step 4 — Register the Feature

Open `common/src/main/java/com/wynntils/core/consumers/features/FeatureManager.java` and add your feature to the `init()` method inside the correct `// region` block, in alphabetical order:

```java
// region combat
// ...existing features...
registerFeature(new MyFeature());   // <-- add here, alphabetically
// ...
// endregion
```

Region names match the `@ConfigCategory` value (e.g., `// region combat` for `Category.COMBAT`).

---

## Step 5 — Add Locale Strings

Open `common/src/main/resources/assets/wynntils/lang/en_us.json` and add entries at the **correct alphabetical position** (the file is sorted):

```json
"feature.wynntils.myFeature.description": "A short description shown in the settings UI.",
"feature.wynntils.myFeature.name": "My Feature",
```

Add one pair per config field:

```json
"feature.wynntils.myFeature.showNotification.description": "Show a notification when the feature triggers.",
"feature.wynntils.myFeature.showNotification.name": "Show Notification",
```

Add one entry per keybind — keybinds use a **different namespace** from feature/config keys:

```json
"wynntils.keybind.toggleMyFeature": "Toggle My Feature",
```

The keybind ID is the first argument passed to `register()` in `KeyBindDefinition.java`.

**Key format reference:**

| String | Key pattern |
|--------|-------------|
| Feature name | `feature.wynntils.<camelName>.name` |
| Feature description | `feature.wynntils.<camelName>.description` |
| Config field name | `feature.wynntils.<camelName>.<fieldName>.name` |
| Config field description | `feature.wynntils.<camelName>.<fieldName>.description` |
| Keybind name | `wynntils.keybind.<keybindId>` |

`<camelName>` is the camelCase class name without "Feature" (e.g., `MyFeature` → `myFeature`).
`<keybindId>` is the `id` string (first arg) passed to `KeyBindDefinition.register()` (e.g., `"toggleMyFeature"`).

---

## Profile Defaults

The `ProfileDefault` constructor argument controls which config profiles enable your feature by default.

| Profile | Description |
|---------|-------------|
| `DEFAULT` | Recommended for experienced players |
| `NEW_PLAYER` | For players new to Wynntils |
| `LITE` | Minimal feature set |
| `MINIMAL` | Only essential features |
| `BLANK_SLATE` | All features disabled |

**Enable for all profiles:**
```java
super(ProfileDefault.ENABLED);
```

**Enable only for specific profiles** (for niche or advanced features):
```java
super(new ProfileDefault.Builder()
        .enabledFor(ConfigProfile.DEFAULT, ConfigProfile.NEW_PLAYER)
        .build());
```

---

## Complete Example

Here's a complete feature with one config field and one keybind:

**`features/combat/EnemyHighlightFeature.java`:**

```java
/*
 * Copyright © Wynntils 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.ProfileDefault;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.keybinds.KeyBindDefinition;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.TickAlwaysEvent;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.COMBAT)
public class EnemyHighlightFeature extends Feature {

    @Persisted
    private final Config<CustomColor> highlightColor = new Config<>(CommonColors.RED);

    @RegisterKeyBind
    private final KeyBind toggleEnemyHighlightKeybind =
            KeyBindDefinition.TOGGLE_ENEMY_HIGHLIGHT.create(this::toggleHighlight);

    public EnemyHighlightFeature() {
        super(ProfileDefault.ENABLED);
    }

    @SubscribeEvent
    public void onTick(TickAlwaysEvent event) {
        if (!Models.WorldState.onWorld()) return;
        // TODO: highlight nearby enemies using highlightColor.get()
    }

    private void toggleHighlight() {
        // TODO: toggle highlight on/off
    }
}
```

**`KeyBindDefinition.java` addition** (inside the class, grouped with other combat keybinds):

```java
public static final KeyBindDefinition TOGGLE_ENEMY_HIGHLIGHT = register(
        "toggleEnemyHighlight",
        "Toggle Enemy Highlight",
        Managers.KeyBind.COMBAT_CATEGORY,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        true);
```

**`FeatureManager.java`** (inside `// region combat`):

```java
registerFeature(new EnemyHighlightFeature());
```

**`en_us.json`** (at the correct alphabetical position):

```json
"feature.wynntils.enemyHighlight.description": "Highlights nearby enemies with a configurable color.",
"feature.wynntils.enemyHighlight.highlightColor.description": "Color used to highlight nearby enemies.",
"feature.wynntils.enemyHighlight.highlightColor.name": "Highlight Color",
"feature.wynntils.enemyHighlight.name": "Enemy Highlight",
"wynntils.keybind.toggleEnemyHighlight": "Toggle Enemy Highlight"
```

---

## Common Mistakes

| Mistake | What happens | Fix |
|---------|-------------|-----|
| Forgot `FeatureManager.init()` registration | Feature silently does nothing | Always add `registerFeature(new YourFeature())` |
| `Config<>` field without `@Persisted` | Value not saved, not shown in settings | Add `@Persisted` to every `Config<>` field |
| Added `featureEnabled` config | Duplicate of existing `userEnabled` | Remove it — `Feature` already has this |
| Hard-coded keybind default key | Conflicts with other users' bindings | Use `GLFW.GLFW_KEY_UNKNOWN` |
| Wrong keybind locale key format | Key not found, shows raw string | Format: `wynntils.keybind.<keybindId>` (NOT under `feature.wynntils.*`) |
| Locale keys out of alphabetical order | Build warnings, hard to maintain | Insert at the correct sorted position |
| Config missing `.description` key | Tooltip shows nothing in settings UI | Every config needs both `.name` and `.description` |
| Feature in wrong `FeatureManager` region | Appears under wrong category | Match region to `@ConfigCategory` value |
