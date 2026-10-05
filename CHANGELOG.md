# Changelog

## 26.3-1.8.0-beta.1

- Add a native Minecraft 26.3 / NeoForge 26.3.0.51-beta build using Java 25 and Gradle 9.2.1.
- Provide the initial 16-block core: six interim FE generators, cable, cell, switch, grinder, DC engine, shaft and four ideal gearboxes. Mechanical arithmetic and engine state are compiled from the same source files as 1.21.1.
- Implement transactional energy transfer with probe rollback, exact commit, loaded-only cable discovery and a shared network budget. Preserve full saved battery charge with bounded native Value IO.
- Add native item/energy automation ports, constrained machine inventory slots and reloadable grinding recipes using item templates.
- Add optional JEI grinding category/catalyst and Jade server-authoritative energy, progress and mechanical status. Send custom recipe content through NeoForge's native datapack synchronization.
- Generate current-format resources and item definitions from shared source assets. Models have complete parked geometry; native animations remain pending.
- Gate publication on server regressions, optional JEI/Jade checks and a real Minecraft client model/texture bake.
- This is a limited core preview, not full 1.21.1 feature parity. Utility/farming blocks, fluid tanks, native animations and the broader legacy gameplay migration remain pending. Use a new test world; omitted IDs make existing 1.21.1 worlds unsuitable for this preview.
- KubeJS has no published NeoForge 26.3 build on Modrinth as of 2026-10-05. Live KubeJS integration on this target remains blocked; 1.21.1 retains its installed KubeJS smoke test.
- EU/J/Create adapters and 26.4 compatibility are not claimed. See docs/version-targets.md and ROADMAP.md for the remaining beta work.

## 1.21.1-1.8.1

- Correct internal energy consumption in input-only machine buffers and restore full saved charge across ten machine types without applying transfer-rate limits. Clamp corrupt charge to capacity.
- Register missing blower and winder energy ports.
- Preserve per-tick network spend across topology rebuilds and avoid retaining adjacent air as consumer ports.
- Release regression coverage expands to 78 GameTests with JEI, Jade and KubeJS installed and absent, plus the real client model and machine GUI checks.

## 1.21.1-1.8.0

- Reduce cable cores and branches to 25% of their previous thickness, with matching selection and collision shapes.
- Route power across loaded cable networks directly to consumers instead of bouncing it between intermediate cable buffers.
- Share the 500 FE cable transfer budget across each connected component per server tick; rotate consumer priority to avoid a fixed preferred endpoint.
- Restore the complete saved battery charge, clamp corrupt values, and stop idle batteries circulating power into cables.
- Register grindstone energy and inventory automation capabilities so real cables can supply the processor.
- Refresh late-created neighboring energy ports and invalidate topology on block and chunk changes. Discovery is bounded to 4096 loaded nodes and shared within a server tick.
- Validation: 75 GameTests pass with JEI, Jade and KubeJS installed and absent; the Minecraft client model and machine GUI checks pass.
- This remains a development prerelease. Full legacy gameplay parity and native EU/J/Create adapters are still pending.

## Unreleased

## 1.21.1-1.7.0 - Machine GUIs, Lighting and Placement

- Fixed black moving machine parts in world lighting by removing full-block occlusion from partial models and using surrounding loaded-block light samples.
- Added horizontal placement facing to all six generator types; aligned partial machine collision/selection shapes and cached their rotated geometry.
- Added inventory/status GUIs for every currently registered machine block entity, with live FE, mechanical input, processing progress, fluid storage and detector status. Server buttons control supported facing, gearbox mode, detector range and analog mode.
- GUI transfers use real persisted inventories, preserve full 32-bit status values and close when the machine is removed or the player leaves reach. Breaking processors, sorters, defoliators and item cannons now drops stored inventory.
- Sorting filters are editable ghost samples and never consume, extract or drop real items. Shift-clicking inserts only into the actual input inventory.
- Replaced the decorative tank's inert name/amount fields with a 16,000 mB NeoForge fluid handler, bucket interactions, comparator updates, persisted fluid components and synchronized fluid display. Existing valid legacy tank data migrates on load.
- Holding a block preserves adjacent placement. Buckets directly interact with tanks and sprinklers, while sneaking retains hand interactions. Cables can visibly connect to neighboring third-party FE capability ports.
- Added server regressions for menu item conservation, ghost filters, 32-bit status, tank storage, generator facing, rotated controls and inventory drops, plus real client GUI screenshot checks.
- Validated 416 blockstates and 30 item models, resource reload, production GUI rendering and fluid rendering in the real client. All 70 server GameTests pass with JEI/Jade/KubeJS installed and absent.
- These GUIs expose the current port's implemented behavior. Original unregistered machines, complete upstream gameplay and provider-specific EU/Joule/kinetic converters remain roadmap work.

## 1.21.1-1.6.0 - Models, Animation and Visual Performance

- Restored twenty original machine/model atlas pairs with original pivots, rotations, mirrored faces and UVs. Inventory and held models include complete parked geometry; placed models draw moving parts only once.
- Added fifteen cached animation meshes: DC engine, shaft, all gearbox ratios, processor, fan, winder, defoliator and five generator models. Cranks, blades, waterwheel and all nine phased performance-engine pistons animate.
- Mechanical rotation follows synchronized speed, including coast-down and stopping. Processing uses its actual mechanical speed; current FE machines use active indicators, preserving their existing gameplay.
- Finished Foundations solar panels, insulated cable couplers, accumulator terminals and switch housings. Added transparent tank frames, eight canola growth models and original canola seed sprites.
- Added sprinkler spray, harvester targeting laser and inspected fan/harvester range overlays. Existing block-shaped blower, sorting and refresher machines retain their custom face artwork.
- Fixed original texture atlas stitching, and removed unused substitute textures. Added real Minecraft client CI coverage for 380 states and 30 items, missing sprites, assembled animations, multiple-angle screenshots and resource reload with JEI/Jade/KubeJS present.
- Cached geometry, normals, render types, rotation storage and animation rates. Limited speed synchronization to changed, staggered five-tick updates. Display-only network caching never affects live gameplay or loads chunks.
- Added a display-cache regression and repeatable five-node benchmark. A measured 5000-query batch dropped from 17.70 ms live traversal to 3.82 ms cached display lookup; this is not a whole-world FPS/tick claim. All 64 server GameTests pass with integrations present and absent.
- Full upstream gameplay parity, conditional coil/failure/material variants and universal cross-mod energy converters remain separate roadmap work. This release completes the visual implementation for currently registered content.

## 1.21.1-1.5.0 - Mechanical Processing Input

- Added a horizontal facing and rear shaft input to the existing material-processing block, preserving its `rotarycraft:grindstone` save ID.
- Mechanical operation requires the legacy Grinder thresholds: 128 Nm, 1 rad/s and 4096 W. The legacy tool-repair Grindstone is a separate machine and remains pending.
- A connected shaft selects mechanical operation, including when stopped or underpowered; FE cannot bypass those thresholds. Without a shaft connection, the interim FE recipe behavior remains available.
- Sufficient mechanical input advances reloadable grinding recipes without consuming FE. Losing power pauses progress; full or incompatible output still blocks processing.
- Jade displays the current shaft signal and required thresholds when mechanical input is connected. Native grinding recipes remain compatible with JEI and KubeJS.
- Added five GameTests covering sufficient/insufficient signals, torque versus watts, pause/resume, blocked output, and a real sided DC connection.
- Successful high-power processing tests inject a signal at the machine boundary; the current DC engine cannot reach the Grinder requirements. Stronger engines, legacy processing duration/recipes, tool repair and cross-mod converters remain pending.

## 1.21.1-1.4.0 - DC Engine Inertia and Persistence

- Replaced instant DC output with legacy server-tick spin-up: 32 rad/s per tick, capped at 256 rad/s and 4 Nm.
- Removing redstone now coasts the engine down using the legacy `speed / 256 + 1` decrement. Torque remains until the engine stops; shafts and gearboxes transmit the changing signal live.
- Saved engine speed and torque resume across loads, with clamping of invalid values. Existing engines without saved rotational data start stopped; transmission blocks still store no shaft signal.
- Jade now distinguishes powered, coasting and stopped DC engines using server data.
- Added arithmetic regressions plus world-ticker, save/load, corrupted-state and gearbox coast-down GameTests.
- Engine upgrades, sounds/renderers, gearbox materials/lubrication/failure, mechanical machine migration and cross-mod energy bridges remain pending.

## 1.21.1-1.3.0 - World Mechanical Transmission

- Added sided `rotarycraft:shaft_power` capability and live, non-buffered world transmission. Queries reject cycles, stop at unloaded chunks, and limit paths to 256 nodes without recursion or chunk loading.
- Added a redstone-controlled DC engine with legacy steady output of 256 rad/s and 4 Nm (1024 W), straight shafts and ideal 2:1, 4:1, 8:1 and 16:1 gearboxes.
- Gearboxes switch between speed reduction and speed increase with an empty-hand right click; facing and mode persist in blockstate. Signals are recomputed rather than saved.
- Added crafting recipes, loot, placeholder directional models and creative entries. Vanilla crafting recipes are available to JEI and KubeJS; Jade displays server-calculated speed, torque, watts and gearbox mode.
- Added mechanical GameTests for sided output, broken/reversed transmission, disabled sources, every gearbox ratio, integer truncation, unloaded chunks, cycles and bounded traversal.
- This is the first world mechanical foundation. Engine inertia/upgrades, material limits/failure, gearbox lubricant/damage, legacy recipes/visuals, machine conversion and FE/EU/joule/Create bridges remain pending.

## 1.21.1-1.2.0 - Recipe Integrations and Automatic Releases

- Added reloadable `rotarycraft:grinding` recipes with ingredient tags, outputs, duration and FE cost; KubeJS can add/remove/replace them through `event.custom`.
- Added a JEI Grinding category and machine catalyst, and server-authoritative Jade grinder energy/progress/status tooltips. Integrations remain optional to install.
- Fixed grinder energy consumption, saved energy loading, blocked/full output handling and component-safe result merging. Replaced the placeholder sand identity recipe with actual processing recipes.
- Added grinding GameTests and CI with integrations installed and absent, including a real KubeJS recipe script.
- Versions committed to master publish automatically after all CI checks pass, with the exact versioned JAR name, release notes and checksum. Published versions are preserved.
- Began the 1:1 mechanical foundation with independent torque/speed signals, safe long power products, legacy ideal gearbox ratios and machine thresholds; not yet wired to world blocks.
- Added ideal ElectriCraft voltage/current signal conversions using the legacy 8 Nm/amp constant, with overflow rejection and conservation checks; world networks remain pending.
- Added mechanical regression checks to Gradle check.
- Added the combined parity roadmap, including ElectriCraft and standard NeoForge FE converters.
- Restored tag-triggered GitHub Releases with Java 21 builds, resource checks, GameTests, exact changelog notes and JAR checksums.

## 1.21.1-1.1.0 - Asset Restoration

- Audited blockstate, block-model, item-model, parent-model, and local texture references; restored every missing machine texture and the Smoke Detector block model.
- Replaced ambiguous item-model parents with explicit references to their block models.
- Added original pixel-art machine textures and a frame-animated Fan rotor that runs only in the powered blockstate; its eight 16×16 frames advance once per game tick.
- Added `docs/visual-animation-audit.md` to compare legacy renderer/tick behavior across the ported machines and track remaining visual-fidelity work.
- Added `tools/validate_assets.py` to check resource JSON, model/blockstate references, local PNG integrity, and animation frame bounds.

## 1.21.1-1.0.0 - Item Refresher

- Added the powered Item Refresher, based on the legacy machine that keeps dropped items from despawning.
  - Uses 16 FE per tick and affects item entities within four blocks; it extends their lifespan while in range and gives stationary items a gentle upward nudge.
  - Stores up to 50,000 FE, exposes the NeoForge energy capability, and shows its active state while powered.
  - Added a crafting recipe, recipe-book advancement, block/item models, blockstate, loot, English localization, and original active/inactive pixel-art textures.
  - Added GameTests for recipe registration, powered lifespan refresh and energy use, range limits, and unpowered behavior.

## 0.9.0 - Defoliator

- Added the powered Defoliator, inspired by the legacy poison-fed vegetation-clearing machine.
  - Accepts poison potions through its item capability, stores up to 4,000 poison charge, and returns empty glass bottles.
  - Uses 8 FE per tick to clear eligible leaves, logs, saplings, plants, vines, and cactus within a bounded three-block radius; each cleared block consumes one poison charge and poisons nearby living entities.
  - Persists its energy, potion inventory, poison charge, and work progress; includes energy and item-handler capabilities, active/redstone indication, recipe-book advancement, block/item models, loot, and English localization.
  - Added original pixel-art block and item textures under `assets/rotarycraft/textures` and GameTests for recipe/capability registration and powered operation.

## 0.8.0 - Mob Harvester

- Added the powered Mob Harvester, which damages eligible living mobs in a four-block vertical column.
  - Excludes players and villagers, stops its targeting column at solid obstructions, consumes 8 FE per operating tick, and deals 6 damage once per second.
  - Stores up to 50,000 FE, indicates active targeting with its lit model state, and emits a full analog redstone signal while a valid target is present.
  - Added energy capability, recipe, recipe-book advancement, block/item models, blockstate, loot, and English localization.
  - Converted the legacy `Textures/TileEntityTex/harvestertex.png` atlas to the in-game block texture; the original atlas remains in the repository as the source asset.
  - Added GameTests for recipe registration, capability and energy persistence, powered damage, safe exclusions, obstruction handling, and unpowered behavior.

## 0.7.0 - Fan

- Added the Fan, a powered directional airflow machine inspired by the legacy RotaryCraft Fan.
  - Pushes entities in a bounded eight-block airflow path and stops at solid block collision shapes.
  - Stores up to 50,000 FE, accepts up to 160 FE per tick, and consumes 16 FE per operating tick.
  - Added energy capability, crafting recipe, recipe-book advancement, block/item models, blockstates, loot, and English localization.
  - Added GameTests for recipe registration, energy capability, directional entity movement, solid-block occlusion, and unpowered behavior.

## 0.6.0 - Sorting

- Added the Sorting Machine, a powered item router with nine persistent filter slots.
  - Routes matching items to three output paths and sends unmatched items downward.
  - Persists filters, input inventory, facing state, and energy.
  - Added energy and item-handler capabilities, recipe, advancement, models, blockstate, and localization.

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
