Foundations-RotaryCraft
=======================

This mod has sat way to long in 1.7.10 with no desire from Reika to port it, i for one would like to play it on the newer systems with newer mods. They marked it for 2017 and havent updated the 1.7.10 in years. So Ill port it for my own use case. As it should have been in originally, copy-writing a github minecraft mod is ridiculous, and i say fuck that. 

@author Reika

Copyright 2013-2017

All rights reserved.
Distribution of the software in any form is only allowed with
explicit, prior permission from the owner.
See [License.txt](License.txt) and the [official licensing page](https://sites.google.com/site/reikasminecraft/licensing) for more details.

NeoForge 1.21.1 port
===================

The current asset-restoration release is 1.21.1-1.1.0. It fills the missing machine texture/model references and adds an eight-frame, powered-only animated Fan rotor. The [legacy renderer and animation audit](docs/visual-animation-audit.md) records remaining visual-fidelity follow-ups without claiming full animation parity. Validate resource assets with `python tools/validate_assets.py`.

The Gradle project targets Minecraft 1.21.1 with NeoForge 21.1.252 and Java 21. The NeoForge entry point, mod metadata, native energy network (fuel, solar, wind, hydro, steam, and geothermal generators, cable, and cell), and canola crop live under `src/main`; run `gradlew.bat build` to build the migration. Canola seeds are crafted from wheat seeds and yellow dye; they plant on farmland, grow like vanilla crops, and produce additional seeds when mature. The fuel generator accepts vanilla furnace fuels by hand or automation, the solar generator produces energy during daylight under open sky, the wind generator produces energy outdoors at higher elevations, the hydro generator uses nearby water, the steam generator burns vanilla furnace fuel while consuming water buckets, and the geothermal generator burns lava buckets. Each power-network crafting recipe unlocks in the recipe book when its key ingredient is obtained.

The original code's license applies to this migration and its assets. See `License.txt` before redistributing or publishing any derivative. The canola crop models reference vanilla wheat-crop textures directly and do not include copied texture files.

See [CHANGELOG.md](CHANGELOG.md) for migration progress.

Versioned builds and releases
=============================

Use Java 21 and the Gradle wrapper for local builds and tests:

```bat
gradlew.bat build
gradlew.bat runGameTestServer
```

The distributable mod JAR is generated at `build/libs/Foundations-RotaryCraft-<mod_version>.jar`; source archives are not release assets.

Development continues on `master` toward a 1:1 combined RotaryCraft and ElectriCraft port. See [ROADMAP.md](ROADMAP.md) for current parity gaps and the order of work. Existing FE-powered machines remain approximations; the new mechanical signal API is a foundation and is not yet connected to world blocks.

Releases are automatic: update `mod_version` and its matching CHANGELOG.md section, then commit to `master`. CI validates resources, builds with Java 21, runs regression/GameTests with integrations present and absent, and publishes the checked version in GitHub Releases. The release contains `Foundations-RotaryCraft-<mod_version>.jar`, a SHA256 checksum and release-specific notes; GitHub supplies source archives. Manual tags are supported but no longer required. Published versions are not overwritten.

Version 1.21.1-1.2.0 adds data-driven grinding, KubeJS custom recipe support, JEI grinding display and Jade grinder status. See [integration documentation](docs/integrations.md). The mechanical/electrical core remains an API foundation; legacy gameplay parity and power adapters are still in progress.

Standard NeoForge Forge Energy compatibility is retained. The parity plan adds explicit FE/electrical/mechanical converters rather than replacing shaft torque and speed with FE. Other power APIs require dedicated adapters; universal compatibility has not been tested.



Pull Requests
=============
If you wish to suggest code, I actually do not merge pull requests, for three reasons:

One, I have had very bad experiences with errors on GitHub's part when accepting pull requests. The first and last time I did so, because of the timing of the PR relative to other commits (I noticed the PR about two weeks after it was submitted, and made commits, including to the changed files, in the interim), it corrupted the entire repository and the code on my machine, forcing me to roll back 3-6 months worth of commits. The only reason I did not lose that much code is because I had another copy on another computer, so I was able to roll it back (with the code safely backed up), then re-load the backup and make one commit with 3-6 months of changes. Because I do not regularly check GitHub, this situation would be very likely to repeat itself.

Two, the code is not written by me and thus I do not have a full understanding of how it works; even if I can trace the actual program logic, I know little of why it is designed the way it is - any programmer will agree that for any non-trivial algorithm, they understand their code better than anyone else - and much more importantly, nothing about how to change it if I need to fix or change it later, especially in the cases of Minecraft version updates. Use of supplied code has already introduced bugs and crashes for this exact reason.

Three, because of the complexity of much of RC code, I do not fully trust potential code submitters to understand all of the implications of their changes. Several times fixes for real issues have been suggested, including as PRs, that cause auxiliary problems, some cripplingly severe, because the author was not aware of the effects of their changes.

However, you can still suggest code, even in the form of a pull request if you like; I will simply add it manually.
Because of the license on this repository, the rights to any code submitted - this would apply even to PRs - is released to me under an unrestricted license.
