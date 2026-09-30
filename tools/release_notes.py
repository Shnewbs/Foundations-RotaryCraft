"""Extract exact release notes and validate a v<mod_version> tag."""
import argparse
import re
from pathlib import Path


def release_notes(version, changelog):
    pattern = rf"^## {re.escape(version)}(?: - [^\n]*)?\n(.*?)(?=^## |\Z)"
    match = re.search(pattern, changelog, re.MULTILINE | re.DOTALL)
    if not match or not match.group(1).strip():
        raise ValueError(f"No release notes for {version}")
    return match.group(1).strip() + "\n"


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("tag")
    parser.add_argument("--output", default="release-notes.md")
    args = parser.parse_args()
    properties = Path("gradle.properties").read_text()
    version = re.search(r"^mod_version=(.+)$", properties, re.MULTILINE).group(1).strip()
    if args.tag != "v" + version:
        raise SystemExit(f"Tag {args.tag} does not match v{version}")
    Path(args.output).write_text(release_notes(version, Path("CHANGELOG.md").read_text()))
