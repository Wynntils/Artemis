# AGENTS.md

This file provides guidance to AI coding agents (Claude Code, Codex, Gemini CLI, etc.) when working with code in this repository.

## Build & Development Commands

```bash
# Format code (run before every commit — Spotless is enforced by CI)
./gradlew spotlessApply

# Check formatting without applying
./gradlew spotlessCheck

# Build all loaders
./gradlew buildDependents -x spotlessCheck -x test

# Run all tests
./gradlew test -x spotlessCheck --stacktrace --info

# Run a single test class
./gradlew :fabric:test --tests "TestCustomColor" -x spotlessCheck

# Full CI-equivalent workflow
./gradlew spotlessApply
find $(git ls-files -m) -size 0 -delete
bash utils/remove_unused_i18n.sh
./gradlew buildDependents -x spotlessCheck -x test
./gradlew test -x spotlessCheck --stacktrace --info
```

Build output: `fabric/build/libs/` and `neoforge/build/libs/`. Use the jar ending in `-fabric.jar` or `-neoforge.jar`.

Tests live in `fabric/src/test/java/` and use JUnit 5. Tests that need Wynntils internals call `WynntilsMod.setupTestEnv()` in `@BeforeAll`.

**Requires Java 21.** Always use `./gradlew` (bundled wrapper), not a system Gradle install.

## Spotless Rules

Two rules cannot be auto-fixed and will fail CI:
- **No wildcard imports** — must be removed manually
- **No IntelliJ annotations** (`org.jetbrains.annotations.*`) — must be removed manually

Everything else (license headers, import order, unused imports, empty lines after `{`) is auto-fixed by `spotlessApply`.

Optional pre-commit hook: `git config core.hooksPath utils/git-hooks`

## Architecture

### Multi-Loader Layout

```
common/   — all game logic (primary module; ~95% of code lives here)
fabric/   — Fabric entrypoint + tests
neoforge/ — NeoForge entrypoint
```

Code in `common/` is loader-agnostic. Loader-specific code is minimal and only in `fabric/` or `neoforge/`.

### Component System

All major subsystems are static singletons in four registry classes, accessed directly anywhere in the codebase:

| Registry | Type | Purpose |
|----------|------|---------|
| `Managers` | `Manager` subclasses | Core framework (config, keybinds, networking, features, JSON, etc.) |
| `Models` | `Model` subclasses | Wynncraft game state (character, combat, items, territories, etc.) |
| `Handlers` | `Handler` subclasses | Low-level game event parsing (chat, scoreboard, action bar, tooltips, etc.) |
| `Services` | `Service` subclasses | External integrations (Athena API, Hades protocol, map data, Discord, etc.) |

Example access: `Models.Character.getActiveCharacter()`, `Managers.Notification.queueMessage(...)`, `Handlers.Chat.addRawChatLine(...)`.

The distinction: **Handlers** parse raw Minecraft events into structured data; **Models** maintain game state derived from that data; **Features** react to game state to provide player-facing functionality; **Services** handle external communication.

### Features

Features are the primary way to add user-facing functionality. Each feature:
- Extends `Feature` and is annotated with `@ConfigCategory`
- Uses `@Persisted` + `Config<T>` for user settings
- Uses `@RegisterKeyBind` + `KeyBind` for keyboard shortcuts
- Is manually registered in `FeatureManager.init()` — **features are not auto-discovered**
- Has locale strings in `common/src/main/resources/assets/wynntils/lang/en_us.json`

See `docs/CREATING_FEATURES.md` for the full step-by-step guide.

### Mixins

`common/src/main/java/com/wynntils/mc/mixin/` contains Mixin classes that inject into Minecraft internals. Mixins registered in `common/src/main/resources/wynntils.mixins.json`.

### Persisted Data

`@Persisted` on a `Config<T>` field makes it user-configurable (shown in settings UI, saved to disk). `@Persisted` on a `Storage<T>` field persists data silently (not shown in UI).

## Code Style

- Always use braces for control statements. Exception: single-line `if` with only `return`/`continue`/`break` and no `else`.
- Keep `final` and non-final fields in separate blocks; one field declaration per line.
- Method order: `public` (including `@SubscribeEvent`) → `protected` → `private`.
- Adapt style to surrounding code — conventions vary slightly by package.

## PR Requirements

PR **titles** must follow [Conventional Commits](https://www.conventionalcommits.org/) (e.g., `feat:`, `fix:`, `refactor:`). This is required for release changelogs and auto-build. Commit messages within a PR are not subject to this rule.

## AI Policy

Do not use AI to write PR descriptions or issue text. The project maintainers prefer short, human-written descriptions — an AI-generated wall of text for a one-line fix wastes reviewer time.

AI-generated code is allowed, but must be reviewed and understood before submitting. If you can't explain the code, the maintainers won't be able to either.
