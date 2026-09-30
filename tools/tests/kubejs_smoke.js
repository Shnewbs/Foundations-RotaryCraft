ServerEvents.recipes(event => {
  event.custom({
    type: 'rotarycraft:grinding',
    ingredient: { item: 'minecraft:paper' },
    result: { id: 'minecraft:book', count: 2 },
    duration: 4,
    energy_per_tick: 2
  }).id('rotarycraft:ci_scripted_grinding')
})
