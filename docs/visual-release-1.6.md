# 1.21.1-1.6.0 visual and performance release

Scope: all currently registered content. The larger RotaryCraft/ElectriCraft gameplay port remains tracked separately in ROADMAP.md.

## Implementation

- Twenty original model/atlas pairs, including complete inventory models, source pivots, mirrors, rotations and per-part texture dimensions.
- Fifteen motion resources with stationary/moving geometry separated. Performance-engine cranks and all nine pistons retain original phase offsets.
- Original atlases explicitly stitched into Minecraft's block atlas; direct animation textures use identical source PNG bytes.
- Foundations electrical housings, solar collector, transparent tank and staged canola meshes replace generic machine/crop substitutes.
- Sprinkler spray, harvester targeting laser and inspected harvester/fan range overlays.
- Cache geometry, normals, render types, reusable quaternion and speed-dependent animation rate. Weak entity phase storage is reset on reload.
- Stagger five-tick changed-only mechanical speed packets. The processor receives actual mechanical speed; other interim FE machines use active indicators.
- Display-only shaft traversal memoized within a tick, bounded to 4096 entries per level; live gameplay remains uncached and chunk-safe.
- Empty processor inputs skip recipe lookup. Overlapping recipe selection uses a stable linear ID comparison, preserving reload semantics.

## Validation

The required `client-visual` CI job launches Minecraft/NeoForge with JEI, Jade and KubeJS under Xvfb/software Mesa. It bakes all 380 registered blockstates and 30 item models, rejects missing models and missing particle sprites, renders inventory models plus stationary/moving assemblies, captures multiple angles/phases including horizontal and vertical states, and exercises resource reload. CI artifacts retain screenshots and logs.

Python tests cover numeric expression restrictions, scale, rotated/mirrored face winding, repeated UVs, pivot/inverse operations, piston phases, finite moving meshes, atlas stitching and release rules. Server GameTests exercise mechanical behavior, recipes, save/load, same-tick gameplay independence from display caching and next-tick display cache expiry, with optional integrations installed and absent.

## Performance methodology

Server fixture: five-node DC/shaft network, Java 21, Ubuntu GitHub runner, 2000 warm-up queries of each path, five batches of 5000 queries, median batch duration. Compare live traversal with the display-only cache on the same network. An integration-enabled run measured 16.810748 ms live versus 4.668918 ms cached; a subsequent integration-absent run measured 17.699740 ms versus 3.819665 ms (4.63x query throughput). This measures repeated display lookups; it does not imply a 3.6x whole-world tick improvement.

Client fixture: 1280x720 Xvfb, GUI scale 2, software Mesa, no world/render distance involved; 30 inventory models and their available block assemblies with moving components, warmed for five ticks. Measured gallery rendering CPU time was approximately 6.6–8.0 ms per frame in the atlas validation run. This is a reproducible client rendering workload, not a hardware FPS guarantee or a before/after world benchmark.

Limitations: no claim of full upstream gameplay parity or universal FE/EU/J conversion. Upstream icons absent from preserved sources are identified as Foundations artwork in `visual-coverage.json`. Range overlays follow the current port's gameplay volumes. Restored animations do not fabricate inventories, failure modes or upgrades that the current gameplay lacks.

## Publication gate

Publish the exact versioned JAR and checksum only after both integration server builds and the real client job pass. GitHub releases remain immutable; failed checks prevent publication.
