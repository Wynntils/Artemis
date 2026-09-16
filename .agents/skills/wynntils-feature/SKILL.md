---
name: wynntils-feature
description: Use when creating a new feature for the Wynntils Minecraft mod — interview the user for name, category, configs, keybinds, and profile defaults, then scaffold all required files including FeatureManager registration.
---

# Wynntils Feature Scaffolding

## Overview

Rigid skill. Run the 5-step interview in order, then produce all 4 file changes. Do not skip steps or files.

Infer sensible defaults. Ask only when a decision is genuinely ambiguous. Ask for locale display names/descriptions from the user — never invent them.

**Features are NOT auto-discovered.** Every feature must be manually registered in `FeatureManager.init()`. Skipping this step means the feature silently does nothing.

---

## Interview Phase

Run steps 1–5 in order. One question per message.

### Step 1 — Feature name + description

Ask: "What should this feature be called, and what does it do?"

Infer from the answer:
- **Class name:** `<PascalCase>Feature` (e.g., "enemy highlight" → `EnemyHighlightFeature`)
- **camelCase name** for locale keys (e.g., `enemyHighlight`)
- **Category:** match description to nearest category:
  - Damage, combat, enemies, spells → `COMBAT`
  - Screens, HUD, menus, text → `UI`
  - Bags, items, chest, shop → `INVENTORY`
  - Map, territory, waypoint → `MAP`
  - Party, guild, players → `PLAYERS`
  - Chat, messages → `CHAT`
  - Visual effects, particles → `EMBELLISHMENTS`
  - Overlays, rendering → `OVERLAYS`
  - Data, debug, dump → `DEBUG`
  - Otherwise → `UTILITIES`

### Step 2 — Category confirmation

Present: "I'll put this in the `<CATEGORY>` category. Does that look right?"

User confirms or picks from: `CHAT`, `COMBAT`, `UI`, `INVENTORY`, `MAP`, `OVERLAYS`, `PLAYERS`, `UTILITIES`, `EMBELLISHMENTS`, `DEBUG`.

### Step 3 — Config fields

Ask: "Does this feature need any user-configurable settings?"

If **yes**, loop:
1. Ask: "What should this setting control?"
2. Infer field name (camelCase), type, and default:
   - on/off toggle → `Config<Boolean>` default `true` or `false`
   - count or delay → `Config<Integer>` with a sensible default (e.g., 3, 100)
   - percentage or multiplier → `Config<Float>` with a sensible default (e.g., 0.5f, 1.0f)
   - color → `Config<CustomColor>` default e.g. `CommonColors.RED` (import `com.wynntils.utils.colors.CustomColor` and `com.wynntils.utils.colors.CommonColors`)
   - a fixed set of choices → define a nested enum, `Config<EnumName>`
3. Ask: "What should this setting be called in the UI? (name + short description)"
4. Ask: "Any more settings?" — repeat or continue to Step 4.

If **no**, proceed to Step 4.

**Do not add configs the user didn't ask for.** The base `Feature` class already provides `userEnabled` for on/off toggling — do not add a redundant `featureEnabled` config.

### Step 4 — Keybinds (ALWAYS ASK)

Ask: "Does this feature need any keybinds?"

If **yes**, loop:
1. Ask: "What should this keybind trigger?"
2. Infer:
   - Constant name: `SCREAMING_SNAKE_CASE` matching the feature and action (e.g., `TOGGLE_ENEMY_HIGHLIGHT`)
   - Category: match to feature's `@ConfigCategory` using the table below Step 4
   - Default key: **always `GLFW.GLFW_KEY_UNKNOWN`** — never assign a specific key; users set their own bindings
3. Ask: "What should this keybind be called in the keybind settings?"
4. Ask: "Any more keybinds?" — repeat or continue to Step 5.

If **no**, proceed to Step 5.

**Keybind category mapping** (match to the feature's `@ConfigCategory`):

| Feature category | Keybind category constant |
|-----------------|--------------------------|
| CHAT | `Managers.KeyBind.CHAT_CATEGORY` |
| COMBAT | `Managers.KeyBind.COMBAT_CATEGORY` |
| EMBELLISHMENTS | `Managers.KeyBind.UI_CATEGORY` |
| INVENTORY | `Managers.KeyBind.INVENTORY_CATEGORY` |
| MAP | `Managers.KeyBind.MAP_CATEGORY` |
| OVERLAYS | `Managers.KeyBind.OVERLAYS_CATEGORY` |
| PLAYERS | `Managers.KeyBind.PLAYERS_CATEGORY` |
| UI | `Managers.KeyBind.UI_CATEGORY` |
| UTILITIES | `Managers.KeyBind.UTILITIES_CATEGORY` |
| DEBUG | `Managers.KeyBind.DEBUG_CATEGORY` |

### Step 5 — Profile default

Propose based on feature type:
- Useful to most players → `ProfileDefault.ENABLED`
- Niche, advanced, or noisy → `new ProfileDefault.Builder().enabledFor(ConfigProfile.DEFAULT, ConfigProfile.NEW_PLAYER).build()`

Present: "I'll default this to `<proposed>`. Does that work?"

User confirms or adjusts.

---

## Scaffolding Phase

Produce all 4 file changes **in this order**. Do not skip any.

### File 1 — New feature Java file

**Path:** `common/src/main/java/com/wynntils/features/<category_lower>/<FeatureName>Feature.java`

Category folder mapping:
| Category | Folder |
|----------|--------|
| CHAT | `chat` |
| COMBAT | `combat` |
| UI | `ui` |
| INVENTORY | `inventory` |
| MAP | `map` |
| OVERLAYS | `overlays` |
| PLAYERS | `players` |
| UTILITIES | `utilities` |
| EMBELLISHMENTS | `embellishments` |
| DEBUG | `debug` |

**Template** (omit imports for things not used):

```java
/*
 * Copyright © Wynntils 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.<category_lower>;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.ProfileDefault;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
// only if configs exist:
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
// only if keybinds exist:
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.keybinds.KeyBindDefinition;
// only if selective profile default:
import com.wynntils.core.persisted.config.ConfigProfile;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.<CATEGORY>)
public class <FeatureName>Feature extends Feature {

    // @Persisted + Config<> for each config field (omit if none)
    @Persisted
    private final Config<<Type>> <fieldName> = new Config<>(<default>);

    // @RegisterKeyBind for each keybind (omit if none)
    @RegisterKeyBind
    private final KeyBind <keybindCamel>Keybind = KeyBindDefinition.<CONST>.create(this::<handlerMethod>);

    public <FeatureName>Feature() {
        super(<ProfileDefault>);
    }

    // Stub event handler — always include; infer best-guess event from description
    @SubscribeEvent
    public void on<InferredEvent>(<EventType> event) {
        if (!Models.WorldState.onWorld()) return;
        // TODO: implement <FeatureName> logic
    }

    // One handler method per keybind (omit if no keybinds)
    private void <handlerMethod>() {
        // TODO: implement
    }
}
```

**Event stub inference guide:**
| Feature description contains | Use event |
|-------------------------------|-----------|
| damage, health, combat | `DamageDealtEvent` |
| item, inventory, container | `ContainerSetContentEvent` |
| chat, message | `ChatMessageEvent` |
| screen, menu | `ScreenOpenedEvent` |
| tick, periodic | `TickAlwaysEvent` |
| player movement | `PlayerMoveEvent` |
| visual, render, glow, highlight | `TickAlwaysEvent` — rendering is done in renderers, not feature events |
| Unsure | `TickAlwaysEvent` — always safe fallback; do not invent event names |

### File 2 — `en_us.json` additions

**Path:** `common/src/main/resources/assets/wynntils/lang/en_us.json`

Insert keys at the **correct alphabetical position**. Never append to the end.

```json
"feature.wynntils.<camelName>.description": "<user-provided description>",
"feature.wynntils.<camelName>.name": "<human-readable name>",
"feature.wynntils.<camelName>.<fieldName>.description": "<user-provided config description>",
"feature.wynntils.<camelName>.<fieldName>.name": "<user-provided config name>",
"wynntils.keybind.<keybindId>": "<user-provided keybind name>"
```

One `.name` + `.description` per config field. One `wynntils.keybind.*` entry per keybind. Omit entries for things that don't exist.

> **Keybinds use a different namespace.** Feature and config keys are under `feature.wynntils.*`. Keybind keys are under `wynntils.keybind.*` — never `feature.wynntils.<camelName>.<keybindCamel>.name`.

**Locale key format:**
| Thing | Key pattern |
|-------|-------------|
| Feature name | `feature.wynntils.<camelName>.name` |
| Feature description | `feature.wynntils.<camelName>.description` |
| Config field name | `feature.wynntils.<camelName>.<fieldName>.name` |
| Config field description | `feature.wynntils.<camelName>.<fieldName>.description` |
| Keybind name | `wynntils.keybind.<keybindId>` |

`<keybindId>` is the `id` string (first arg) passed to `KeyBindDefinition.register()` (e.g., `"toggleEnemyHighlight"`).

### File 3 — `KeyBindDefinition.java` entry (keybinds only)

**Path:** `common/src/main/java/com/wynntils/core/keybinds/KeyBindDefinition.java`

```java
public static final KeyBindDefinition <CONST> = register(
        "<keybindCamel>",
        "<human-readable name>",
        Managers.KeyBind.<CATEGORY>_CATEGORY,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        true);
```

`<CATEGORY>_CATEGORY` options: `COMBAT_CATEGORY`, `WYNNTILS_CATEGORY`, `INVENTORY_CATEGORY`.

Skip this file entirely if no keybinds.

### File 4 — `FeatureManager.java` registration

**Path:** `common/src/main/java/com/wynntils/core/consumers/features/FeatureManager.java`

**Features are NOT auto-discovered. This step is mandatory.** Without it, the feature is never loaded.

Add `registerFeature(new <FeatureName>Feature());` inside the correct `// region` block, in alphabetical order:

```
// region chat          → CHAT
// region combat        → COMBAT
// region embellishments → EMBELLISHMENTS
// region inventory     → INVENTORY
// region map           → MAP
// region overlays      → OVERLAYS
// region players       → PLAYERS
// region ui            → UI
// region utilities     → UTILITIES
// region debug         → DEBUG
```

---

## Worked Example

**User:** "An enemy highlight feature that highlights nearby enemies with a configurable highlight color (default red) and a keybind to toggle it on/off."

**Interview result:**
- Class: `EnemyHighlightFeature`, camelName: `enemyHighlight`, category: `COMBAT`
- Config: `highlightColor: Config<CustomColor>(CommonColors.RED)` — name: "Highlight Color", desc: "Color used to highlight nearby enemies"
- Keybind: `TOGGLE_ENEMY_HIGHLIGHT` — name: "Toggle Enemy Highlight"
- Profile default: `ProfileDefault.ENABLED`

---

**File 1** — `common/src/main/java/com/wynntils/features/combat/EnemyHighlightFeature.java`:

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
        // TODO: implement EnemyHighlight logic
    }

    private void toggleHighlight() {
        // TODO: implement
    }
}
```

---

**File 2** — `en_us.json` additions (alphabetical position):

```json
"feature.wynntils.enemyHighlight.description": "Highlights nearby enemies with a configurable color.",
"feature.wynntils.enemyHighlight.highlightColor.description": "Color used to highlight nearby enemies",
"feature.wynntils.enemyHighlight.highlightColor.name": "Highlight Color",
"feature.wynntils.enemyHighlight.name": "Enemy Highlight",
"wynntils.keybind.toggleEnemyHighlight": "Toggle Enemy Highlight"
```

---

**File 3** — `KeyBindDefinition.java`:

```java
public static final KeyBindDefinition TOGGLE_ENEMY_HIGHLIGHT = register(
        "toggleEnemyHighlight",
        "Toggle Enemy Highlight",
        Managers.KeyBind.COMBAT_CATEGORY,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        true);
```

---

**File 4** — `FeatureManager.java` (inside `// region combat`):

```java
registerFeature(new EnemyHighlightFeature());
```

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| **Not adding to `FeatureManager.init()`** — feature silently does nothing | File 4 is mandatory. Features are not auto-discovered. |
| Setting a specific default keybind key (e.g., `GLFW_KEY_H`) | Always use `GLFW.GLFW_KEY_UNKNOWN` — users set their own bindings |
| Keybind locale key in feature namespace (e.g., `"feature.wynntils.<name>.<keybindCamel>.name"`) | Correct format: `"wynntils.keybind.<keybindId>"` |
| Adding a `featureEnabled` config field | The base `Feature` class already provides `userEnabled` — don't duplicate it |
| Adding configs the user didn't request | Scaffold only what was asked for (YAGNI) |
| `Config<>` field without `@Persisted` | Every `Config<>` field must be annotated `@Persisted` |
| Locale keys out of alphabetical order | Find the correct sorted position — never append to end |
| Feature registered in wrong `FeatureManager` region | Region must match `@ConfigCategory` value |
| Config missing `.description` locale key | Every config needs both `.name` and `.description` |
| `KeyBind.create(this::method)` with no matching method | Write the handler method stub in the same class |

---

## Rigid Constraints

- Steps 1–5 must run in order. Do not skip any.
- Step 4 (keybinds) is ALWAYS asked, even if description doesn't mention keys.
- All 4 file changes are mandatory. File 4 (`FeatureManager`) is especially easy to forget — do not skip it.
- Locale display names/descriptions are ALWAYS asked from the user. Never generate them without asking.
- The event handler stub is ALWAYS generated. If event type is uncertain, use `TickAlwaysEvent`.
- File 3 (`KeyBindDefinition.java`) is skipped ONLY if user says no keybinds in Step 4.
- Keybind default key is ALWAYS `GLFW.GLFW_KEY_UNKNOWN`. Never assign a specific key.
