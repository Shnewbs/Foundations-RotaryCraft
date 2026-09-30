RotaryCraft
===========

This mod has sat way to long in 1.7.10 with no desire from Reika to port it, i for one would like to play it on the newer systems with newer mods. They marked it for 2017 and havent updated the 1.7.10 in years. So Ill port it for my own use case. As it should have been in originally, copy-writing a github minecraft mod is ridiculous, and i say fuck that. 

@author Reika

Copyright 2013-2017

All rights reserved.
Distribution of the software in any form is only allowed with
explicit, prior permission from the owner.
See [License.txt](License.txt) and the [official licensing page](https://sites.google.com/site/reikasminecraft/licensing) for more details.

NeoForge 1.21.1 port
===================

The Gradle project now targets Minecraft 1.21.1 with NeoForge 21.1.252 and Java 21. The NeoForge entry point, mod metadata, and native energy network (fuel, solar, wind, hydro, steam, and geothermal generators, cable, and cell) live under `src/main`; run `gradlew.bat build` to build the migration. The fuel generator accepts vanilla furnace fuels by hand or automation, the solar generator produces energy during daylight under open sky, the wind generator produces energy outdoors at higher elevations, the hydro generator uses nearby water, the steam generator burns vanilla furnace fuel while consuming water buckets, and the geothermal generator burns lava buckets. Each power-network crafting recipe unlocks in the recipe book when its key ingredient is obtained.

The power, solar, wind, hydro, steam, and geothermal generators expose NeoForge energy capabilities and feed adjacent cables/cells. A craftable redstone power switch is enabled while unpowered and exposes a side-aware, zero-buffer energy pass-through only while enabled. A redstone signal removes its capability, blocks input and output, and updates neighboring cable connection visuals; removing the switch cannot discard buffered energy because it stores none. The solar generator stores up to 50,000 FE, generates 32 FE per tick in daylight with an unobstructed sky, and distributes up to 320 FE per tick. The wind generator stores up to 50,000 FE, produces a fixed 16 FE per tick, and distributes up to 160 FE per tick. Its generation-rate, capacity, output, and elevation threshold are isolated tuning constants; it generates only when its block Y is at least 32 blocks above the dimension sea level, below the top build limit, and the block above it has unobstructed sky access. Wind generation is independent of time of day and weather, and the generator does not accept energy. The hydro generator stores up to 50,000 FE and generates 8 FE per tick for each of the six face-adjacent blocks whose fluid state is water, including flowing water, for up to 48 FE per tick. It does not count diagonal or more distant water. Before reading any neighbor fluid state or querying a neighbor energy capability, it checks that the neighbor position's chunk is already loaded; it never requests a chunk load. The steam generator stores up to 50,000 FE, produces 40 FE per tick while supplied with both a water bucket and vanilla furnace fuel, and distributes at most 160 FE per tick. Each bucket supplies up to 1,000 productive ticks (40,000 FE); its empty bucket remains in the water input slot for retrieval by hand or automation. Two bounded item-capability slots accept water buckets and vanilla furnace fuel, and its inputs, burn time, steam reserve, output rotation, and energy buffer persist. It pauses its fuel burn when no water is available and does not consume either input once its energy buffer is full. The geothermal generator stores up to 50,000 FE, converts a lava bucket into an empty bucket, produces 40 FE per tick for up to 1,000 ticks per bucket, and distributes at most 160 FE per tick. Its bucket and energy buffer persist, and the bucket can be retrieved by hand or automation. These generators buffer generated energy, accept no energy, and distribute across adjacent receiving capabilities, including cables and cells. Before querying an adjacent energy capability, the hydro, steam, and geothermal generators check that the neighboring chunk is already loaded; they never request a chunk load. All generation and transfer run from the server-side block-entity ticker. Cable and cell blocks show adjacent network connections and provide comparator output based on stored energy. These are clean, native implementations; no ElectriCraft code or assets are copied. Run `gradlew.bat runGameTestServer` for fuel, solar, wind, hydro, steam, geothermal, and redstone-switch transfer/operating-condition GameTests. The original 1.7-era sources, XML resources, textures, and sounds remain in their original locations as migration references. They are not part of the new Gradle source set yet: their Forge/FML APIs and DragonAPI helper implementations have not been ported. DragonAPI is to be absorbed into this mod rather than retained as a separate mod or project; the DragonAPI sources are not present in this checkout. The current artifact is therefore not a gameplay-complete RotaryCraft release.

The original code's license applies to this migration and its assets. See `License.txt` before redistributing or publishing any derivative.

See [CHANGELOG.md](CHANGELOG.md) for migration progress.

Versioned builds and releases
=============================

Use Java 21 and the Gradle wrapper for local builds and tests:

```sh
./gradlew clean build
./gradlew test
./gradlew runGameTestServer
```

On Windows, use `gradlew.bat` instead of `./gradlew`. The distributable mod JAR is `build/libs/rotarycraft-<mod_version>.jar`; source archives are not release assets.

To prepare a release, update `mod_version` in `gradle.properties` (currently `1.21.1-0.1.0`), commit the version change, and push a tag matching `v<mod_version>`—for example, `v1.21.1-0.1.0`. GitHub Actions builds on every push and pull request. A pushed version tag must exactly match the Gradle version; after the build succeeds, the workflow creates a GitHub Release and attaches that build's mod JAR. A mismatched tag fails validation and cannot publish a release.


Pull Requests
=============
If you wish to suggest code, I actually do not merge pull requests, for three reasons:

One, I have had very bad experiences with errors on GitHub's part when accepting pull requests. The first and last time I did so, because of the timing of the PR relative to other commits (I noticed the PR about two weeks after it was submitted, and made commits, including to the changed files, in the interim), it corrupted the entire repository and the code on my machine, forcing me to roll back 3-6 months worth of commits. The only reason I did not lose that much code is because I had another copy on another computer, so I was able to roll it back (with the code safely backed up), then re-load the backup and make one commit with 3-6 months of changes. Because I do not regularly check GitHub, this situation would be very likely to repeat itself.

Two, the code is not written by me and thus I do not have a full understanding of how it works; even if I can trace the actual program logic, I know little of why it is designed the way it is - any programmer will agree that for any non-trivial algorithm, they understand their code better than anyone else - and much more importantly, nothing about how to change it if I need to fix or change it later, especially in the cases of Minecraft version updates. Use of supplied code has already introduced bugs and crashes for this exact reason.

Three, because of the complexity of much of RC code, I do not fully trust potential code submitters to understand all of the implications of their changes. Several times fixes for real issues have been suggested, including as PRs, that cause auxiliary problems, some cripplingly severe, because the author was not aware of the effects of their changes.

However, you can still suggest code, even in the form of a pull request if you like; I will simply add it manually.
Because of the license on this repository, the rights to any code submitted - this would apply even to PRs - is released to me under an unrestricted license.
