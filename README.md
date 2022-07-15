<div align="center">
<img src="https://upload.wikimedia.org/wikipedia/commons/d/d2/Artemis.png" width=30%>
<p><i align="center">(Image in public domain)</i></p>
<br>
<a href="https://discord.gg/ve49m9J"><img src="https://discordapp.com/api/guilds/394189072635133952/widget.png"></a>
<a href="https://ci.wynntils.com/job/Artemis/"><img src="http://ci.wynntils.com/buildStatus/icon?job=Artemis"></a>
<a href="https://github.com/Wynntils/Artemis/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-AGPL%203.0-green.svg"></a>
</div>

About Artemis
========
> Artemis is the greek goddess of hunting and the moon, born of Zeus and Leto, twin of Apollo.

This is a preliminary rewrite of **[Wynntils](https://github.com/Wynntils/Wynntils)** in 1.18.2 using Architectury, to support Fabric, Forge and Quilt.

Pull Request
========
<i>TODO</i>

Setup the Workspace
========
To set up the workspace, just import the project as a gradle project into your IDE
<br> To build the mod just call the ``buildDependents`` and the artifacts should be generated in `fabric/build/libs` and `forge/build/libs`. There are a lot of jars there, but the mod jars are the ones without a dashed suffix at the end.

The code format is checked by spotless. To make sure your commits pass the spotless check, you can install a git pre-commit hook. The hook is optional to use. If you want to use it, you must tell git to pick up hooks from the directory containing the hook. To do this, run this from your repo root: `git config core.hooksPath utils/git-hooks`.

Using the Hotswap Agent is recommended if you want to do live code editing. See [Hotswap Agent installation instructions](http://hotswapagent.org/mydoc_quickstart-jdk17.html),
but bear in mind that the instructions are incorrect (!). Don't "unpack" `hotswap-agent.jar`, instead
rename the downloaded jar file to `hotswap-agent.jar`.

<i>TODO Run Configurations and Authenticating</i>

License
========

Artemis is licensed over the license [GNU Affero General Public License v3.0](https://github.com/Wynntils/Artemis/blob/development/LICENSE)<br>
Unless specified otherwise, All the assets **are over Wynntils domain © Wynntils**.
