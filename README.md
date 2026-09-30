RotaryCraft
===========

The source code to my tech mod RotaryCraft.

@author Reika

Copyright 2013-2017

All rights reserved.
Distribution of the software in any form is only allowed with
explicit, prior permission from the owner.
See [License.txt](License.txt) and the [official licensing page](https://sites.google.com/site/reikasminecraft/licensing) for more details.

NeoForge 1.21.1 port
===================

The Gradle project now targets Minecraft 1.21.1 with NeoForge 21.1.252 and Java 21. The NeoForge entry point, mod metadata, and native energy network (fuel and solar generators, cable, and cell) live under `src/main`; run `gradlew.bat build` to build the migration. The fuel generator accepts vanilla furnace fuels by hand or automation, and the solar generator produces energy during daylight when placed under open sky. Both generators output energy through the NeoForge energy capability.

The power and solar generators expose NeoForge energy capabilities and feed adjacent cables/cells. The solar generator stores up to 50,000 FE, generates 32 FE per tick in daylight with an unobstructed sky, and distributes up to 320 FE per tick; it does not accept energy. Cable and cell blocks show adjacent network connections and provide comparator output based on stored energy. These are clean, native implementations; no ElectriCraft code or assets are copied. Run `gradlew.bat runGameTestServer` for the fuel-generator, daylight-solar-transfer, and blocked-sky solar GameTests. The original 1.7-era sources, XML resources, textures, and sounds remain in their original locations as migration references. They are not part of the new Gradle source set yet: their Forge/FML APIs and DragonAPI helper implementations have not been ported. DragonAPI is to be absorbed into this mod rather than retained as a separate mod or project; the DragonAPI sources are not present in this checkout. The current artifact is therefore not a gameplay-complete RotaryCraft release.

The original code's license applies to this migration and its assets. See `License.txt` before redistributing or publishing any derivative.

See [CHANGELOG.md](CHANGELOG.md) for migration progress.


Pull Requests
=============
If you wish to suggest code, I actually do not merge pull requests, for three reasons:

One, I have had very bad experiences with errors on GitHub's part when accepting pull requests. The first and last time I did so, because of the timing of the PR relative to other commits (I noticed the PR about two weeks after it was submitted, and made commits, including to the changed files, in the interim), it corrupted the entire repository and the code on my machine, forcing me to roll back 3-6 months worth of commits. The only reason I did not lose that much code is because I had another copy on another computer, so I was able to roll it back (with the code safely backed up), then re-load the backup and make one commit with 3-6 months of changes. Because I do not regularly check GitHub, this situation would be very likely to repeat itself.

Two, the code is not written by me and thus I do not have a full understanding of how it works; even if I can trace the actual program logic, I know little of why it is designed the way it is - any programmer will agree that for any non-trivial algorithm, they understand their code better than anyone else - and much more importantly, nothing about how to change it if I need to fix or change it later, especially in the cases of Minecraft version updates. Use of supplied code has already introduced bugs and crashes for this exact reason.

Three, because of the complexity of much of RC code, I do not fully trust potential code submitters to understand all of the implications of their changes. Several times fixes for real issues have been suggested, including as PRs, that cause auxiliary problems, some cripplingly severe, because the author was not aware of the effects of their changes.

However, you can still suggest code, even in the form of a pull request if you like; I will simply add it manually.
Because of the license on this repository, the rights to any code submitted - this would apply even to PRs - is released to me under an unrestricted license.
