"""Reproducible native models for Foundations additions, using existing material assets."""
import json
from pathlib import Path
A=Path(__file__).resolve().parents[1]/'src/main/resources/assets/rotarycraft'
D=('down','up','north','south','east','west')
T={'metal':'rotarycraft:legacy/shaft','dark':'minecraft:block/black_concrete','glass':'minecraft:block/glass','red':'minecraft:block/red_concrete','green':'minecraft:block/lime_concrete','panel':'minecraft:block/blue_terracotta','leaf':'minecraft:block/green_concrete','flower':'minecraft:block/yellow_concrete'}
def box(a,b,material='metal',uv=None):
    if uv is None:uv=[0,0,4,4] if material=='metal' else [0,0,16,16]
    return dict(from_=a,to=b,faces={d:dict(texture='#'+material,uv=uv) for d in D})
def save(name,elements,transparent=False):
    for e in elements:e['from']=e.pop('from_')
    value={'parent':'minecraft:block/block','textures':dict(T,particle=T['metal']),'elements':elements}
    if transparent:value['render_type']='minecraft:cutout'
    (A/'models/block'/f'{name}.json').write_text(json.dumps(value,separators=(',',':'))+'\n')
# Insulated bus coupler and directed arms; connection states remain gameplay driven.
save('power_cable',[box([4,4,4],[12,12,12],'dark'),box([5,5,5],[11,11,11])])
save('power_cable_arm',[box([6,6,0],[10,10,8],'dark'),box([5,5,0],[11,11,2]),box([5,5,5],[11,11,7])])
# Accumulator case, terminal pairs, and separated reinforcing bands.
e=[box([2,1,2],[14,14,14],'dark'),box([1,0,1],[15,2,15]),box([1,12,1],[15,14,15])]
for x in (3,10):e.extend([box([x,14,6],[x+3,15,10]),box([x+0.5,15,7],[x+2.5,16,9],'red' if x==3 else 'dark')])
for x in (2,13):e.append(box([x,2,1],[x+1,12,15]))
save('power_cell',e)
# Solar collector on a rigid foot, independent cells and supporting rails.
e=[box([3,0,3],[13,2,13]),box([7,2,7],[9,7,9]),box([0,7,0],[16,8,16],'dark')]
for x in range(4):
    for z in range(4):e.append(box([x*4+.25,8,z*4+.25],[x*4+3.75,8.5,z*4+3.75],'panel'))
for x in (0,15):e.append(box([x,6,0],[x+1,8,16]))
for z in (0,15):e.append(box([0,6,z],[16,8,z+1]))
save('solar_generator',e);save('solar_generator_lit',[box([3,0,3],[13,2,13]),box([7,2,7],[9,7,9]),box([0,7,0],[16,8,16],'dark')]+[box([x*4+.25,8,z*4+.25],[x*4+3.75,8.5,z*4+3.75],'panel') for x in range(4) for z in range(4)]+[box([0,6,0],[1,8,16]),box([15,6,0],[16,8,16]),box([0,6,0],[16,8,1]),box([0,6,15],[16,8,16])])
for enabled in (False,True):
    save('power_switch_'+('enabled' if enabled else 'disabled'),[box([2,0,2],[14,3,14]),box([4,3,4],[12,10,12],'dark'),box([3,7,3],[13,9,13]),box([6,10,6],[10,14,10],'green' if enabled else 'red'),box([5,14,5],[11,15,11])])
# Canola stems, opposing leaves, yellow flower clusters and dark mature seed pods.
for stage in range(8):
    h=2+stage*1.5;e=[]
    for x,z in ((5,5),(11,10),(5,12)):
        e.append(box([x-.25,0,z-.25],[x+.25,h,z+.25],'leaf'))
        if stage>=2:
            e.extend([box([x-2,h*.4,z-.6],[x,h*.4+.2,z+.6],'leaf'),box([x,h*.65,z-.6],[x+2,h*.65+.2,z+.6],'leaf')])
        if stage>=4:
            for dx,dz in ((-1,0),(1,0),(0,-1),(0,1)):
                e.append(box([x+dx-.6,h,z+dz-.6],[x+dx+.6,h+.4,z+dz+.6],'flower' if stage<7 else 'dark'))
    save('canola_crop_stage'+str(stage),e)
(A/'models/item/canola_seeds.json').write_text(json.dumps({'parent':'rotarycraft:block/canola_crop_stage7'},indent=2)+'\n')
print('Built Foundations electrical models and eight canola growth stages')
