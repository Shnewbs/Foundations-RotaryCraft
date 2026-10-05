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
        self.assertEqual("26.3-1.8.0-beta.1", version_from_properties("mod_version=26.3-1.8.0-beta.1\n"))
        for version in ["26.4-1.8.0", "", "1.2.0", "1.21.1-1.2.0/../x", "1.21.1-1.2.0;exec"]:
            with self.assertRaises(ValueError):
                version_from_properties("mod_version=" + version)

class PublicationSelectionTest(unittest.TestCase):
    def test_superseded_master_defers_publication(self):
        import os
        import tempfile
        from unittest.mock import patch
        from prepare_release import main
        previous = Path.cwd()
        with tempfile.TemporaryDirectory() as directory:
            try:
                os.chdir(directory)
                Path("gradle.properties").write_text("mod_version=1.21.1-1.8.0\n")
                def response(path):
                    return {"object": {"sha": "new-head"}} if path == "/git/ref/heads/master" else None
                with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/master", "GITHUB_SHA": "old-head", "GITHUB_OUTPUT": "output", "RELEASE_PROPERTIES": "gradle.properties"}), patch("prepare_release.api", side_effect=response):
                    main()
                self.assertIn("publish=false", Path("output").read_text())
                self.assertFalse(Path("release-notes.md").exists())
            finally:
                os.chdir(previous)

    def test_native_target_selects_its_own_version_and_notes(self):
        import os
        import tempfile
        from unittest.mock import patch
        from prepare_release import main
        previous = Path.cwd()
        with tempfile.TemporaryDirectory() as directory:
            try:
                os.chdir(directory)
                Path("gradle.properties").write_text("mod_version=1.21.1-1.8.0\n")
                Path("ports/26.3").mkdir(parents=True)
                Path("ports/26.3/gradle.properties").write_text("mod_version=26.3-1.8.0-beta.1\n")
                Path("CHANGELOG.md").write_text("## 1.21.1-1.8.0\n- Older target\n\n## 26.3-1.8.0-beta.1\n- Native target\n")
                def response(path):
                    return {"object": {"sha": "head"}} if path == "/git/ref/heads/master" else None
                with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/master", "GITHUB_SHA": "head", "GITHUB_OUTPUT": "output", "RELEASE_PROPERTIES": "ports/26.3/gradle.properties"}), patch("prepare_release.api", side_effect=response):
                    main()
                self.assertIn("version=26.3-1.8.0-beta.1", Path("output").read_text())
                self.assertIn("publish=true", Path("output").read_text())
                self.assertEqual("- Native target\n", Path("release-notes.md").read_text())
            finally:
                os.chdir(previous)
