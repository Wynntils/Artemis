<div align="center">
<img src="https://upload.wikimedia.org/wikipedia/commons/d/d2/Artemis.png" width=30%>
<p align="center"><i>(Image in public domain)</i></p>
<br>
<a href="https://discord.gg/ve49m9J"><img src="https://discordapp.com/api/guilds/394189072635133952/widget.png"></a>
<a href="https://ci.wynntils.com/job/Artemis/"><img src="http://ci.wynntils.com/buildStatus/icon?job=Artemis"></a>
<a href="https://github.com/Wynntils/Artemis/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-AGPL%203.0-green.svg"></a>
</div>

About Artemis
========
> Artemis is the greek goddess of hunting and the moon, born of Zeus and Leto, twin of Apollo.

Artemis is a rewrite of **[Wynntils](https://github.com/Wynntils/Wynntils)** (informally referred to as "Legacy") in 1.18.2 using Architectury, to support **Fabric**, **Forge** and **Quilt**.

Downloading a release
========
You can download the latest build from our [releases page](https://github.com/Wynntils/Artemis/releases). You can also download the latest build from our [Modrinth Page](https://modrinth.com/mod/wynntils) and [CurseForge](https://www.curseforge.com/minecraft/mc-mods/wynntils).

Pull Requests
========
All pull requests are welcome. We'll analyse it and if we determine it should a part of the mod, we'll accept it. Note that the process of a pull request getting merged here is likely more strenuous than legacy. To begin, one pull request could be porting features from legacy.

We welcome all forms of assistance. =)
<br>

<strong>We require you to name your pull request according to the <a href="https://www.conventionalcommits.org/en/v1.0.0/#summary">Conventional Commits</a> specification. Otherwise, your pull request won't be visible in release logs, and will fail to auto-build. </strong>

Workspace Setup
========

### Initial Setup
To set up the workspace, just import the project as a gradle project into your IDE. You might have to run `./gradlew --refresh-dependencies` if your IDE does not automatically do it.

### Building
To build the mod just call the `buildDependents` and the artifacts should be generated in `fabric/build/libs`, `quilt/build/libs` and `forge/build/libs`. There are a lot of jars there, use the jar which has the respective loader at the end (eg. `wynntils-VERSION-fabric.jar`).

### Code Formatting
The code format is checked by Spotless using the Palantir engine. To make sure your commits pass the Spotless check, you can install a git pre-commit hook. The hook is optional to use. If you want to use it, you must tell git to pick up hooks from the directory containing the hook. To do this, run this from your repo root: `git config core.hooksPath utils/git-hooks`.

If you are using IntelliJ IDEA, it is recommended to install the [Palantir plugin](https://plugins.jetbrains.com/plugin/13180-palantir-java-format), to get proper formatting using the "Reformat code" command.

### Hot-swapping
Using the Hotswap Agent is recommended if you want to do live code editing. See [Hotswap Agent installation instructions](http://hotswapagent.org/mydoc_quickstart-jdk17.html),
but bear in mind that the instructions are incorrect (!). Don't "unpack" `hotswap-agent.jar`, instead
rename the downloaded jar file to `hotswap-agent.jar`. Finally, add `wynntils.hotswap=true` in your personal `gradle.properties` file.
By default, this is `C:\Users\<your username>\.gradle\gradle.properties` on Windows, or `~/.gradle/gradle.properties` on Linux/MacOS.


### Run Configurations and Authenticating
Architectury Loom currently only supports VSCode and IntelliJ IDEA. Eclipse if not supported by upstream at the moment. After running Initial Setup, run configurations should appear automatically (note that you might have to restart your IDE after Initial Setup).

The project has [DevAuth](https://github.com/DJtheRedstoner/DevAuth) set up by default. When you run the development run configurations, you will get a link to log in with your Microsoft account. After first login, you will be able to run the game like you would in a production environment.

### Quiltflower decompiler
The project has [LoomQuiltflower](https://github.com/Juuxel/LoomQuiltflower) set-up automatically. This is done so to highly increase the quality of decompiled sources. To use it, run `./gradlew quiltflowerDecompile`. After it finished, the decompiled Minecraft source will be in `minecraft-project-@common-clientOnly-named-sources.jar` You have to attach these sources in Intellij IDEA for Quiltflower to take effect.

License
========

Artemis is licensed over the license [GNU Affero General Public License v3.0](https://github.com/Wynntils/Artemis/blob/alpha/LICENSE)<br>
Unless specified otherwise, All the assets **are over Wynntils domain © Wynntils**.
