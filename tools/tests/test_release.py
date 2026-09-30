import sys
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prepare_release import version_from_properties
from release_notes import release_notes


class ReleaseTest(unittest.TestCase):
    def test_exact_version_notes(self):
        text = "## Unreleased\n- Later\n\n## 1.21.1-1.2.0 - Recipes\n- Current\n\n## 1.21.1-1.1.0\n- Old\n"
        self.assertEqual("- Current\n", release_notes("1.21.1-1.2.0", text))

    def test_missing_or_empty_notes_fail(self):
        for text in ["## Unreleased\n- Later\n", "## 1.21.1-1.2.0\n\n"]:
            with self.assertRaises(ValueError):
                release_notes("1.21.1-1.2.0", text)

    def test_filename_version_validation(self):
        self.assertEqual("1.21.1-1.2.0", version_from_properties("mod_version=1.21.1-1.2.0\n"))
        for version in ["", "1.2.0", "1.21.1-1.2.0/../x", "1.21.1-1.2.0;exec"]:
            with self.assertRaises(ValueError):
                version_from_properties("mod_version=" + version)
