# Changelog

## Unreleased

## 0.5.0 - Item Transport

- Added the Item Cannon, a powered nine-slot transport machine.
  - Sends items to a configured target inventory every eight ticks.
  - Supports high-power full-stack transfers and persists its target, inventory, and energy buffer.
  - Added item-handler and energy capabilities, recipe, advancement, blockstate, models, and localization.

## 0.4.0 - Detection Utilities

- Added the Player Detector, a powered proximity sensor with configurable range and binary or analog redstone output.
  - Detects players within up to 64 blocks after a five-second reaction delay.
  - Analog mode outputs up to 15 based on the number of nearby players.
- Added the Smoke Detector, a powered fire sensor with an eight-block detection range.
  - Emits a full redstone alarm while fire or soul fire is nearby.
  - Reports a low-battery state to automation and persists its energy buffer.
- Added registration, energy capabilities, recipes, advancements, models, blockstates, and English localization for both detectors.

## 0.3.0 - Decorative Storage

- Added the DecoTank: A decorative fluid storage and display block.
  - Stores up to 16 buckets (16,000 mB) of any fluid type.
  - Provides comparator output proportional to fill level (0-15).
  - Added shaped recipe: 4 iron ingots + 4 glass panes + 1 glass block = DecoTank.
  - Persists fluid amount and type on block destruction.

- Added DecoTank blockstates, models, item models, English localization, and recipe-book advancement triggers.

## 0.2.0 - Utility Machines

- Added the Grindstone machine: A rotational-powered grinding block for processing materials.
  - Accepts 2 input/output slots, requires 100 FE per operation cycle, and grinds items into products based on a recipe registry.
  - Integrated with NeoForge energy capability network; exposes stored energy and accepts FE input.
  - Added shaped recipe: 9 iron ingots + cobblestone = Grindstone.
  - Basic recipes: cobblestone → gravel, gravel → sand.
  - Added comparator output proportional to Grindstone progress (0-15).
  
- Added the Winder machine: A power-conditioning device for speed and flow regulation.
  - Buffers up to 50,000 FE with 160 FE/tick input and output capacity.
  - Acts as an intermediate regulator between generators and machinery.
  - Added shaped recipe: 1 copper block + 5 iron ingots + 4 redstone blocks = Winder.
  - Comparator output shows active operation status.
  
- Added the Blower machine: An air/pressure generation system for pneumatic operations.
  - Requires 80 FE/tick to operate, stores up to 50,000 FE.
  - Generates directional air flow for material transport and pneumatic systems.
  - Added shaped recipe: 1 copper block + 4 iron ingots + 4 pistons = Blower.
  - Comparator output shows operating state (0 = idle, 15 = operating).

- Added Grindstone, Winder, and Blower blockstates, models, item models, English localization, and recipe-book advancement triggers.

## 0.1.0 - Initial Power System

- Renamed the user-visible mod and distributable JAR to `Foundations-RotaryCraft`; the `rotarycraft` mod ID and resource namespace remain unchanged.
- Added recipe-book advancements for all native power-network crafting recipes, with a GameTest covering recipe registration and results.
- Added a NeoForge 1.21.1 / Minecraft 1.21.1 Gradle project targeting Java 21.
- Added the NeoForge mod entry point and generated mod metadata.
- Added a native energy cable and energy cell with buffered NeoForge energy capability transfer.
- Added a fuel-powered generator that accepts vanilla furnace fuels, exposes energy output and an automation fuel slot, and persists its buffer and burn state.
- Added crafting recipes and lit/unlit block models for the power cable, cell, and generator.
- Added generator-to-network energy transfer and a NeoForge GameTest that verifies a fueled generator charges an adjacent cable.
- Added a daylight solar generator with persisted energy storage, open-sky/daylight generation, balanced capability output to the existing cable/cell network, a shaped recipe, block/item models, lit state, and English localization.
- Added daylight solar-to-cable transfer and blocked-sky no-generation GameTests.
- Added a native wind generator with fixed, isolated tuning constants; generation requires open sky and an elevation at least 32 blocks above sea level, operates server-side, persists stored energy, and transfers output through the NeoForge energy capability.
- Added wind-generator recipe, original vanilla-texture block/item models, blockstates, English localization, and GameTests for elevated open-air output, blocked sky, and insufficient elevation.
- Added a native hydro generator that counts only face-adjacent water fluid blocks in already-loaded neighboring chunks, generates 8 FE per adjacent block per tick, persists its 50,000 FE buffer, and transfers up to 160 FE per tick through the existing cable/cell network.
- Added hydro-generator registration, creative-tab item, shaped recipe, vanilla-texture block/item models, lit state, English localization, and GameTests for water-powered cable output and dry non-generation.
- Added a steam generator that consumes vanilla water buckets and furnace fuels, produces 40 FE per productive tick, stores up to 50,000 FE, and distributes up to 160 FE per tick through the native energy network. Its bounded item inventory, burn/steam timers, energy buffer, and output rotation persist; empty buckets remain retrievable in the water slot, and fuel pauses when water is unavailable.
- Added steam-generator capability registration, crafting recipe, blockstates/models, English localization, and GameTests for fueled water-to-cable operation, no-input inactivity, and buffering against a full adjacent cell.
- Added a geothermal generator that converts lava buckets into 1,000 ticks of energy generation, retains the empty bucket, and distributes power through the native network; includes registration, recipe-book unlock, models, localization, and GameTests.
- Added a craftable redstone-controlled power switch with a zero-buffer, side-aware energy pass-through; powered switches expose no energy capability, update cable connections, and retain no energy to lose on removal.
- Added redstone-switch GameTests for enabled generator-to-cable transfer, powered blocking, and signal-driven restoration of transfer.
- Added a water-bucket-fed sprinkler that hydrates nearby farmland and bonemeal-grows one nearby vanilla or canola crop every 40 ticks; water storage persists and its recipe, models, and GameTest use only native APIs and vanilla textures.
- Added dense canola seeds, a craftable variant that plants a 3x3 patch with one seed, with native placement checks, recipe-book unlock, item model, localization, and a GameTest.
- Added connection-aware cable and cell models plus comparator output proportional to stored energy.
- Moved the new NeoForge Java package namespace to `Foundations.RotaryCraft`; original source and copyright attribution is preserved.
- Kept the legacy source tree and assets in place as migration references; their gameplay systems are not yet part of the new build.
- Began the single-mod migration. DragonAPI helper functionality and the legacy RotaryCraft power systems still need to be migrated into this project; no separate DragonAPI mod or project is configured.
