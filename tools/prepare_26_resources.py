"""Generate native 26.3 resources from the authoritative shared assets."""
import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "ports/26.3/build/generated/resources"
IDS = {
    "dc_engine", "shaft", "gearbox_2", "gearbox_4", "gearbox_8", "gearbox_16",
    "power_cable", "power_cell", "power_generator", "solar_generator",
    "wind_generator", "hydro_generator", "steam_generator", "geothermal_generator",
    "grindstone", "power_switch",
}


def modern(value):
    """26.x ingredient codecs encode item identifiers and #tag strings directly."""
    if isinstance(value, list):
        return [modern(item) for item in value]
    if not isinstance(value, dict):
        return value
    if set(value) == {"item"}:
        return value["item"]
    if set(value) == {"tag"}:
        return "#" + value["tag"]
    return {key: modern(item) for key, item in value.items()}


def references(value):
    return set(re.findall(r"rotarycraft:([a-z0-9_]+)", json.dumps(value)))


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    shutil.copytree(ROOT / "src/main/resources/assets", OUT / "assets")
    assets = OUT / "assets/rotarycraft"
    for path in (assets / "blockstates").glob("*.json"):
        if path.stem not in IDS:
            path.unlink()
    for name in sorted(IDS):
        write_json(assets / "items" / f"{name}.json", {
            "model": {"type": "minecraft:model", "model": f"rotarycraft:item/{name}"},
        })
        # Complete parked geometry while the native animation backend is pending.
        model = json.loads((assets / "models/item" / f"{name}.json").read_text())
        if model.get("loader") == "neoforge:obj":
            model.pop("display", None)
            for suffix in ("", "_lit"):
                target = assets / "models/block" / f"{name}{suffix}.json"
                if target.exists():
                    write_json(target, model)
    source = ROOT / "src/main/resources/data"
    for path in source.rglob("*.json"):
        value = modern(json.loads(path.read_text()))
        relative = path.relative_to(source)
        if "tags" in relative.parts:
            value["values"] = [item for item in value.get("values", [])
                               if not references(item) - IDS]
        if "advancement" in relative.parts:
            for criterion in value.get("criteria", {}).values():
                conditions = criterion.get("conditions", {})
                if criterion.get("trigger") == "minecraft:recipe_unlocked" and "recipe" in conditions:
                    conditions["recipes"] = [conditions.pop("recipe")]
        if references(value) - IDS - {"grinding"}:
            continue
        write_json(OUT / "data" / relative, value)
    shutil.copytree(source / "rotarycraft/structure", OUT / "data/rotarycraft/structure")
    # One mod pack contains resource format 97.1 and data format 121.0.
    write_json(OUT / "pack.mcmeta", {"pack": {
        "description": "Foundations RotaryCraft 26.3",
        "min_format": [97, 1], "max_format": [121, 0],
    }})
    print("26.3 resources prepared for", len(IDS), "registered blocks")


if __name__ == "__main__":
    main()
