MapData rewrite
========

## TODO

* Old POIs that are not (properly) converted

All these should really have their json data converted to the new format, and just read from online,
as one (or possibly several) providers.
  private final Set<LabelPoi> labelPois = new HashSet<>(); // read from json (but sort of done in PlaceListProvider)
  private final Set<ServicePoi> servicePois = new HashSet<>(); // read from json (but sort of done in ServiceListProvider)
  private final Set<CombatPoi> combatPois = new HashSet<>(); // read from json (but sort of done in CombatListProvider)
  private final Set<CombatPoi> cavePois = new HashSet<>(); // read from json (NOT DONE!)

This should have a system for storing user-provided custom points in a local file, using the new format.
We might need to have a conversion system for the old POI data.
private final Map<CustomPoiProvider, List<CustomPoi>> providedCustomPois = new ConcurrentHashMap<>(); // not done!

The Compass system will need to be reworked to fit better into the new mapdata model.

* Attibrutes (like zoom fading) that are not (fully) implemented

Comments about current zoom levels:
        // zoom 3 (max zoomed in) up until 0.8 will keep service poi at 100% alpha (1.0f)
        // fading until we get to zoom 0.540, at which point it goes to 0.1f and disappears.
        // and finally at zoom 0.4727, it goes to 0.0f

        // fast travel is at 100% until 0.1667, and disappareas with 0.1f at:
        // 0.10858048, or possibly 0.11261813, and completely with 0.0f at 0.1,
        // which is max zoomed out

        // these happense since:
        // public final Config<Float> servicePoiMinZoom = new Config<>(0.8f);
        // public final Config<Float> fastTravelPoiMinZoom = new Config<>(0.166f);

        // so this point shows when it starts to fade, but we want to specify
        // the opposite, when it should be totally gone.
        // and then also possibly a fading speed...

        // we also have
        // public final Config<Float> poiFadeAdjustment = new Config<>(0.4f);
        // which is used to calculate where the item is completely faded out:
        // minZoom * (1 - poiFadeAdjustment),

How to map DisplayPriority enums to priority int levels:
    LOWEST, // 100
    LOW, // 300
    NORMAL, // 500
    HIGH, // 700
    HIGHEST // 900

## Design notes

/*
style application:
1) root style
2) category style, starting at top category and letting most specific category override
3) the feature's own style, starting at top category and letting most specific category override

json files can contain:
features == list of concrete map features (locations, paths or areas)
categories == list of category definitions
icons == icon name -> base64 png representation
*/

Examples of categories:

// wynntils:lootrun:chest:tier1
// wynntils:service:profession:scribing
// wynntils:service:identifier
// wynntils:npc:quest
// wynntils:personal:lootrunpath
// wynntils:personal:openedchest:tier3
// wynntils:personal:discovery:territory
// wynntils:personal:saved_bookmarks_poi ???


Artemis
========
[![Discord](https://discordapp.com/api/guilds/394189072635133952/widget.png)](https://discord.gg/ve49m9J)
[![CurseForge](https://cf.way2muchnoise.eu/short_wynntils.svg)](https://www.curseforge.com/minecraft/mc-mods/wynntils)
[![Modrinth](https://img.shields.io/modrinth/dt/Wynntils?label=modrinth)](https://modrinth.com/mod/wynntils)
[![GitHub](https://img.shields.io/github/downloads/Wynntils/Artemis/total?logo=github)](https://github.com/Wynntils/Artemis/releases)
[![License](https://img.shields.io/badge/license-LGPL%203.0-green.svg)](https://github.com/Wynntils/Artemis/blob/main/LICENSE)

<div align="center">
<img src="https://upload.wikimedia.org/wikipedia/commons/d/d2/Artemis.png" width=20%>

*(Image in public domain)*
</div>

> Artemis is the greek goddess of hunting and the moon, born of Zeus and Leto, twin of Apollo.

Artemis is a rewrite of **[Wynntils](https://github.com/Wynntils/Wynntils)** (informally referred to as "Legacy") in 1.20.2 using Architectury, to support **Fabric** and **Forge**.

Downloading a release
========
You can download the latest build from our [releases page](https://github.com/Wynntils/Artemis/releases). You can also download the latest build from our [Modrinth Page](https://modrinth.com/mod/wynntils) and [CurseForge](https://www.curseforge.com/minecraft/mc-mods/wynntils).

Pull Requests
========
All pull requests are welcome. We'll analyse it and if we determine it should a part of the mod, we'll accept it. Note that the process of a pull request getting merged here is likely more strenuous than legacy. To begin, one pull request could be porting features from legacy.

We welcome all forms of assistance. =)

**Make sure you set the title of your pull request according to the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/#summary) specification. Otherwise, your pull request won't be visible in release logs, and will fail to auto-build.**

Workspace Setup
========

### Initial Setup
To set up the workspace, just import the project as a gradle project into your IDE. You might have to run `./gradlew --refresh-dependencies` if your IDE does not automatically do it.

### Building
To build the mod just run `./gradlew buildDependents` and the artifacts should be generated in `fabric/build/libs` and `forge/build/libs`. There are a lot of jars there, use the jar which has the respective loader at the end (eg. `wynntils-VERSION-fabric.jar`).

### Code Formatting
The code format is checked by Spotless using the Palantir engine. When opening a PR, Spotless checks will be run automatically by GitHub Actions. This bot runs the `spotlessApply` which fixes all formatting errors that it can find. If such errors are found, the bot will then push a commit to your branch with these fixes.

However, it can be nice to fix the formatting locally before pushing. There are several ways to do this.

* The most basic is to run the same gradle target as the bot, `spotlessApply`. This can be done from the command line `./gradlew spotlessApply` or as a gradle target from your IDE.

* If you are using IntelliJ IDEA, you can install the [Spotless Gradle plugin](https://plugins.jetbrains.com/plugin/18321-spotless-gradle) to get an easy and quick way of running the Spotless target for the current editor.

* To make sure your commits always pass the Spotless check, you can install a git pre-commit hook. The hook is optional to use. If you want to use it, you must tell git to pick up hooks from the directory containing the hook. To do this, run this from your repo root: `git config core.hooksPath utils/git-hooks`.

* Finally, you can also use the Palantir formatting directly. This will skip the additional non-Palantir rules we have in Spotless, but these are few and unlikely to affect most cases. For IntelliJ, there is the [Palantir plugin](https://plugins.jetbrains.com/plugin/13180-palantir-java-format) that is supposed to do that, but at the moment it unfortunately seems to be broken.

### Hot-swapping
Using the Hotswap Agent is recommended if you want to do live code editing. Please note that at the moment, only version [Hotswap Agent 1.4.2](https://github.com/HotswapProjects/HotswapAgent/releases/tag/1.4.2-SNAPSHOT) works. See [Hotswap Agent installation instructions](http://hotswapagent.org/mydoc_quickstart-jdk17.html),
but bear in mind that the instructions are incorrect (!). Don't "unpack" `hotswap-agent.jar`, instead
rename the downloaded jar file to `hotswap-agent.jar`. Finally, add `wynntils.hotswap=true` in your personal `gradle.properties` file.
By default, this is `C:\Users\<your username>\.gradle\gradle.properties` on Windows, or `~/.gradle/gradle.properties` on Linux/MacOS.
If the file does not exist, you can create it yourself.
Don't forget to set the correct Java installation for your run configurations, and make sure to use [these](https://i.imgur.com/4VMFCM0.png) settings in IntelliJ IDEA.
After this, start the game using your run configurations, you should see some logs about HotswapAgent. If you don't see any, you did something wrong. To hotswap, hit the build button (build, not rerun, restart, etc.).

### Run Configurations and Authenticating
Architectury Loom currently only supports VSCode and IntelliJ IDEA. Eclipse is not supported by upstream at the moment. After running Initial Setup, run configurations should appear automatically (note that you might have to restart your IDE after Initial Setup).

The project has [DevAuth](https://github.com/DJtheRedstoner/DevAuth) set up by default. When you run the development run configurations, you will get a link to log in with your Microsoft account. After first login, you will be able to run the game like you would in a production environment. You can use an alt configuration by specifying `-Ddevauth.account=alt` in your JVM options, or by temporarily changing `.devauth/config.toml`.

### Vineflower decompiler
The project has [Vineflower for Loom](https://github.com/Juuxel/loom-vineflower) set-up automatically. This is done so to highly increase the quality of decompiled sources. To use it, run `./gradlew genSourcesWithVineflower`. After it finished, the decompiled Minecraft source will be in `minecraft-project-@common-merged-named-sources.jar` You have to attach these sources in Intellij IDEA for Vineflower to take effect.

License
========

Artemis is licensed under the license [GNU Lesser General Public License v3.0](https://github.com/Wynntils/Artemis/blob/main/LICENSE)

Unless specified otherwise, All the assets **are over Wynntils domain © Wynntils**.
