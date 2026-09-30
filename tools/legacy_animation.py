"""Translate the legacy renderer's affine operations into cached render groups."""
import re
import math
import copy

def expression(value,variables,phi=0):
    value=re.sub(r'(?<=\d)[FfDd]\b','',value.strip())
    total=1.0
    for token in value.split('*'):
        token=token.strip()
        if token in variables:total*=variables[token]
        elif token.startswith('-') and token[1:] in variables:total*=-variables[token[1:]]
        elif token=='phi':total*=phi
        elif token=='-phi':total*=-phi
        elif re.fullmatch(r'[-+]?\d+(?:\.\d+)?',token):total*=float(token)
        else:raise ValueError('Unsupported render expression '+value)
    return total

def simplify(operations):
    stack=[]
    for original in operations:
        op={key:list(value) for key,value in original.items()}
        if stack and 'translate' in op and 'translate' in stack[-1]:
            op={'translate':[a+b for a,b in zip(stack.pop()['translate'],op['translate'])]}
        elif stack and 'rotate' in op and 'rotate' in stack[-1] and op['rotate'][2:]==stack[-1]['rotate'][2:]:
            old=stack.pop()['rotate'];now=op['rotate'];op={'rotate':[old[0]+now[0],old[1]+now[1],*now[2:]]}
        values=op.get('translate',op.get('rotate',[])[:2])
        if any(abs(v)>1e-9 for v in values):stack.append(op)
    return stack

def groups(path,parts):
    source=path.read_text()
    body=source[source.index('public void renderAll'):]
    body=body[:body.index('\n\t@Override')] if '\n\t@Override' in body else body
    # Defoliator's explicit radial repetition.
    body=re.sub(r'for \(int i = 0; i < 3; i\+\+\) \{([\s\S]*?)\n\t+\}',lambda m:'\n'.join(re.sub(r'120\*i',str(120*i),m[1]) for i in range(3)),body)
    body=re.sub(r'for \(int i = 0; i < 4; i\+\+\) \{([\s\S]*?)\n\t+\}',lambda m:'\n'.join(re.sub(r'-45-90\*i',str(-45-90*i),re.sub(r'45\+90\*i',str(45+90*i),m[1])) for i in range(4)),body)
    byname={p['name'].split('_')[-1]:p for p in parts}
    ops=[]; variables={}; result=[]; rendered=set()
    for line in body.splitlines():
        line=line.strip()
        assignment=re.fullmatch(r'(?:double )?(\w+) = ([^;]+);',line)
        if assignment:
            try:variables[assignment[1]]=expression(assignment[2],variables)
            except ValueError:pass
        rotation=re.fullmatch(r'(\w+)\.rotateAngle([XYZ]) = ([^;]+);',line)
        if rotation and rotation[1] in byname:
            byname[rotation[1]]['rotation']['XYZ'.index(rotation[2])]=expression(rotation[3],variables)
        pivot=re.fullmatch(r'(\w+)\.setRotationPoint\(([^)]+)\);',line)
        if pivot and pivot[1] in byname:byname[pivot[1]]['pivot']=[expression(v,variables) for v in pivot[2].split(',')]
        translate=re.search(r'GL11.glTranslated\(([^)]+)\)',line)
        if translate:ops.append({'translate':[expression(v,variables) for v in translate[1].split(',')]})
        rotate=re.search(r'GL11.glRotatef\(([^)]+)\)',line)
        if rotate:
            args=rotate[1].split(','); angle=args[0]
            ops.append({'rotate':[expression(angle,variables,1) if 'phi' in angle else 0,
                                  expression(angle,variables,0),*[expression(v,variables) for v in args[1:]]]})
        match=re.fullmatch(r'(\w+)\.render\(te, f5\);',line)
        if match and match[1] in byname:
            name=match[1]
            result.append({'part':copy.deepcopy(byname[name]),'operations':simplify(ops)})
            rendered.add(name)
    # Base gearbox mount and support parts are rendered by inherited helper methods.
    if path.name.startswith('ModelGearbox'):
        for name,p in byname.items():
            if name not in rendered:result.append({'part':p,'operations':[]})
    return result

def transform(point,operations,phi=0):
    # OpenGL post-multiplication applies the last operation first to a vertex.
    x,y,z=point
    for op in reversed(operations):
        if 'translate' in op:
            a,b,c=op['translate'];x+=a;y+=b;z+=c
        elif 'rotate' in op:
            coefficient,offset,a,b,c=op['rotate']; angle=math.radians(coefficient*phi+offset)
            co,si=math.cos(angle),math.sin(angle)
            if a:x,y,z=x,y*co-z*si,y*si+z*co
            elif b:x,y,z=x*co+z*si,y,-x*si+z*co
            else:x,y,z=x*co-y*si,x*si+y*co,z
    return x,y,z
