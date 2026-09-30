#!/usr/bin/env python3
"""Export original Techne model boxes and atlas UVs to NeoForge OBJ assets.

Keeps original texture PNG bytes. Exports constructor/rest-pose geometry only;
renderer animation, conditional parts and client validation remain separate work.
"""
import json
import math
import re
import shutil
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
    'mob_harvester': (['Models/ModelHarvester.java'], 'harvestertex.png', 0),
}
NUMBER = r'[-+]?\d+(?:\.\d+)?[FfDd]?'

def numbers(value):
    return [float(v.rstrip('FfDd')) for v in value.split(',')]

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

def export():
    target = ASSETS / 'models/legacy'
    textures = ASSETS / 'textures/legacy'
    target.mkdir(parents=True, exist_ok=True)
    textures.mkdir(parents=True, exist_ok=True)
    report={}
    for name,(sources,texture,orientation) in ENTRIES.items():
        original=ROOT/'Textures/TileEntityTex'/texture
        shutil.copyfile(original,textures/(name+'.png'))
        modelparts=[part for source in sources for part in parts(source)]
        lines=['# Derived from original RotaryCraft model source; see License.txt.',f'mtllib rotarycraft:models/legacy/{name}.mtl','usemtl original']
        positions={}
        coordinates={}
        for part in modelparts:
            lines.append('g '+part['name'])
            for face in mesh(part,orientation):
                indices=[]
                for vertex,uv in face:
                    position=' '.join(f'{v:.9f}' for v in vertex)
                    coordinate=' '.join(f'{v:.9f}' for v in uv)
                    if position not in positions:
                        positions[position]=len(positions)+1
                        lines.append('v '+position)
                    if coordinate not in coordinates:
                        coordinates[coordinate]=len(coordinates)+1
                        lines.append('vt '+coordinate)
                    indices.append(f'{positions[position]}/{coordinates[coordinate]}')
                lines.append('f '+' '.join(indices))
        (target/(name+'.obj')).write_text('\n'.join(lines)+'\n')
        (target/(name+'.mtl')).write_text('newmtl original\nKd 1 1 1\nd 1\nmap_Kd rotarycraft:legacy/'+name+'\n')
        model={'parent':'minecraft:block/block','loader':'neoforge:obj','model':f'rotarycraft:models/legacy/{name}.obj',
               'automatic_culling':False,'shade_quads':True,'flip_v':False,'emissive_ambient':False,
               'textures':{'particle':f'rotarycraft:legacy/{name}'}}
        (ASSETS/'models/block'/(name+'.json')).write_text(json.dumps(model,indent=2)+'\n')
        report[name]={'sources':sources,'texture':str(original.relative_to(ROOT)),'parts':len(modelparts),
                      'quads':len(modelparts)*6,'status':'rest-pose import; animation and in-client acceptance pending'}
    (ROOT/'docs/legacy-model-import.json').write_text(json.dumps(report,indent=2)+'\n')
    print(f'Imported {len(report)} original model/atlas pairs')

if __name__=='__main__':export()
