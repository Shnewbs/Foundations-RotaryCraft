#!/usr/bin/env python3
"""Export original Techne model boxes and atlas UVs to NeoForge OBJ assets.

Keeps original texture PNG bytes. Exports stationary block meshes, complete parked
item meshes and cached renderer operations. Client validation is required in CI.
"""
import json
import math
import re
import shutil
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import legacy_animation
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/rotarycraft'
ENTRIES = {
    'dc_engine': (['Models/Engine/ModelDC.java'], 'Engine/dc.png', 90),
    'shaft': (['Models/Animated/ShaftOnly/ModelShaft.java'], 'Transmission/Shaft/shafttexs.png', 90),
    'gearbox_2': (['Base/ModelGearboxBase.java', 'Models/Animated/ModelGearbox.java'], 'Transmission/Gear/geartexs.png', 90),
    'gearbox_4': (['Base/ModelGearboxBase.java', 'Models/Animated/ModelGearbox4.java'], 'Transmission/Gear/geartexs.png', 90),
    'gearbox_8': (['Base/ModelGearboxBase.java', 'Models/Animated/ModelGearbox8.java'], 'Transmission/Gear/geartexs.png', 90),
    'gearbox_16': (['Base/ModelGearboxBase.java', 'Models/Animated/ModelGearbox16.java'], 'Transmission/Gear/geartexs.png', 90),
    'grindstone': (['Models/Animated/ModelGrinder.java'], 'grindertex.png', 0),
    'player_detector': (['Models/ModelDetector.java'], 'detectortex.png', 0),
    'smoke_detector': (['Models/ModelSmokeDetector.java'], 'smokedetectortex.png', 0),
    'item_cannon': (['Models/ModelItemCannon.java'], 'itemcannontex.png', 0),
    'power_generator': (['Models/Engine/ModelCombustion.java'], 'Engine/combtex.png', 90),
    'steam_generator': (['Models/Engine/ModelSteam.java'], 'Engine/steamtex.png', 90),
    'wind_generator': (['Models/Engine/ModelWind.java'], 'Engine/windtex.png', 90),
    'hydro_generator': (['Models/Engine/ModelHydro.java'], 'Engine/hydrotex.png', 90),
    'geothermal_generator': (['Models/Engine/ModelPerformance.java'], 'Engine/perftex.png', 90),
    'fan': (['Models/Animated/ModelFan.java'], 'fantex.png', 0),
    'winder': (['Models/Animated/ModelWinder.java'], 'windertex.png', 0),
    'defoliator': (['Models/Animated/ModelDefoliator.java'], 'defoliatortex.png', 0),
    'sprinkler': (['Models/ModelSprinkler.java'], 'sprinklertex.png', 0),
    'mob_harvester': (['Models/ModelHarvester.java'], 'harvestertex.png', 0),
}
NUMBER = r'[-+]?\d+(?:\.\d+)?[FfDd]?'

def numbers(value):
    result=[]
    for expression in value.split(','):
        factors=expression.strip().split('*')
        if not all(re.fullmatch(NUMBER, factor.strip()) for factor in factors):
            raise ValueError(f'Unsupported numeric expression: {expression}')
        result.append(math.prod(float(factor.strip().rstrip('FfDd')) for factor in factors))
    return result

def parts(path):
    source = (ROOT / path).read_text()
    source = re.sub(r'/\*[\s\S]*?\*/|//[^\n]*', '', source)
    dimensions = re.search(r'setTextureSize\((\d+),\s*(\d+)\)', source)
    if dimensions is None: raise ValueError(f'{path}: missing texture dimensions')
    width, height = int(dimensions[1]), int(dimensions[2])
    result = []
    matches = list(re.finditer(r'(\w+)\s*=\s*new LODModelPart\(this,\s*(\d+),\s*(\d+)\);', source))
    for index, match in enumerate(matches):
        name = match[1]
        end = matches[index + 1].start() if index + 1 < len(matches) else source.find('@Override', match.end())
        body = source[match.end():end if end != -1 else len(source)]
        box = re.search(rf'{name}\.addBox\(([^)]+)\)', body)
        pivot = re.search(rf'{name}\.setRotationPoint\(([^)]+)\)', body)
        rotation = re.search(rf'setRotation\({name},\s*([^)]+)\)', body)
        if box is None or pivot is None:
            raise ValueError(f'{path}: unsupported part {name}')
        b = numbers(box[1])
        if len(b) != 6:
            raise ValueError(f'{path}: unsupported box arguments {name}')
        mirrors = re.findall(rf'{name}\.mirror\s*=\s*(true|false)', body)
        result.append(dict(name=Path(path).stem+'_'+name, uv=[int(match[2]), int(match[3])], box=b,
                           pivot=numbers(pivot[1]), rotation=numbers(rotation[1]) if rotation else [0,0,0],
                           mirror=bool(mirrors and mirrors[-1] == 'true'), size=[int(v) for v in re.search(r'setTextureSize\((\d+),\s*(\d+)\)', body).groups()]))
    if not result:
        raise ValueError(f'{path}: no parts')
    return result

def rotate(point, angles):
    x, y, z = point
    a, b, c = angles
    y, z = y*math.cos(a)-z*math.sin(a), y*math.sin(a)+z*math.cos(a)
    x, z = x*math.cos(b)+z*math.sin(b), -x*math.sin(b)+z*math.cos(b)
    x, y = x*math.cos(c)-y*math.sin(c), x*math.sin(c)+y*math.cos(c)
    return x, y, z

def mesh(part, orientation):
    x, y, z, w, h, d = part['box']
    lo, hi = (x+w, x) if part['mirror'] else (x, x+w)
    vertices = [(lo,y,z),(hi,y,z),(hi,y+h,z),(lo,y+h,z),
                (lo,y,z+d),(hi,y,z+d),(hi,y+h,z+d),(lo,y+h,z+d)]
    transformed = []
    for vertex in vertices:
        v = rotate(vertex, part['rotation'])
        v = tuple(v[i]+part['pivot'][i] for i in range(3))
        # Original TERenderer: translation (0.5,1.5,0.5), scale (1,-1,-1).
        v = (v[0]/16, (24-v[1])/16-0.5, -v[2]/16)
        v = rotate(v, (0, math.radians(orientation), 0))
        transformed.append((v[0]+0.5,v[1]+0.5,v[2]+0.5))
    u, t = part['uv']
    definitions = [([5,1,2,6],(u+d+w,t+d,u+d+w+d,t+d+h)),
                   ([0,4,7,3],(u,t+d,u+d,t+d+h)),
                   ([5,4,0,1],(u+d,t,u+d+w,t+d)),
                   ([2,3,7,6],(u+d+w,t+d,u+d+w+w,t)),
                   ([1,0,3,2],(u+d,t+d,u+d+w,t+d+h)),
                   ([4,5,6,7],(u+d+w+d,t+d,u+d+w+d+w,t+d+h))]
    faces=[]
    width,height=part['size']
    for indices, (u1,v1,u2,v2) in definitions:
        coords=[(u2/width,v1/height),(u1/width,v1/height),(u1/width,v2/height),(u2/width,v2/height)]
        face=list(zip(indices,coords))
        if part['mirror']:face.reverse()
        faces.append([(transformed[i],uv) for i,uv in face])
    return faces

def wrap_uvs(face):
    """Split a quad at atlas-repeat boundaries; interpolate geometry before wrapping."""
    a,b,c,d = face
    def cuts(first,last):
        result={0.0,1.0}
        for low,high in zip(first,last):
            if abs(high-low)<1e-12: continue
            for edge in range(math.floor(min(low,high))+1,math.ceil(max(low,high))):
                fraction=(edge-low)/(high-low)
                if 0<fraction<1:result.add(fraction)
        return sorted(result)
    ts=cuts(a[1],b[1]); ss=cuts(a[1],d[1])
    def interpolate(t,s):
        point=tuple(a[0][i]+t*(b[0][i]-a[0][i])+s*(d[0][i]-a[0][i]) for i in range(3))
        uv=tuple(a[1][i]+t*(b[1][i]-a[1][i])+s*(d[1][i]-a[1][i]) for i in range(2))
        return point,uv
    for t0,t1 in zip(ts,ts[1:]):
        for s0,s1 in zip(ss,ss[1:]):
            tile=tuple(math.floor(v) for v in interpolate((t0+t1)/2,(s0+s1)/2)[1])
            piece=[interpolate(t0,s0),interpolate(t1,s0),interpolate(t1,s1),interpolate(t0,s1)]
            yield [(point,tuple(uv[i]-tile[i] for i in range(2))) for point,uv in piece]

def render_faces(part, operations, orientation):
    for face in mesh(part,0):
        for piece in wrap_uvs(face):
            output=[]
            for point,uv in piece:
                raw=(point[0]-0.5,1.5-point[1],0.5-point[2])
                x,y,z=legacy_animation.transform(raw,operations)
                x,y,z=rotate((x,1-y,-z),(0,math.radians(orientation),0))
                output.append(((x+0.5,y+0.5,z+0.5),uv))
            yield output

def write_obj(path,name,groups,orientation):
    lines=['# Original RotaryCraft geometry; see License.txt.',f'mtllib rotarycraft:models/legacy/{name}.mtl','usemtl original']
    positions={};coordinates={}; quads=0
    for group in groups:
        part=group['part'];lines.append('g '+part['name'])
        for face in render_faces(part,group['operations'],orientation):
            indices=[]
            for vertex,uv in face:
                pos=' '.join(f'{v:.9f}' for v in vertex); tex=' '.join(f'{v:.9f}' for v in uv)
                if pos not in positions:positions[pos]=len(positions)+1;lines.append('v '+pos)
                if tex not in coordinates:coordinates[tex]=len(coordinates)+1;lines.append('vt '+tex)
                indices.append(f'{positions[pos]}/{coordinates[tex]}')
            lines.append('f '+' '.join(indices));quads+=1
    path.write_text('\n'.join(lines)+'\n');return quads

def export():
    target=ASSETS/'models/legacy';textures=ASSETS/'textures/legacy';motion=ASSETS/'motion'
    for path in [target,textures,motion]:path.mkdir(parents=True,exist_ok=True)
    report={}
    animated={'dc_engine','shaft','gearbox_2','gearbox_4','gearbox_8','gearbox_16','grindstone','fan','winder','defoliator','power_generator','steam_generator','wind_generator','hydro_generator','geothermal_generator'}
    for name,(sources,texture,orientation) in ENTRIES.items():
        original=ROOT/'Textures/TileEntityTex'/texture
        shutil.copyfile(original,textures/(name+'.png'))
        modelparts=[part for source in sources for part in parts(source)]
        if name=='winder':modelparts=[p for p in modelparts if not p['name'].split('_')[-1].startswith('Shape6')]
        if name=='geothermal_generator':
            rendered=legacy_animation.performance_groups(modelparts)
        elif name in animated:
            rendered=legacy_animation.groups(ROOT/sources[-1],modelparts)
        else:rendered=[{'part':p,'operations':[]} for p in modelparts]
        moving=[];stationary=[]
        for group in rendered:
            # A symbolic rotation may cancel completely; compare three non-collinear points.
            changed=any(any(abs(a-b)>1e-8 for a,b in zip(legacy_animation.transform(point,group['operations'],0),legacy_animation.transform(point,group['operations'],37))) for point in [(0,0,0),(1,0,0),(0,1,0),(0,0,1)])
            (moving if changed else stationary).append(group)
        blocks=stationary if moving else rendered
        quads=write_obj(target/(name+'.obj'),name,blocks,orientation)
        write_obj(target/(name+'_inventory.obj'),name,rendered,orientation)
        (target/(name+'.mtl')).write_text('newmtl original\nKd 1 1 1\nd 1\nmap_Kd rotarycraft:legacy/'+name+'\n')
        model={'parent':'minecraft:block/block','render_type':'minecraft:cutout','loader':'neoforge:obj','model':f'rotarycraft:models/legacy/{name}.obj','automatic_culling':False,'shade_quads':True,'flip_v':False,'emissive_ambient':False,'textures':{'particle':f'rotarycraft:legacy/{name}'}}
        (ASSETS/'models/block'/(name+'.json')).write_text(json.dumps(model,indent=2)+'\n')
        inventory=dict(model);inventory['model']=f'rotarycraft:models/legacy/{name}_inventory.obj'
        inventory['display']={'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[0.65,0.65,0.65]},'ground':{'scale':[0.25,0.25,0.25],'translation':[0,3,0]},'fixed':{'scale':[0.5,0.5,0.5]},'thirdperson_righthand':{'rotation':[75,45,0],'scale':[0.375,0.375,0.375],'translation':[0,2.5,0]},'firstperson_righthand':{'rotation':[0,45,0],'scale':[0.4,0.4,0.4]}}
        (ASSETS/'models/item'/(name+'.json')).write_text(json.dumps(inventory,indent=2)+'\n')
        for variant in {'power_generator':['power_generator_lit'],'steam_generator':['steam_generator_lit'],'wind_generator':['wind_generator_lit'],'hydro_generator':['hydro_generator_lit'],'geothermal_generator':['geothermal_generator_lit'],'fan':['fan_active'],'defoliator':['defoliator_active'],'sprinkler':['sprinkler_active'],'mob_harvester':['mob_harvester_active']}.get(name,[]):
            (ASSETS/'models/block'/(variant+'.json')).write_text(json.dumps(model,indent=2)+'\n')
        data={'orientation':orientation,'groups':[]}
        combined={}
        for group in moving:
            faces=[]
            for face in mesh(group['part'],0):
                for piece in wrap_uvs(face):
                    faces.append([[point[0]-0.5,1.5-point[1],0.5-point[2],*uv] for point,uv in piece])
            key=json.dumps(group['operations'],sort_keys=True)
            if key not in combined:combined[key]={'operations':group['operations'],'faces':[]}
            combined[key]['faces'].extend(faces)
        data['groups']=list(combined.values())
        if moving:(motion/(name+'.json')).write_text(json.dumps(data,separators=(',',':'))+'\n')
        report[name]={'sources':sources,'texture':str(original.relative_to(ROOT)),'parts':len(rendered),'stationary_quads':quads,'moving_groups':len(moving),'status':'source geometry and renderer operations implemented; validated by required client CI'}
    (ROOT/'docs/legacy-model-import.json').write_text(json.dumps(report,indent=2)+'\n')
    print(f'Imported {len(report)} original model/atlas pairs with animation groups')

if __name__=='__main__':export()
