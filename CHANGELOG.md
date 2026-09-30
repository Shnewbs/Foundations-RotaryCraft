# Changelog

## Unreleased

- Added a NeoForge 1.21.1 / Minecraft 1.21.1 Gradle project targeting Java 21.
- Added the NeoForge mod entry point and generated mod metadata.
- Added a native energy cable and energy cell with buffered NeoForge energy capability transfer.
- Added a fuel-powered generator that accepts vanilla furnace fuels, exposes energy output and an automation fuel slot, and persists its buffer and burn state.
- Added crafting recipes and lit/unlit block models for the power cable, cell, and generator.
- Added generator-to-network energy transfer and a NeoForge GameTest that verifies a fueled generator charges an adjacent cable.
- Added a daylight solar generator with persisted energy storage, open-sky/daylight generation, balanced capability output to the existing cable/cell network, a shaped recipe, block/item models, lit state, and English localization.
- Added daylight solar-to-cable transfer and blocked-sky no-generation GameTests.
- Added connection-aware cable and cell models plus comparator output proportional to stored energy.
- Moved the new NeoForge Java package namespace to `Foundations.RotaryCraft`; original source and copyright attribution is preserved.
- Kept the legacy source tree and assets in place as migration references; their gameplay systems are not yet part of the new build.
- Began the single-mod migration. DragonAPI helper functionality and the legacy RotaryCraft power systems still need to be migrated into this project; no separate DragonAPI mod or project is configured.
