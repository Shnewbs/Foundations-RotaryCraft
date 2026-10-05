"""Build version resources from authoritative 1.21.1 assets without copying unsupported registry data."""
import json,re,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'ports/26.3/build/generated/resources'
IDS={'dc_engine','shaft','gearbox_2','gearbox_4','gearbox_8','gearbox_16','power_cable','power_cell','power_generator','solar_generator','wind_generator','hydro_generator','steam_generator','geothermal_generator','grindstone','power_switch'}
if OUT.exists():shutil.rmtree(OUT)
shutil.copytree(ROOT/'src/main/resources/assets',OUT/'assets')
assets=OUT/'assets/rotarycraft'
for p in (assets/'blockstates').glob('*.json'):
 if p.stem not in IDS:p.unlink()
(assets/'items').mkdir(exist_ok=True)
for name in IDS:
 (assets/'items'/f'{name}.json').write_text(json.dumps({'model':{'type':'minecraft:model','model':f'rotarycraft:item/{name}'}}))
 # Complete parked geometry until the 26.x submit/extract animation backend is ready.
 item=assets/'models/item'/f'{name}.json'
 data=json.loads(item.read_text())
 if data.get('loader')=='neoforge:obj':
  data.pop('display',None)
  for suffix in ('','_lit'):
   target=assets/'models/block'/f'{name}{suffix}.json'
   if target.exists():target.write_text(json.dumps(data))
def modern(value):
 if isinstance(value,list):return [modern(x) for x in value]
 if not isinstance(value,dict):return value
 if set(value)=={'item'}:return value['item']
 if set(value)=={'tag'}:return '#'+value['tag']
 return {k:modern(v) for k,v in value.items()}
for p in (ROOT/'src/main/resources/data').rglob('*'):
 if not p.is_file() or p.suffix!='.json':continue
 value=modern(json.loads(p.read_text()))
 ids=set(re.findall(r'rotarycraft:([a-z0-9_]+)',json.dumps(value)))
 if ids-IDS-{'grinding'}:continue
 out=OUT/'data'/p.relative_to(ROOT/'src/main/resources/data');out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(value))
(OUT/'pack.mcmeta').write_text(json.dumps({'pack':{'description':'Foundations RotaryCraft 26.3','min_format':[97,1],'max_format':[121,0]}}))
print('26.3 resources prepared for',len(IDS),'registered blocks')

shutil.copytree(ROOT/'src/main/resources/data/rotarycraft/structure',OUT/'data/rotarycraft/structure')
