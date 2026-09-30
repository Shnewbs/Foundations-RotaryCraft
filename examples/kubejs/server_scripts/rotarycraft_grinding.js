// Copy into kubejs/server_scripts. Reload with /reload.
// This uses the standard custom recipe API; no separate addon is required.
ServerEvents.recipes(event => {
  event.remove({ id: 'rotarycraft:grinding/cobblestone' })
  event.custom({
    type: 'rotarycraft:grinding',
    ingredient: { tag: 'c:cobblestones' },
    result: { id: 'minecraft:gravel', count: 2 },
    duration: 80,
    energy_per_tick: 4
  }).id('rotarycraft:grinding/cobblestone')
})
