# Machine GUI and placement follow-up

The menus cover currently registered Foundations block entities. They use actual server inventories and status values; this is not complete legacy machine or original GUI-layout parity.

| Content | Menu access |
| --- | --- |
| Fuel generator | Fuel inventory, FE buffer, facing |
| Steam generator | Fuel/water inventories, FE buffer, facing |
| Geothermal generator | Bucket inventory, FE buffer, facing |
| Solar, wind, hydro generators | FE buffer, facing |
| Processor (`grindstone`) | Input/output, FE buffer, speed/torque, recipe progress, facing |
| Sorter | Input inventory, nine ghost samples grouped north/south/down, FE buffer |
| Defoliator and item cannon | Actual inventories, FE buffer, poison charge where applicable and facing |
| Detector | FE buffer, range/output, range and analog controls |
| Fan, harvester, smoke detector, item refresher | Existing FE and active state, supported facing |
| Winder | Existing progress state; legacy coil behavior is still pending |
| DC engine, shafts, gearboxes | Speed/torque, facing, gearbox mode where applicable |
| Cable, cell, switch | Existing FE buffer or redstone-controlled switch state |
| Sprinkler | Stored water; bucket interaction retained |
| Decorative tank | Real fluid amount/capacity; bucket/pipe access and synchronized fluid display |

Right-click without a block item opens a menu. Holding a block keeps adjacent placement available. Fluid containers interact directly with tanks, water buckets interact directly with sprinklers, and sneaking preserves hand interactions. Filters are samples, not stored items: clicking with a held stack copies a one-item sample without changing the cursor; clicking with an empty cursor clears it. Shift-clicking moves only stored inventory items.

Server checks cover real inventory transfer, output extraction, distance rejection, split 32-bit status, ghost filter conservation, generator facing, controls and break drops, plus tank simulation, capacity, mixed-fluid rejection, persistence and draining. The client gallery renders the production menu screen and captures processor/sorter screenshots and a filled tank; it has no player/world and therefore delegates rendering without vanilla container ticking. It does not replace multiplayer or modpack playtesting.

The lighting repair removes inappropriate full-block occlusion and gives moving meshes loaded-neighbor light samples. Models retain original geometry and texture maps. This client gallery uses full lighting; it is not an in-world screenshot validation of the user's exact setup.

Candidate validation: 70 server GameTests pass with integrations present and absent. The real client checks 416 blockstates and 30 item models, production menu rendering and resource reload. The versioned release repeats all required checks before automatic publication.
