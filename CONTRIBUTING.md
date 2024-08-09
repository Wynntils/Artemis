# Introduction

### Welcome to the Wynntils Contribution Guide!

First off, thank you for considering contributing to Wynntils. We've written this page to help new contributors start off.

Following these guidelines helps to communicate that you respect the time of the developers managing and developing this open source project. In return, they should reciprocate that respect in addressing your issue, assessing changes, and helping you finalize your pull requests.

### What kinds of contributions do we look for?

We generally welcome almost any kind of contribution. New features and bugfixes are generally accepted. If you are looking for something to work on, but you don't have an idea, feel free to look at [our issues page](https://github.com/Wynntils/Wynntils/issues) or ask in [our discord.](https://discord.gg/wynntils). However, before attempting to fix any of the issues, please make a comment on the issue. There are some really complicated issues listed, and we wouldn't want you to get caught up in them.

### What kinds of contributions do we NOT look for?

Please, don't use the issue tracker for support questions. [Our discord](https://discord.gg/wynntils-mod) should be used for getting support.

# Ground Rules

We would like to maintain a respectful communication with our contributors. If you are not willing to adapt your code after our reviews, we are going to close your pull request. Respect goes both ways.

# Your First Contribution

At this point, you're ready to make your changes! Feel free to ask for help; everyone is a beginner at first :smile_cat:. A great place for this is either in your pull request, or in [our discord.](https://discord.gg/wynntils)

# Getting started

You can find some rules you have to follow to get your pull request in. Don't worry if you miss some of these at first, we will help you with it, but please try your best.

1. Only change what is necessary to achieve your goal. It is hard to review and bug-prone to add commits that change a lot of unrelated code, or do unnecessary changes.
2. Check if your code passes `spotlessCheck`. You can check this by running the `spotlessCheck` task in gradle. If there are changes to be made, `spotlessApply` can resolve some (but not all) of them.
3. Other than enforcing code format with `spotless`, we also have some general code style rules:
    * Always use braces for control statements such as loops or conditions. You can only emit braces after if statements, if the code fits in a single line, and you only call control flow statements (return, continue, break), and there is no else clause.
    * Try to keep your final and non-final fields in separate "blocks".
    * You should put every one of your field declarations in a new line (`private final int a, b` is disallowed).
    * Try to organize your methods in some way. We generally recommend public (including @SubscribeEvent), protected then private methods as an order, but this can change depending on your class.
    * Adapt your code style to the surrounding code. We have sometimes adopted more informal coding style rules than what are written here. Have a look at some different files and try to mimic what you see. This will decrease the amount of churn needed to get your PR accepted.

### Commit message conventions.
We use [conventional commit](https://www.conventionalcommits.org/en/v1.0.0/) messages. Check out other PR titles, if you are unsure what that means.

# How to report a bug

If you find a security vulnerability, do NOT open an issue. Open a ticket in [our discord](https://discord.gg/wynntils) instead.

You are free to create issues as long as you check whether you are using the latest version of the mod, are willing to write up a description of your issue. Please check [this section](#what-kinds-of-contributions-do-we-not-look-for) on what not to make issues for.

### What to include?

When filing an issue, make sure to answer these seven questions. We should be able to reproduce your issue with this information.

1. What version of the mod are you using? Please include a full version number.
2. What mod loader did you use?
3. Did you use any other mods?
4. What operating system are you using?
5. What did you do?
6. What did you expect to see?
7. What did you see instead?

# How to suggest a feature or enhancement

[Our discord](https://discord.gg/wynntils) should be used for suggesting features or enchancments.

# Code review process

Your code will get reviewed by the Wynntils Development Team. Multiple people may comment on your pull request. Your code will be merged after at least one team members that interacted with your pull request have approved your code.
