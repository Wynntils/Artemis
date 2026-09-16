---
name: wynntils-overlay
description: Use when creating a new overlay for the Wynntils Minecraft mod — interviews for name, type (TextOverlay or custom), single vs group, feature attachment, configs, and locale name, then scaffolds the overlay Java file, feature field annotation, and en_us.json entries.
---

# Wynntils Overlay Scaffolding

## Overview

Rigid skill. Run the 6-step interview in order, then produce all required file changes. Do not skip steps or files.

**Every overlay must be owned by a Feature.** Overlays are registered via `@RegisterOverlay` or `@OverlayGroup` field annotations on the Feature class — there is no `registerOverlay()` call.

---

## Interview Phase

Run steps 1–6 in order. One question per message.

### Step 1 — Overlay name + description

Ask: "What should this overlay be called, and what does it display?"

Infer:
- **Class name:** `<PascalCase>Overlay` (e.g., "spell cooldown" → `SpellCooldownOverlay`)
- **camelCase name** for locale keys (e.g., `spellCooldown`)
- **Default position:** match description to nearest anchor:
  - Combat stats, damage, spell info → `MIDDLE_LEFT`, offsets (0, 5)
  - Timers, cooldowns, status → `TOP_RIGHT`, offsets (5, -5)
  - Health/mana bars → `BOTTOM_MIDDLE`, offsets (-30, 0)
  - Quest/activity info → `MIDDLE_RIGHT`, offsets (0, -5)
  - Unsure → `TOP_RIGHT`, offsets (5, -5)

### Step 2 — Single vs group

Ask: "Will you need multiple independent instances of this overlay (for example, several info boxes users can each configure separately)?"

- **No** → single `@RegisterOverlay` field
- **Yes** → `@OverlayGroup`; ask: "How many instances by default?" (default: 3)

### Step 3 — Overlay type

Ask: "Does this overlay display formatted text or data, or does it need fully custom rendering (drawing shapes, progress bars, images)?"

- **Text/data** → extends `TextOverlay`; implements `getTemplate()` + `getPreviewTemplate()`
- **Custom rendering** → extends `Overlay`; implements `render(GuiGraphics, DeltaTracker, Window)`

### Step 4 — Feature attachment

Ask: "Do you want to attach this overlay to an existing feature, or create a new dedicated feature for it?"

- **New feature:** Invoke the `wynntils-feature` skill now to scaffold the feature first. Use `@ConfigCategory(Category.OVERLAYS)` for the category. Once the feature is scaffolded, continue to Step 5.
- **Existing feature:** Ask for the feature class name and file path.

### Step 5 — Config fields

Ask: "Does this overlay need any user-configurable settings?"

If **yes**, loop — same pattern as `wynntils-feature` skill:
1. Ask what the setting controls
2. Infer field name (camelCase), type, and default:
   - Toggle → `Config<Boolean>` default `true`/`false`
   - Count/delay → `Config<Integer>` sensible default
   - Float multiplier → `Config<Float>` sensible default
   - Color → `Config<CustomColor>` e.g. `CommonColors.WHITE`
   - Fixed choices → nested enum + `Config<EnumName>`
3. Ask for human-readable name + description
4. Repeat until done

If **no**, proceed to Step 6.

Do not add a `userEnabled` config — the base `Overlay` class already provides this.

### Step 6 — Locale display name

Ask: "What should this overlay be called in the Wynntils settings UI?"

---

## Scaffolding Phase

Produce file changes in this order. Do not skip any.

### File 1 — New overlay Java file

**Path:** `common/src/main/java/com/wynntils/overlays/<OverlayName>Overlay.java`

**TextOverlay template:**

```java
/*
 * Copyright © Wynntils 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays;

import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.TextOverlay;
// only if configs exist:
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;

public class <Name>Overlay extends TextOverlay {
    private static final String TEMPLATE = ""; // TODO: define template expression

    @Persisted
    private final Config<<Type>> <fieldName> = new Config<>(<default>);

    public <Name>Overlay() {
        super(
                new OverlayPosition(
                        <verticalOffset>,
                        <horizontalOffset>,
                        VerticalAlignment.<TOP|MIDDLE|BOTTOM>,
                        HorizontalAlignment.<LEFT|CENTER|RIGHT>,
                        OverlayPosition.AnchorSection.<SECTION>),
                new OverlaySize(<width>, <height>));
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

**Custom Overlay template:**

```java
/*
 * Copyright © Wynntils 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays;

import com.mojang.blaze3d.platform.Window;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
// only if configs exist:
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public class <Name>Overlay extends Overlay {

    @Persisted
    private final Config<<Type>> <fieldName> = new Config<>(<default>);

    public <Name>Overlay() {
        super(
                new OverlayPosition(
                        <verticalOffset>,
                        <horizontalOffset>,
                        VerticalAlignment.<TOP|MIDDLE|BOTTOM>,
                        HorizontalAlignment.<LEFT|CENTER|RIGHT>,
                        OverlayPosition.AnchorSection.<SECTION>),
                new OverlaySize(<width>, <height>));
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker, Window window) {
        // TODO: implement <Name>Overlay rendering
    }
}
```

**Default sizes:** 150×30 for TextOverlay, 150×50 for custom Overlay.

**OverlayPosition anchor grid:**

```
TOP_LEFT    | TOP_MIDDLE    | TOP_RIGHT
MIDDLE_LEFT | MIDDLE        | MIDDLE_RIGHT
BOTTOM_LEFT | BOTTOM_MIDDLE | BOTTOM_RIGHT
```

`verticalOffset`: positive = down from anchor, negative = up.
`horizontalOffset`: positive = right from anchor, negative = left.

### File 2 — Feature field annotation

Add to the owning Feature class body (after existing fields):

**Single overlay:**

```java
// Add imports:
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.annotations.RegisterOverlay;
import com.wynntils.overlays.<Name>Overlay;
import com.wynntils.utils.type.RenderElementType;

// Add field:
@RegisterOverlay(renderType = RenderElementType.<TYPE>)
private final Overlay <camelName>Overlay = new <Name>Overlay();
```

**Group overlay:**

```java
// Add imports:
import com.wynntils.core.consumers.overlays.annotations.OverlayGroup;
import com.wynntils.overlays.<Name>Overlay;
import com.wynntils.utils.type.RenderElementType;
import java.util.ArrayList;
import java.util.List;

// Add field:
@OverlayGroup(instances = <N>, renderType = RenderElementType.<TYPE>)
private final List<<Name>Overlay> <camelName>Overlays = new ArrayList<>();
```

**RenderElementType selection:**

| Overlay content | Use |
|-----------------|-----|
| General HUD / most overlays | `CHAT` (default — omit `renderType` to use default) |
| Replaces/augments scoreboard | `SCOREBOARD` |
| Replaces/augments action bar | `ACTION_BAR` |
| Replaces/augments boss bars | `BOSS_BARS` |
| Replaces/augments hotbar | `HOTBAR` |
| Crosshair area | `CROSSHAIR` |
| Player tab list | `PLAYER_TAB_LIST` |
| World-space / camera overlay area | `CAMERA_OVERLAYS` |
| Replaces/augments title display | `TITLE` |
| Must render after all GUI | `GUI_POST` |

### File 3 — `en_us.json` additions

**Path:** `common/src/main/resources/assets/wynntils/lang/en_us.json`

Insert at the correct **alphabetical position** — never append to end.

```json
"feature.wynntils.<featureCamel>.overlay.<overlayCamel>.name": "<user-provided display name>",
"feature.wynntils.<featureCamel>.overlay.<overlayCamel>.<fieldName>.description": "<user-provided>",
"feature.wynntils.<featureCamel>.overlay.<overlayCamel>.<fieldName>.name": "<user-provided>"
```

**Key derivation:**
- `<featureCamel>` = feature class name minus "Feature", first letter lowercased
  (e.g., `SpellCooldownOverlayFeature` → `spellCooldownOverlay`)
- `<overlayCamel>` = overlay class name minus "Overlay", first letter lowercased
  (e.g., `SpellCooldownOverlay` → `spellCooldown`)

**Locale key format:**

| Thing | Key pattern |
|-------|-------------|
| Overlay name | `feature.wynntils.<featureCamel>.overlay.<overlayCamel>.name` |
| Config field name | `feature.wynntils.<featureCamel>.overlay.<overlayCamel>.<fieldName>.name` |
| Config field description | `feature.wynntils.<featureCamel>.overlay.<overlayCamel>.<fieldName>.description` |

### File 4 — `FeatureManager.java` (new features only)

If a **new** Feature was created in Step 4, register it (handled by `wynntils-feature` skill). Skip if attaching to an existing feature.

---

## Worked Example

**User:** "A text overlay showing how much time is left on the current spell cooldown, in the top-right corner. New dedicated feature."

**Interview result:**
- Class: `SpellCooldownOverlay`, camelName: `spellCooldown`
- Single instance
- TextOverlay
- New feature → `wynntils-feature` skill invoked → produces `SpellCooldownOverlayFeature` with `Category.OVERLAYS`
- No configs
- Display name: "Spell Cooldown"

---

**File 1** — `common/src/main/java/com/wynntils/overlays/SpellCooldownOverlay.java`:

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

---

**File 2** — `SpellCooldownOverlayFeature.java` field addition (after `wynntils-feature` skill scaffolds the class):

```java
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.annotations.RegisterOverlay;
import com.wynntils.overlays.SpellCooldownOverlay;

@RegisterOverlay // renderType omitted — defaults to CHAT
private final Overlay spellCooldownOverlay = new SpellCooldownOverlay();
```

---

**File 3** — `en_us.json` addition:

```json
"feature.wynntils.spellCooldownOverlay.overlay.spellCooldown.name": "Spell Cooldown"
```

---

**File 4** — `FeatureManager.java` (handled by `wynntils-feature` skill):

```java
registerFeature(new SpellCooldownOverlayFeature());
```

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Creating an overlay with no owning Feature | Every overlay needs a Feature — invoke `wynntils-feature` skill or attach to existing |
| Putting `@RegisterOverlay` on the overlay **class** | Annotation goes on the **field** inside the Feature class |
| Calling `registerOverlay()` manually | No such call needed — `@RegisterOverlay`/`@OverlayGroup` fields are discovered automatically |
| Wrong locale key format (e.g. `overlay.wynntils.spellCooldown.name`) | Correct: `feature.wynntils.<featureCamel>.overlay.<overlayCamel>.name` |
| Forgetting `getPreviewTemplate()` in a TextOverlay | Always implement both `getTemplate()` and `getPreviewTemplate()` |
| Adding `userEnabled` config | Base `Overlay` class already provides this — don't duplicate it |
| Locale keys out of alphabetical order | Find correct sorted position — never append to end |
| Forgetting `FeatureManager.init()` for a new feature | Handled by `wynntils-feature` skill; if creating manually, don't skip it |

---

## Rigid Constraints

- Steps 1–6 must run in order. Do not skip any.
- Step 2 (single vs group) is always asked.
- Step 3 (TextOverlay vs custom) is always asked with a one-sentence explanation of the difference.
- Step 4 (feature attachment) always offers attach-or-create; always invokes `wynntils-feature` skill for new features.
- File 2 (feature field annotation) is never skipped — every overlay needs it.
- Locale display name is always asked from the user in Step 6 — never invented.
- `getPreviewTemplate()` is always generated for TextOverlay — never omit it.
