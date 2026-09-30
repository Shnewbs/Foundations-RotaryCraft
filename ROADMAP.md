# Combined RotaryCraft / ElectriCraft parity roadmap

Target: Minecraft 1.21.1, NeoForge 21.1.252, one Foundations-RotaryCraft JAR.
Development branch: master. Reference implementations: ReikaKalseki/RotaryCraft and ReikaKalseki/ElectriCraft.

This roadmap was established after checking master on 2026-09-30: no gameplay roadmap was tracked there. The existing visual audit remains in docs/visual-animation-audit.md.

## Current baseline

1.21.1-1.1.0 provides FE-powered approximations and partial assets. It is not a 1:1 port. Existing block IDs and saves must be preserved during migration. The first world mechanical foundation is available in 1.21.1-1.3.0; existing FE machines have not yet migrated.

## Required recipe and information integrations

- [x] Reloadable grinding recipes plus KubeJS `event.custom` example.
- [x] JEI grinding category and catalyst.
- [x] Server-authoritative Jade grinder energy, progress and running state.
- [ ] Extend these integrations to every machine as each legacy behavior is ported.
- [ ] Client visual validation for JEI and Jade; live recipe reload validation.

## Work order

- [x] Establish non-buffered shaft signal: independent speed, torque, long watt product.
- [x] Establish ideal gearbox ratio arithmetic and separate machine operating thresholds, with regression checks.
- [x] Register sided mechanical capability and initial DC engine, straight shafts and ideal gearboxes; server-authoritative live queries, loop rejection and unloaded chunk protection.
- [ ] Complete engine upgrades, shaft variants/materials, gearbox lubricant/damage/failure and reload/multiplayer validation. Facing and gearbox mode currently persist through vanilla blockstate storage.
- [x] Port DC engine steady output, spin-up, coast-down and bounded rotational-state persistence.
- [ ] Port remaining engine outputs/fuels/upgrades, material limits, lubricant, damage and failure from legacy sources.
- [x] Add initial material-processing shaft input under the preserved grindstone ID, with legacy Grinder torque/speed/watt thresholds.
- [ ] Complete processing duration/recipes, separate tool-repair Grindstone, remaining machine migration and explicit FE converters. Interim FE operation remains available without a connected shaft.
- [x] Add ideal ElectriCraft generator/motor signal conversion (8 Nm per amp), with conservation and overflow checks. World blocks remain pending.
- [ ] Integrate ElectriCraft voltage/current network, wire materials, resistance/losses, batteries, transformers, generators and motors from the reference implementation.
- [ ] FE interoperability: sided standard NeoForge EnergyStorage capability on electrical bridges, bounded buffers and rates, simulation without mutation, conservation and restart tests.
- [ ] EU adapters for the actual installed EU providers (for example Modern Industrialization and a supported NeoForge GregTech implementation): voltage tiers, packet limits and overload behavior.
- [ ] Joule adapters for the actual installed providers (including Mekanism where its API is available): use configured FE/J conversion rates, not an assumed universal rate.
- [ ] Create kinetic adapter: RPM, direction and stress capacity/impact; overspeed and overload handling; never treat stress units as stored FE.
- [ ] Additional tech-mod APIs identified from the target modpack; adapters must load optionally and work when the other mod is absent.

### Compatibility status

| System | Current status | Acceptance |
| --- | --- | --- |
| NeoForge FE / RF-style mods exposing FE | Existing energy capability integration; modpack testing pending | Input/output, simulation, bounded rate, persistence |
| RotaryCraft shaft power | Sided DC source, straight shafts, ideal gearboxes; machine conversion pending | Sided engines/transmission/machines and legacy limits |
| ElectriCraft | Legacy reference inspected; world network pending | Voltage/current, resistance, losses and converters |
| EU | Planned optional adapters; not implemented | Provider/version-specific voltage and packet behavior |
| Joules / Mekanism | Planned optional adapter; not implemented | Provider configuration and exact conversion accounting |
| Create | Planned optional kinetic adapter; not implemented | RPM/direction, stress load/capacity and failure behavior |
| Other tech mods | Inventory from the actual modpack required | Tested provider API; no unsupported universal claim |
- [ ] Audit every RotaryCraft and ElectriCraft registry entry, recipe, GUI, tool, fluid, world feature, sound and model against pinned upstream commits.
- [ ] Complete renderer/animation parity in docs/visual-animation-audit.md.
- [ ] Dedicated-server and client validation; migration and multiplayer regression checks.

## 1.21.1-1.6.0 — Visual restoration and performance

Visual implementation for current registered content is complete. See [release validation](docs/visual-release-1.6.md) and [coverage map](docs/visual-coverage.json).

- [x] Map all current blockstate/item resources to source geometry or explicit Foundations artwork.
- [x] Restore original machine geometry, atlas UVs, source pivots and parked inventory/held models.
- [x] Restore applicable moving parts, mechanical speed/coasting, FE active indicators and visible effects.
- [x] Bake all 380 states/30 items in Minecraft; inspect multi-angle assembly screenshots and verify resource reload.
- [x] Measure display-network lookup and client gallery rendering; cache resources and bound synchronization.
- [x] Pass integration-present/absent server regressions and require client CI before automatic publication.

Unimplemented gameplay variants remain under the earlier parity milestones; restored models do not imply those features are implemented.

## Acceptance rules

A feature is complete only after source comparison, behavior checks and required assets/recipes. Total FE cannot substitute for separate speed/torque thresholds. Conversion must never yield more energy than supplied; configurable conversion rates and efficiency must be shared by both directions. World-facing converters are pending, not implied by the signal API.

## Releases

Commit a new `mod_version` and matching CHANGELOG.md section to master. CI tests with KubeJS/JEI/Jade installed and absent, requires the real client model/render/reload check, then automatically creates `v<mod_version>` and a GitHub prerelease with `Foundations-RotaryCraft-<mod_version>.jar`, SHA256 checksum and version-specific notes. No manual tag is required. Ordinary commits keep building but do not overwrite published versions. Failed builds/tests never publish. The workflow also accepts explicit matching version tags and manual runs.

Current development milestone: 1.21.1-1.6.0. World mechanical transmission and an initial material-processing shaft input are connected. Other machines retain the interim FE baseline; the processing block also retains FE operation when no shaft is connected. Full machine behavior and explicit converters remain pending.

## Source checkpoints

- RotaryCraft upstream master: b2288638f7f4703bb4555f72a2b86f72407cc2ae.
- ElectriCraft upstream master: abd8bd061ef8962e41b363a03b64bfb0c9f1726f.
- Starting Foundations master: 4fda59037baa8ec5b2281f7ccd9d752a3d12e150.

Mechanical comparisons: Base/TileEntity/TileEntityPowerReceiver.java, TileEntities/Transmission/TileEntityGearbox.java and Registry/PowerReceivers.java. Electrical comparisons: ElectriCraft Network/WireNetwork.java and TileEntities/TileEntityGenerator.java / TileEntityMotor.java. Ideal conversions preserve legacy integer truncation and reject arithmetic overflow; network losses, overload explosions and world integration are still pending.
