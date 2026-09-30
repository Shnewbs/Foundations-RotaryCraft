# Visual implementation for 1.21.1-1.6.0

The shipped registry, rather than every unimplemented upstream machine, defines this visual release. `visual-coverage.json` maps all 29 blockstate resources and the canola seed item. `legacy-model-import.json` records original model sources and atlas paths.

| Content | Geometry and animation |
| --- | --- |
| DC engine, shaft, four gearbox ratios | Original models/atlases; source pivot operations; speed synchronized from the mechanical network. Shaft exponent 1.25, engine/gearbox exponent 1.05. Powered and coasting motion stops at zero speed. |
| Material processor (`grindstone` save ID) | Original Grinder model, not the upstream tool-repair Grindstone. Mechanical speed drives the wheel at the legacy 0.85 multiplier; interim FE operation uses an active speed indicator. |
| Fan, winder, defoliator | Original models, moving component groups and original pivots. The current FE machines use active speed indicators; fan multiplier 3. Winder has no coil inventory, so conditional coil geometry is omitted. |
| Power, steam, wind, hydro, geothermal generators | Original engine geometry adapted to the existing Foundations FE generators. Cranks, blades, waterwheel and nine phased performance-engine pistons animate while active. This does not add upstream engine gameplay. |
| Player/smoke detector, item cannon, mob harvester, sprinkler | Original static geometry and atlas UVs. Sprinkler emits water spray while active. Harvester shows its targeting laser; looking at it displays its current clear range. |
| Fan bounds | Looking at the fan displays its unobstructed airflow volume, matching current port gameplay dimensions. |
| Blower, sorting, item refresher | Block-shaped legacy machines with Foundations face artwork. No legacy moving renderer was found. Active refresher face artwork remains state driven. |
| Solar collector, power cable/cell/switch | Explicit Foundations geometry: divided collector cells, insulated cable couplers, terminals, accumulator frame and switch indicator. Shared vanilla materials are used as materials, not substitute machine models. |
| Decorative tank, canola | Transparent framed tank and eight staged plant meshes with leaves, yellow flowers and mature pods. Original individual crop/block icons are absent from the preserved source, so these are Foundations designs. |

Static placed meshes exclude moving parts. Inventory/held/dropped models retain all parked parts. The renderer caches geometry, normals, render types and animation rates, and clears its weak per-entity phase cache on resource reload. Direct animation textures and stitched block-atlas textures use the same original PNG bytes. Mechanical visual packets are changed-only snapshots at staggered five-tick intervals. Display traversal caching expires every tick and is never read by gameplay.

Future gameplay parity remains in ROADMAP.md: coil inventory, material/lubrication/failure variants, stronger engines, mechanical migration and explicit cross-mod converters. Those features cannot be implied by restored visuals.
