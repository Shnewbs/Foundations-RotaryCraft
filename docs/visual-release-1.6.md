# 1.21.1-1.6.0 visual and performance release

Status: implementation in progress; visual acceptance pending. Latest published version: 1.21.1-1.5.0.

## Coverage

All content already registered in the current port is in scope: DC engine, shafts, every gearbox ratio, generators, power cables/cells/switches, material processing, farming blocks/items and all other existing machines. Include each blockstate and inventory, held, dropped and placed appearance. Unimplemented machines remain in the gameplay parity roadmap.

Use the original RotaryCraft model and texture sources where applicable, preserving geometry, texture coordinates, scale and orientation. Do not treat original texture sheets applied to generic cubes as a completed port. Content without a matching original machine needs an explicit, finished visual design and must be identified as Foundations content. The preserved grindstone ID currently represents material processing; use the original Grinder visual reference, not the tool-repair Grindstone.

## Visual acceptance

- [ ] Record a complete registry-to-model/texture/render-reference mapping and per-entry status.
- [ ] Replace all temporary substitute textures and placeholder machine shapes.
- [ ] Restore original component pivots, UV mapping, material variants and correct input/output alignment.
- [ ] Restore applicable animations from original renderer/tick behavior; show static parts only once, animate moving parts without double rendering and stop them at zero speed.
- [ ] Verify horizontal/vertical mechanical placement and all supported orientations, active/idle/coasting states, inventory and held-item transforms.
- [ ] Inspect transparency, lighting, normals, face culling, seams and overlays in a real Minecraft client, with screenshots from multiple angles.
- [ ] Verify resource reload and multiplayer state synchronization; dedicated servers must not load client rendering classes.
- [ ] Confirm JEI presentation and Jade information remain correct with integrations present and absent.

## Performance acceptance

Profile the same representative scenes and machine networks before/after changes. Record hardware, render distance, counts, graphics settings and warm-up conditions. Compare frame time and server tick time, allocation and network traffic where relevant. No unsupported FPS improvement claims.

- [ ] Measure idle versus active machine rendering and larger repeated installations.
- [ ] Measure server recipe lookup, mechanical traversal and existing energy networks.
- [ ] Cache reusable model geometry and resources; avoid rebuilding meshes, repeated allocations and texture uploads every frame.
- [ ] Limit animation/state synchronization to needed updates; verify interpolation without excessive packets.
- [ ] Implement findings without changing mechanical results, recipe reload behavior, chunk loading policy or save compatibility.
- [ ] Document baseline, changes, repeatable results and remaining bottlenecks.

## Publication

Keep the current published mod_version while implementation is incomplete. Once all visual acceptance items and applicable performance/regression checks pass, bump to 1.21.1-1.6.0 with final changelog notes. The existing workflow publishes the tested JAR automatically. Python asset validation and server GameTests alone cannot mark this visual release complete.


## Implementation checkpoint

Eleven rest-pose model imports now use original atlas bytes and source geometry through NeoForge's cached OBJ loader. Mesh export preserves model-part rotations, mirrors, pivots and per-part texture dimensions, and deduplicates vertex/UV entries. `docs/legacy-model-import.json` records each source mapping. Import tests check base scale and outward face winding under rotated/mirrored transforms; asset validation checks OBJ/material/texture references.

This checkpoint is not visual acceptance. Legacy renderer motion and conditional pieces, remaining content, vertical transmission appearance, client UV/orientation/lighting checks and actual performance profiling are outstanding. No graphical Minecraft client or Xvfb display is currently available in this workspace, so a real-client visual check has not been performed. The mod_version remains at the published baseline to prevent a premature automatic 1.6 release.

The processing tick skips recipe lookup for empty inputs and selects the lowest matching recipe ID with a linear scan, retaining reload behavior without stream/comparator allocations. These are code-path reductions; FPS/tick-time improvements have not been measured.
