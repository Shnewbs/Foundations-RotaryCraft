"""Map all shipped blockstate and item model assets to their visual implementation."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/rotarycraft'
original=json.loads((ROOT/'docs/legacy-model-import.json').read_text())
def models(value):
    if isinstance(value,dict):
        if 'model' in value:yield value['model']
        for child in value.values():yield from models(child)
    elif isinstance(value,list):
        for child in value:yield from models(child)
coverage={}
for path in sorted((A/'blockstates').glob('*.json')):
    name=path.stem
    implementation='original model and atlas' if name in original else 'Foundations native geometry'
    if name in ('blower','sorting','item_refresher'):implementation='legacy block-shaped machine; Foundations face artwork'
    coverage[name]={'implementation':implementation,'blockstate':str(path.relative_to(ROOT)),
                    'models':sorted(set(models(json.loads(path.read_text())))),
                    'item_model':f'rotarycraft:item/{name}' if (A/'models/item'/f'{name}.json').exists() else None,
                    'animation':f'rotarycraft:motion/{name}.json' if (A/'motion'/f'{name}.json').exists() else None}
    if name in original:coverage[name]['reference']=original[name]
coverage['_item_only']=[p.stem for p in sorted((A/'models/item').glob('*.json')) if p.stem not in coverage]
(ROOT/'docs/visual-coverage.json').write_text(json.dumps(coverage,indent=2)+'\n')
print(f'Mapped {len(coverage)-1} blockstate assets and {len(coverage["_item_only"])} item-only assets')
