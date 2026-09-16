# Creating a New Overlay

This guide walks you through adding a new overlay to Wynntils. Overlays are HUD components that render on screen — timers, stat displays, custom bars, and anything else that appears over the game world.

## The Overlay-Feature Relationship

> **Every overlay must be owned by a Feature.** There are no standalone overlays.

Overlays are registered by placing a `@RegisterOverlay` or `@OverlayGroup` annotated field inside a Feature class. The overlay's lifecycle (enabled/disabled state, config visibility) is tied to its parent Feature.

This means creating an overlay always involves two classes:
1. The **overlay class** — extends `TextOverlay` or `Overlay`, contains rendering logic and configs
2. The **feature class** — owns the overlay via an annotated field, registered in `FeatureManager`

If you're adding an overlay to an existing feature, you only need to create the overlay class and add the field. If you need a new dedicated feature, follow `docs/CREATING_FEATURES.md` first.

---

## Step 1 — Choose Your Base Class

### TextOverlay
Use `TextOverlay` when your overlay displays formatted text or data values. You implement `getTemplate()` returning a function expression string — the framework handles rendering.

```java
public class SpellCooldownOverlay extends TextOverlay {
    private static final String TEMPLATE = ""; // define your expression here

    @Override
    public String getTemplate() {
        return TEMPLATE;
    }

    @Override
    public String getPreviewTemplate() {
        return getTemplate(); // shown in settings UI preview
    }
}
```

**Always implement both `getTemplate()` and `getPreviewTemplate()`.** The settings UI calls `getPreviewTemplate()` to show a preview — if it's missing, the overlay won't display correctly in settings.

### Custom Overlay
Use `Overlay` directly when you need full control over rendering — drawing shapes, images, progress bars, or anything requiring `GuiGraphics` directly.

```java
public class MyCustomOverlay extends Overlay {

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker, Window window) {
        // your rendering code here
    }
}
```

---

## Step 2 — Create the Overlay Class

All overlay classes live in:
```
common/src/main/java/com/wynntils/overlays/<OverlayName>Overlay.java
```

The constructor sets the default position and size:

```java
public SpellCooldownOverlay() {
    super(
            new OverlayPosition(
                    5,          // verticalOffset: positive = down, negative = up
                    -5,         // horizontalOffset: positive = right, negative = left
                    VerticalAlignment.TOP,
                    HorizontalAlignment.RIGHT,
                    OverlayPosition.AnchorSection.TOP_RIGHT),
            new OverlaySize(150, 30));
}
```

### OverlayPosition — The 9-Section Grid

The screen is divided into a 3×3 grid of anchor sections:

```
TOP_LEFT    │ TOP_MIDDLE    │ TOP_RIGHT
────────────┼───────────────┼────────────
MIDDLE_LEFT │ MIDDLE        │ MIDDLE_RIGHT
────────────┼───────────────┼────────────
BOTTOM_LEFT │ BOTTOM_MIDDLE │ BOTTOM_RIGHT
```

Pick the section closest to where your overlay should appear by default. The `verticalAlignment` and `horizontalAlignment` control alignment *within* that section. Offsets are small pixel adjustments.

**Common defaults by overlay type:**

| Content | AnchorSection | Alignment |
|---------|--------------|-----------|
| Combat stats, damage | `MIDDLE_LEFT` | TOP, LEFT |
| Timers, cooldowns | `TOP_RIGHT` | TOP, RIGHT |
| Health/mana bars | `BOTTOM_MIDDLE` | BOTTOM, CENTER |
| Quest/activity | `MIDDLE_RIGHT` | TOP, RIGHT |

---

## Step 3 — Add Config Fields

Config fields work exactly like in features — `@Persisted` + `Config<T>`:

```java
@Persisted
private final Config<Boolean> showMilliseconds = new Config<>(false);

@Persisted
private final Config<CustomColor> textColor = new Config<>(CommonColors.WHITE);
```

Do **not** add a `userEnabled` config — the base `Overlay` class already provides this.

---

## Step 4 — Wire the Overlay to a Feature

Add a `@RegisterOverlay` field to the owning Feature class:

```java
@ConfigCategory(Category.OVERLAYS)
public class SpellCooldownOverlayFeature extends Feature {

    @RegisterOverlay // renderType omitted — defaults to CHAT
    private final Overlay spellCooldownOverlay = new SpellCooldownOverlay();

    public SpellCooldownOverlayFeature() {
        super(ProfileDefault.onlyDefault());
    }
}
```

**Rules:**
- `@RegisterOverlay` goes on the **field**, not the overlay class
- The field type is `Overlay` (the base class), not the concrete type
- Do not call `registerOverlay()` anywhere — discovery is automatic

### Specifying RenderElementType

By default, overlays render above most UI elements (`RenderElementType.CHAT`). If your overlay replaces or augments a specific UI element, specify the type:

```java
@RegisterOverlay(renderType = RenderElementType.SCOREBOARD)
private final Overlay scoreboardOverlay = new ScoreboardOverlay();
```

**RenderElementType options:**

| Value | When to use |
|-------|-------------|
| `CHAT` | General HUD overlays (default — omit `renderType` to use) |
| `SCOREBOARD` | Replaces/augments the scoreboard |
| `ACTION_BAR` | Replaces/augments the action bar |
| `BOSS_BARS` | Replaces/augments boss bars |
| `HOTBAR` | Replaces/augments the hotbar |
| `CROSSHAIR` | Crosshair area |
| `PLAYER_TAB_LIST` | Player tab list |
| `CAMERA_OVERLAYS` | World-space / camera overlay area |
| `TITLE` | Replaces/augments the title display |
| `GUI_POST` | Must render after all other GUI |

---

## Step 5 — Multiple Instances with @OverlayGroup

Use `@OverlayGroup` when users should be able to configure multiple independent instances of the same overlay (e.g., several info boxes):

```java
@OverlayGroup(instances = 7)
private final List<InfoBoxOverlay> infoBoxOverlays = new ArrayList<>();
```

Each instance gets a unique ID (`1`, `2`, `3`, …) used in its display name and locale keys. The `instances` value sets how many exist by default — users can add more in settings.

---

## Step 6 — Register the Feature in FeatureManager

If you created a **new** feature to own the overlay, register it in `FeatureManager.init()` at:
```
common/src/main/java/com/wynntils/core/consumers/features/FeatureManager.java
```

Add inside the correct `// region` block (alphabetically):
```java
// region overlays
registerFeature(new SpellCooldownOverlayFeature());
// endregion
```

See `docs/CREATING_FEATURES.md` for the full feature registration guide.

---

## Step 7 — Add Locale Strings

Open `common/src/main/resources/assets/wynntils/lang/en_us.json` and add entries at the **correct alphabetical position**:

```json
"feature.wynntils.spellCooldownOverlay.description": "Displays remaining spell cooldown time.",
"feature.wynntils.spellCooldownOverlay.name": "Spell Cooldown Overlay",
"feature.wynntils.spellCooldownOverlay.overlay.spellCooldown.name": "Spell Cooldown",
"feature.wynntils.spellCooldownOverlay.overlay.spellCooldown.showMilliseconds.description": "Show milliseconds in the cooldown timer.",
"feature.wynntils.spellCooldownOverlay.overlay.spellCooldown.showMilliseconds.name": "Show Milliseconds"
```

**Key format reference:**

| String | Key pattern |
|--------|-------------|
| Feature name | `feature.wynntils.<featureCamel>.name` |
| Feature description | `feature.wynntils.<featureCamel>.description` |
| Overlay name | `feature.wynntils.<featureCamel>.overlay.<overlayCamel>.name` |
| Config name | `feature.wynntils.<featureCamel>.overlay.<overlayCamel>.<fieldName>.name` |
| Config description | `feature.wynntils.<featureCamel>.overlay.<overlayCamel>.<fieldName>.description` |

**Deriving the key parts:**
- `<featureCamel>` = feature class name minus "Feature", first letter lowercased
  (`SpellCooldownOverlayFeature` → `spellCooldownOverlay`)
- `<overlayCamel>` = overlay class name minus "Overlay", first letter lowercased
  (`SpellCooldownOverlay` → `spellCooldown`)

---

## Complete Example

**Request:** "A text overlay showing spell cooldown remaining time, top-right, new dedicated feature."

**`common/src/main/java/com/wynntils/overlays/SpellCooldownOverlay.java`:**

```java
/*
 * Copyright © Wynntils 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays;

import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.consumers.overlays.TextOverlay;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;

public class SpellCooldownOverlay extends TextOverlay {
    private static final String TEMPLATE = ""; // TODO: define spell cooldown template

    public SpellCooldownOverlay() {
        super(
                new OverlayPosition(
                        5,
                        -5,
                        VerticalAlignment.TOP,
                        HorizontalAlignment.RIGHT,
                        OverlayPosition.AnchorSection.TOP_RIGHT),
                new OverlaySize(150, 30));
    }

    @Override
    public String getTemplate() {
        return TEMPLATE;
    }

    @Override
    public String getPreviewTemplate() {
        return getTemplate();
    }
}
```

**`common/src/main/java/com/wynntils/features/overlays/SpellCooldownOverlayFeature.java`:**

```java
/*
 * Copyright © Wynntils 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.overlays;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.ProfileDefault;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.annotations.RegisterOverlay;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.overlays.SpellCooldownOverlay;

@ConfigCategory(Category.OVERLAYS)
public class SpellCooldownOverlayFeature extends Feature {

    @RegisterOverlay // renderType omitted — defaults to CHAT
    private final Overlay spellCooldownOverlay = new SpellCooldownOverlay();

    public SpellCooldownOverlayFeature() {
        super(ProfileDefault.onlyDefault());
    }
}
```

> **Profile default:** `ProfileDefault.onlyDefault()` enables the feature only in the `DEFAULT` config profile — the standard choice for overlay features. See `docs/CREATING_FEATURES.md` for the full Profile Defaults table.

**`FeatureManager.java`** (inside `// region overlays`):

```java
registerFeature(new SpellCooldownOverlayFeature());
```

**`en_us.json`** (at correct alphabetical position):

```json
"feature.wynntils.spellCooldownOverlay.description": "Displays remaining spell cooldown time.",
"feature.wynntils.spellCooldownOverlay.name": "Spell Cooldown Overlay",
"feature.wynntils.spellCooldownOverlay.overlay.spellCooldown.name": "Spell Cooldown"
```

---

## Common Mistakes

| Mistake | What happens | Fix |
|---------|-------------|-----|
| Forgot `registerFeature()` in FeatureManager | Overlay silently does nothing | Always register the owning feature |
| `@RegisterOverlay` on the overlay class | Annotation is ignored | Put it on the **field** inside the Feature |
| Called `registerOverlay()` manually | Compile error or double-registration | Remove it — discovery is automatic |
| Wrong locale key format | Shows raw key in settings UI | Use `feature.wynntils.<featureCamel>.overlay.<overlayCamel>.name` |
| Missing `getPreviewTemplate()` | Settings preview broken | Always implement both template methods |
| Added `userEnabled` config | Duplicate config in settings | Remove it — `Overlay` already has `userEnabled` |
| Locale keys out of alphabetical order | Build warnings | Insert at correct sorted position |
