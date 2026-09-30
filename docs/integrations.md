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
