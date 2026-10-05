# Version coverage and 26.4 preparation

The main Gradle project targets Minecraft 1.21.1 / NeoForge 21.1.252 / Java 21.
`ports/26.3` targets Minecraft 26.3 / NeoForge 26.3.0.51-beta / Java 25, with Gradle 9.2.1.
Both compile the same shaft power, DC engine state and operating-threshold sources. Native 26.x code uses transaction capabilities, ValueInput/ValueOutput persistence and item templates; it does not emulate removed APIs.

| Feature | 1.21.1 | 26.3 core preview |
| --- | --- | --- |
| Registered block IDs | Existing 29 | 16 listed below |
| FE cable routing | Loaded-only graph, shared 500 FE/tick budget, consumer priority | Native transaction implementation of the same routing policy |
| Cells | Full saved charge, bounded restoration, no idle cable circulation | Same behavior through native Value IO |
| Generators | Six interim FE generators | Six interim FE generators |
| Grinding | Reloadable recipes, FE fallback, mechanical input thresholds | Native recipe templates, FE fallback and mechanical thresholds |
| Mechanical core | DC engine, straight shafts, ideal 2/4/8/16 gearboxes | Same rules and sided capability |
| Inventories and GUIs | Existing machine controls/status menus | Five-slot native menus with valid input/output restrictions |
| JEI / Jade | Existing integrations with installed/absent regression checks | Native category and server status adapters; compatibility checks required |
| KubeJS | Installed smoke script validates custom grinding recipes | No NeoForge 26.3 KubeJS build available on Modrinth on 2026-10-05; live integration cannot yet be verified |
| Animated models | Existing renderers and model/GUI client check | Complete parked shared models; native animation submission remains pending |
| Full original RotaryCraft / ElectriCraft behavior | Incomplete; ROADMAP.md is authoritative | Incomplete; smaller core preview |
| EU, Joules, Create kinetic adapters | Pending | Pending |

Native IDs: `dc_engine`, `shaft`, `gearbox_2`, `gearbox_4`, `gearbox_8`, `gearbox_16`, `power_cable`, `power_cell`, `power_switch`, `power_generator`, `solar_generator`, `wind_generator`, `hydro_generator`, `steam_generator`, `geothermal_generator`, `grindstone`.

The 26.3 preview omits utility/farming blocks and the decorative fluid tank. It is intended for new test worlds: opening an existing 1.21.1 world in it would leave those block IDs unavailable. The 1.21.1 build preserves its existing registry IDs.

## Checks before publication

Each target has a separate artifact and automatic prerelease job. Release publication requires that target's build, server regressions and client model checks. Version names are `Foundations-RotaryCraft-1.21.1-<modversion>.jar` and `Foundations-RotaryCraft-26.3-<modversion>.jar`. Published versions are immutable. Superseded master runs defer publication to the newer checked build rather than tagging an unverified head or asking for broader token permissions.

## Preparing 26.4

1. Keep shared mechanical rules and source assets authoritative in the main project.
2. Keep changes to capabilities, persistence, recipes and rendering inside the native version project.
3. Pin the actual Minecraft/NeoForge toolchain; do not widen the compatibility range to claim untested 26.4 support.
4. When 26.4 APIs are available, update the native target and run transaction rollback, network conservation, processor, persistence, optional-mod and client model checks before publishing that version.
5. Complete the remaining native machine registrations, animation backend, custom status GUIs and integration reload checks before declaring feature parity or a complete beta.
