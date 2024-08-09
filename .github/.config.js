"use strict";
const config = require("conventional-changelog-conventionalcommits");

function whatBump(commits) {
    let releaseType = 2;

    // chore(bump-mc) or chore! -> major (0)
    // feat! or fix! -> minor (1)
    // otherwise -> patch (2)

    for (let commit of commits) {
        if (commit == null || !commit.header) continue;

        // We want to select the highest release type
        if (commit.header.startsWith("chore(bump-mc)") || commit.header.startsWith("chore!") || commit.header.startsWith("feat(major)")) {
            releaseType = 0;
            break;
        }

        if ((commit.header.startsWith("feat!") || commit.header.startsWith("fix!")) && releaseType > 1) {
            releaseType = 1;
        }
    }

    let releaseTypes = ["major", "minor", "patch"];

    let reason = "No special commits found. Defaulting to a patch.";

    switch (releaseTypes[releaseType]) {
        case "major":
            reason = "Found a commit with a chore(bump-mc) or feat(major) header.";
            break;
        case "minor":
            reason = "Found a commit with a feat! or fix! header.";
            break;
    }

    return {
        releaseType: releaseTypes[releaseType],
        reason: reason
    }
}

async function getOptions() {
    let options = await config(
        {
            types: [
                // Unhide all types except "ci" so that they show up on generated changelog
                // Default values:
                // https://github.com/conventional-changelog/conventional-changelog/blob/master/packages/conventional-changelog-conventionalcommits/writer-opts.js
                { type: "feat", section: "New Features" },
                { type: "feature", section: "New Features" },
                { type: "fix", section: "Bug Fixes" },
                { type: "perf", section: "Performance Improvements" },
                { type: "revert", section: "Reverts" },
                { type: "docs", section: "Documentation" },
                { type: "style", section: "Styles" },
                { type: "chore", section: "Miscellaneous Chores" },
                { type: "refactor", section: "Code Refactoring" },
                { type: "test", section: "Tests" },
                { type: "build", section: "Build System" },
                { type: "ci", section: "Continuous Integration", hidden: true },
            ]
        }
    );

    // Both of these are used in different places...
    options.recommendedBumpOpts.whatBump = whatBump;
    options.whatBump = whatBump;

    return options;
}

module.exports = getOptions();
