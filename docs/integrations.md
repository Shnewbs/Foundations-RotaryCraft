# Required integrations

KubeJS, JEI and Jade support is a release requirement. They remain optional installed mods: Foundations-RotaryCraft must also start when they are absent. Every future processing machine must expose reloadable recipe data, a recipe viewer category and server-authoritative Jade information.

## Grinding (1.21.1-1.2.0)

Recipes live at `data/<namespace>/recipe/<path>.json`, using `rotarycraft:grinding`:

```json
{
  "type": "rotarycraft:grinding",
  "ingredient": { "item": "minecraft:cobblestone" },
  "result": { "id": "minecraft:gravel", "count": 1 },
  "duration": 100,
  "energy_per_tick": 1
}
```

Ingredients support Minecraft item tags and NeoForge ingredient codecs. Duration is 1–72000 ticks; energy cost is 1–100000 FE/t. These are the temporary FE baseline costs, not completed legacy mechanical parity. Recipes reload through the recipe manager and synchronize to clients. Overlapping recipes are selected by lexicographically smallest recipe ID.

KubeJS can add/remove/replace these recipes with `ServerEvents.recipes` and `event.custom`. See `examples/kubejs/server_scripts/rotarycraft_grinding.js`. The example is not installed into a player's world automatically. Typed KubeJS builders for future recipe types remain a follow-up.

JEI adds a Grinding category with tagged ingredients, result/count, duration, FE/t and the grinder catalyst. Jade provides server-side energy, progress and running status for the grinder. Other machine-specific Jade details and recipe categories must be added as their mechanics are ported.

CI builds/tests with KubeJS, JEI and Jade present and absent, and installs a KubeJS smoke-test script that adds a recipe used by a GameTest. A client visual test and a live `/reload` test remain separate validation requirements; do not infer them from server GameTests.

## Power adapters still pending

FE interoperability is the existing baseline. EU, joules, Create kinetic/stress and other mod-specific adapters remain on ROADMAP.md and are not implemented by this release.


## Mechanical transmission (1.21.1-1.3.0)

Place a DC Engine with its copper/redstone output face aimed along the shaft line. The facing is opposite the nearest direction the player is looking. Supply redstone to the engine. Straight shafts and gearboxes accept mechanical input only behind their output face. The initial source produces 256 rad/s at 4 Nm (1024 W); it is not an FE generator. Shaft paths up to 256 nodes are supported and never force-load chunks.

Gearboxes default to speed reduction/torque increase. Empty-hand right click toggles speed increase/torque reduction. Their configuration is stored in blockstate. Integer truncation follows the legacy ideal arithmetic, so increasing speed with insufficient torque can produce zero usable power. Material, lubricant, damage, upgrades and original recipes/models are pending; these six recipes and vanilla-material models are temporary scaffolding.

All six blocks have native crafting recipes that JEI discovers automatically. KubeJS can remove/replace them by their `rotarycraft:dc_engine`, `rotarycraft:shaft` and `rotarycraft:gearbox_2/4/8/16` IDs. Jade receives live speed, torque, wattage and mode from the server. Client visual validation remains pending. The material-processing block gains a shaft input in 1.21.1-1.5.0; other FE machines and electrical/cross-mod bridges remain roadmap work.


### DC engine inertia (1.21.1-1.4.0)

The DC engine reaches 256 rad/s after eight powered server ticks, increasing by 32 rad/s each tick. Removing redstone starts coast-down: subtract `speed / 256 + 1` each tick, keeping 4 Nm until speed reaches zero. From full speed it stops after 255 ticks. Reapplying redstone accelerates from the current speed. Jade shows redstone/running, coasting or stopped status.

Only the engine saves rotational state. Restored speed/torque are clamped to 0–256 rad/s and 0–4 Nm; a zero-speed engine has zero torque. Old saves with no rotational data start stopped. Saved engines do not simulate time while their chunks are unloaded. Transmission still recomputes its signal on demand. Sounds, legacy models, integrated gears and redstone upgrades remain pending.


### Processing shaft input (1.21.1-1.5.0)

The existing `rotarycraft:grindstone` block processes materials, so its mechanical requirements follow the legacy **Grinder** (128 Nm, 1 rad/s, 4096 W), rather than the legacy tool-repair **Grindstone**. The latter remains a separate parity task. Place the processing block facing you and aim a shaft's output into its rear face. Existing saves default to facing north, with input from the south.

A connected shaft selects mechanical operation even when its source is stopped or too weak. Its torque, speed and watts must all meet the thresholds; stored FE does not bypass them. With no connection, the interim FE mode remains available. Changing/removing a connection takes effect on the next machine tick. Progress pauses during insufficient power and resumes when sufficient power returns. Mechanical mode uses the recipe's configured duration and output, consuming no FE. Full legacy speed-dependent duration is pending.

The current DC engine produces only 1024 W and cannot run this machine, even after gearing. The successful mechanical-processing GameTests inject sufficient power at the input boundary; full end-to-end validation with a stronger engine remains pending. Jade displays input speed/torque/watts and thresholds. JEI and KubeJS continue to use the existing reloadable grinding recipe type.
