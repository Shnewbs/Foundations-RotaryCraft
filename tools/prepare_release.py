"""Select an unpublished version after CI checks; preserve published releases."""
import json
import os
import re
import urllib.error
import urllib.request
from pathlib import Path
from release_notes import release_notes


def version_from_properties(text):
    match = re.search(r"^mod_version=(.+)$", text, re.MULTILINE)
    version = match.group(1).strip() if match else ""
    if not re.fullmatch(r"1\.21\.1-\d+\.\d+\.\d+(?:[a-zA-Z0-9.-]*)", version):
        raise ValueError("Invalid release version: " + version)
    return version


def api(path):
    request = urllib.request.Request(
        "https://api.github.com/repos/" + os.environ["GITHUB_REPOSITORY"] + path,
        headers={"Authorization": "Bearer " + os.environ["GH_TOKEN"],
                 "Accept": "application/vnd.github+json"})
    try:
        with urllib.request.urlopen(request) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return None
        raise


def main():
    version = version_from_properties(Path("gradle.properties").read_text())
    tag = "v" + version
    if os.environ["GITHUB_REF"].startswith("refs/tags/") and os.environ["GITHUB_REF"] != "refs/tags/" + tag:
        raise ValueError("Tag does not match the build version")
    existing = api("/releases/tags/" + tag)
    # Published versions are immutable; ordinary commits build without republishing.
    publish = existing is None or existing.get("draft", False)
    if publish:
        ref = api("/git/ref/tags/" + tag)
        if ref:
            obj = ref["object"]
            while obj["type"] == "tag":
                obj = api("/git/tags/" + obj["sha"])["object"]
            if obj["sha"] != os.environ["GITHUB_SHA"]:
                raise ValueError("Existing release tag points to another commit; bump the version")
        Path("release-notes.md").write_text(release_notes(version, Path("CHANGELOG.md").read_text()))
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        output.write(f"version={version}\ntag={tag}\npublish={str(publish).lower()}\n")
        output.write("draft=" + str(bool(existing and existing.get("draft"))).lower() + "\n")
    print("Publish " + tag if publish else "Already published: " + tag)


if __name__ == "__main__":
    main()
