Wynntils
========
[![Discord](https://discordapp.com/api/guilds/394189072635133952/widget.png)](https://discord.gg/ve49m9J)
[![CurseForge](https://cf.way2muchnoise.eu/short_wynntils.svg)](https://www.curseforge.com/minecraft/mc-mods/wynntils)
[![Modrinth](https://img.shields.io/modrinth/dt/Wynntils?label=modrinth)](https://modrinth.com/mod/wynntils)
[![GitHub](https://img.shields.io/github/downloads/Wynntils/Artemis/total?logo=github)](https://github.com/Wynntils/Artemis/releases)
[![License](https://img.shields.io/badge/license-LGPL%203.0-green.svg)](https://github.com/Wynntils/Wynntils/blob/main/LICENSE)

<div align="center">

<img src="http://cdn.wynntils.com/wynntils%20logo%20-%20w%20paintstroke.png" width=15% alt="Wynntils Logo">

</div>

Wynntils is a Wynncraft mod that seeks to enhance the user's gameplay with a variety of customizable options and additions.

Wynntils is based on Minecraft 1.21 using Architectury, and supports **Fabric** and **NeoForge**. This repository contains a rewrite (originally codenamed Artemis) of the original Wynntils mod, now known as [Wynntils Legacy](https://github.com/Wynntils/Wynntils-Legacy) .

Downloading a release
========
You can download the latest build from our [releases page](https://github.com/Wynntils/Wynntils/releases). You can also download the latest build from our [Modrinth Page](https://modrinth.com/mod/wynntils) and [CurseForge](https://www.curseforge.com/minecraft/mc-mods/wynntils).

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
To build the mod just run `./gradlew buildDependents` and the artifacts should be generated in `fabric/build/libs` and `neoforge/build/libs`. There are a lot of jars there, use the jar which has the respective loader at the end (eg. `wynntils-VERSION-fabric.jar`).

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

Wynntils is licensed under the license [GNU Lesser General Public License v3.0](https://github.com/Wynntils/Wynntils/blob/main/LICENSE)

Unless specified otherwise, All the assets **are over Wynntils domain © Wynntils**.
