import importlib.util
from pathlib import Path
import unittest
import math

spec = importlib.util.spec_from_file_location('legacy', Path(__file__).resolve().parents[1] / 'import_legacy_models.py')
legacy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(legacy)

class LegacyModels(unittest.TestCase):
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

if __name__=='__main__' :unittest.main()
