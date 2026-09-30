import importlib.util
from pathlib import Path
import unittest
import math

spec = importlib.util.spec_from_file_location('legacy', Path(__file__).resolve().parents[1] / 'import_legacy_models.py')
legacy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(legacy)

class LegacyModels(unittest.TestCase):
    def test_numeric_expressions_are_restricted(self):
        self.assertEqual(legacy.numbers('0F, -1.047198F*0, 2F*3'), [0,0,6])
        with self.assertRaises(ValueError):legacy.numbers('function()')

    def test_original_dc_geometry(self):
        parts = legacy.parts('Models/Engine/ModelDC.java')
        self.assertEqual(len(parts), 11)
        base = parts[0]
        faces = legacy.mesh(base, 0)
        points = [p for face in faces for p, uv in face]
        self.assertAlmostEqual(min(p[1] for p in points), 0)
        self.assertAlmostEqual(max(p[1] for p in points), 1/16)
        self.assertEqual(base['size'], [128, 128])
        self.assertEqual(len(faces), 6)

    def test_rotations_and_face_winding(self):
        for mirrored in (False, True):
            part = dict(box=[0,0,0,2,4,6],pivot=[-1,20,-3],rotation=[0.2,0.3,0.4],mirror=mirrored,uv=[0,0],size=[128,128])
            faces = legacy.mesh(part, 90)
            points = [p for face in faces for p, uv in face]
            center = tuple(sum(p[i] for p in points)/len(points) for i in range(3))
            for face in faces:
                a,b,c = [p for p,uv in face[:3]]
                ab=[b[i]-a[i] for i in range(3)];ac=[c[i]-a[i] for i in range(3)]
                n=(ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0])
                self.assertGreater(sum(n[i]*(a[i]-center[i]) for i in range(3)),0)
                for point,uv in face:self.assertTrue(all(math.isfinite(v) for v in point+uv))

    def test_repeat_coordinates_stay_inside_atlas(self):
        for part in legacy.parts('Models/Animated/ModelGrinder.java'):
            for original in legacy.mesh(part,0):
                for face in legacy.wrap_uvs(original):
                    for point,uv in face:
                        self.assertTrue(all(-1e-9 <= v <= 1+1e-9 for v in uv))

class AnimationGroups(unittest.TestCase):
    def test_pivot_rotation_and_inverse(self):
        a=legacy.legacy_animation
        ops=[{'translate':[0,1,0]},{'rotate':[1,0,1,0,0]},{'translate':[0,-1,0]}]
        self.assertEqual(a.transform((0,1,0),ops,90),(0,1,0))
        self.assertAlmostEqual(a.transform((0,2,0),ops,90)[2],1)
        self.assertEqual(a.simplify([{'translate':[0,1,0]},{'translate':[0,-1,0]}]),[])

    def test_performance_piston_phases(self):
        a=legacy.legacy_animation
        groups=a.performance_groups(legacy.parts('Models/Engine/ModelPerformance.java'))
        self.assertEqual(len([g for g in groups if g['operations']]),11)
        piston=next(g for g in groups if g['part']['name'].endswith('_Shape4'))
        self.assertAlmostEqual(a.transform((0,0,0),piston['operations'],22.5)[1],0.03125)
        self.assertAlmostEqual(a.transform((0,0,0),piston['operations'],67.5)[1],-0.03125)

    def test_legacy_textures_are_stitched_into_block_atlas(self):
        import json
        atlas=legacy.ROOT/'src/main/resources/assets/minecraft/atlases/blocks.json'
        self.assertIn({'type':'minecraft:directory','source':'legacy','prefix':'legacy/'},json.loads(atlas.read_text())['sources'])

    def test_every_animation_contains_finite_geometry(self):
        import json
        motion=legacy.ASSETS/'motion'
        self.assertEqual(len(list(motion.glob('*.json'))),15)
        for path in motion.glob('*.json'):
            groups=json.loads(path.read_text())['groups']
            self.assertTrue(groups,path.name)
            for group in groups:
                self.assertTrue(group['faces'],path.name)
                for face in group['faces']:
                    self.assertEqual(len(face),4)
                    self.assertTrue(all(math.isfinite(v) for vertex in face for v in vertex))

if __name__=='__main__' :unittest.main()
