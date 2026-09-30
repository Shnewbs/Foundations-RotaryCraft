#!/usr/bin/env python3
"""Validate RotaryCraft model, blockstate, texture, and animation resources."""

import json
import struct
import sys
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "rotarycraft"
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


class AssetError(Exception):
    pass


def read_json(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise AssetError(f"{relative(path)}: invalid JSON ({error})") from error


def relative(path):
    return path.relative_to(ROOT).as_posix()


def resource_parts(resource, default_namespace="minecraft"):
    if ":" in resource:
        return resource.split(":", 1)
    return default_namespace, resource


def model_path(resource, default_namespace="minecraft"):
    namespace, path = resource_parts(resource, default_namespace)
    return ROOT / "src" / "main" / "resources" / "assets" / namespace / "models" / (path + ".json")


def texture_path(resource, default_namespace="rotarycraft"):
    namespace, path = resource_parts(resource, default_namespace)
    return ROOT / "src" / "main" / "resources" / "assets" / namespace / "textures" / (path + ".png")


def png_dimensions(path):
    try:
        content = path.read_bytes()
    except OSError as error:
        raise AssetError(f"{relative(path)}: cannot read PNG ({error})") from error
    if not content.startswith(PNG_SIGNATURE):
        raise AssetError(f"{relative(path)}: invalid PNG signature")

    offset = len(PNG_SIGNATURE)
    compressed = bytearray()
    dimensions = None
    saw_iend = False
    while offset < len(content):
        if offset + 12 > len(content):
            raise AssetError(f"{relative(path)}: truncated PNG chunk")
        length = struct.unpack(">I", content[offset : offset + 4])[0]
        chunk_type = content[offset + 4 : offset + 8]
        chunk_end = offset + 12 + length
        if chunk_end > len(content):
            raise AssetError(f"{relative(path)}: truncated PNG chunk data")
        data = content[offset + 8 : offset + 8 + length]
        expected_crc = struct.unpack(">I", content[offset + 8 + length : chunk_end])[0]
        actual_crc = zlib.crc32(chunk_type + data) & 0xFFFFFFFF
        if actual_crc != expected_crc:
            raise AssetError(f"{relative(path)}: invalid {chunk_type.decode('ascii', 'replace')} CRC")
        if chunk_type == b"IHDR":
            if length != 13 or dimensions is not None:
                raise AssetError(f"{relative(path)}: invalid PNG header")
            width, height = struct.unpack(">II", data[:8])
            if width == 0 or height == 0:
                raise AssetError(f"{relative(path)}: zero-sized PNG")
            dimensions = (width, height)
        elif chunk_type == b"IDAT":
            compressed.extend(data)
        elif chunk_type == b"IEND":
            saw_iend = True
            if length != 0:
                raise AssetError(f"{relative(path)}: invalid PNG end chunk")
            offset = chunk_end
            break
        offset = chunk_end

    if dimensions is None or not saw_iend or offset != len(content):
        raise AssetError(f"{relative(path)}: incomplete PNG")
    try:
        zlib.decompress(compressed)
    except zlib.error as error:
        raise AssetError(f"{relative(path)}: invalid compressed image data ({error})") from error
    return dimensions


def validate_animation(path, dimensions):
    metadata_path = path.with_name(path.name + ".mcmeta")
    if not metadata_path.exists():
        return False
    metadata = read_json(metadata_path)
    animation = metadata.get("animation")
    if not isinstance(animation, dict):
        raise AssetError(f"{relative(metadata_path)}: missing animation object")

    width, height = dimensions
    frame_width = animation.get("width", width)
    frame_height = animation.get("height", frame_width)
    if not isinstance(frame_width, int) or not isinstance(frame_height, int):
        raise AssetError(f"{relative(metadata_path)}: frame dimensions must be integers")
    if frame_width <= 0 or frame_height <= 0 or width % frame_width or height % frame_height:
        raise AssetError(f"{relative(metadata_path)}: frame dimensions do not divide the {width}x{height} image")

    frame_count = (width // frame_width) * (height // frame_height)
    default_time = animation.get("frametime", 1)
    if not isinstance(default_time, int) or default_time <= 0:
        raise AssetError(f"{relative(metadata_path)}: frametime must be a positive integer")
    frames = animation.get("frames")
    if frames is not None:
        if not isinstance(frames, list) or not frames:
            raise AssetError(f"{relative(metadata_path)}: frames must be a non-empty array")
        for frame in frames:
            index = frame.get("index") if isinstance(frame, dict) else frame
            duration = frame.get("time", default_time) if isinstance(frame, dict) else default_time
            if not isinstance(index, int) or index < 0 or index >= frame_count:
                raise AssetError(f"{relative(metadata_path)}: frame index {index!r} is outside 0..{frame_count - 1}")
            if not isinstance(duration, int) or duration <= 0:
                raise AssetError(f"{relative(metadata_path)}: frame duration must be a positive integer")
    return True


def validate():
    errors = []
    json_files = sorted(ASSETS.rglob("*.json"))
    model_files = sorted((ASSETS / "models").rglob("*.json"))
    blockstate_files = sorted((ASSETS / "blockstates").glob("*.json"))
    model_ids = {path.relative_to(ASSETS / "models").with_suffix("").as_posix() for path in model_files}
    png_files = sorted((ASSETS / "textures").rglob("*.png"))
    png_sizes = {}

    for path in json_files:
        try:
            read_json(path)
        except AssetError as error:
            errors.append(str(error))

    for path in png_files:
        try:
            png_sizes[path] = png_dimensions(path)
        except AssetError as error:
            errors.append(str(error))

    def check_local_model_reference(resource, source, kind, default_namespace="minecraft"):
        namespace, path = resource_parts(resource, default_namespace)
        if namespace == "rotarycraft":
            target = model_path(resource, default_namespace)
            if not target.is_file():
                errors.append(f"{relative(source)}: missing {kind} model {resource}")

    def check_texture(resource, source, default_namespace="rotarycraft"):
        namespace, path = resource_parts(resource, default_namespace)
        if namespace != "rotarycraft":
            return
        target = texture_path(resource, default_namespace)
        if not target.is_file():
            errors.append(f"{relative(source)}: missing texture {resource}")
        elif target not in png_sizes:
            try:
                png_sizes[target] = png_dimensions(target)
            except AssetError as error:
                errors.append(str(error))

    def visit_blockstate(value, source):
        if isinstance(value, dict):
            model = value.get("model")
            if isinstance(model, str):
                check_local_model_reference(model, source, "blockstate")
            for child in value.values():
                visit_blockstate(child, source)
        elif isinstance(value, list):
            for child in value:
                visit_blockstate(child, source)

    for path in blockstate_files:
        try:
            visit_blockstate(read_json(path), path)
        except AssetError as error:
            errors.append(str(error))

    model_data = {}
    for path in model_files:
        try:
            model_data[path] = read_json(path)
        except AssetError as error:
            errors.append(str(error))

    for path, data in model_data.items():
        parent = data.get("parent")
        if isinstance(parent, str):
            check_local_model_reference(parent, path, "parent")

    # Validate external OBJ geometry and material/texture references.
    for path, data in model_data.items():
        if data.get("loader") != "neoforge:obj":
            continue
        namespace, resource = resource_parts(data.get("model", ""))
        obj = ROOT / "src/main/resources/assets" / namespace / resource
        if namespace != "rotarycraft" or not obj.is_file():
            errors.append(f"{relative(path)}: missing OBJ geometry {obj}")
            continue
        for line in obj.read_text().splitlines():
            if line.startswith("mtllib "):
                ns, material = resource_parts(line.split(maxsplit=1)[1])
                mtl = ROOT / "src/main/resources/assets" / ns / material
                if not mtl.is_file():
                    errors.append(f"{relative(path)}: missing OBJ material {mtl}")
                    continue
                for entry in mtl.read_text().splitlines():
                    if entry.startswith("map_Kd "):
                        tex = texture_path(entry.split(maxsplit=1)[1])
                        if not tex.is_file(): errors.append(f"{relative(path)}: missing OBJ texture {tex}")

    # Follow local model parent chains to catch missing links and cycles.
    visiting = set()
    visited = set()

    def visit_model(path):
        if path in visiting:
            errors.append(f"{relative(path)}: cyclic model parent chain")
            return
        if path in visited or path not in model_data:
            return
        visiting.add(path)
        parent = model_data[path].get("parent")
        if isinstance(parent, str):
            namespace, model_id = resource_parts(parent)
            if namespace == "rotarycraft":
                visit_model(model_path(parent))
        visiting.remove(path)
        visited.add(path)

    for path in model_data:
        visit_model(path)

    def inherited_textures(path, chain=None):
        if chain is None:
            chain = set()
        if path in chain or path not in model_data:
            return {}
        chain = chain | {path}
        data = model_data[path]
        textures = {}
        parent = data.get("parent")
        if isinstance(parent, str):
            namespace, _ = resource_parts(parent)
            if namespace == "rotarycraft":
                textures.update(inherited_textures(model_path(parent), chain))
        local = data.get("textures", {})
        if isinstance(local, dict):
            textures.update(local)
        return textures

    def resolve_texture(value, textures, source, resolving=None):
        if not isinstance(value, str):
            return
        if not value.startswith("#"):
            check_texture(value, source)
            return
        key = value[1:]
        if resolving is None:
            resolving = set()
        if key in resolving:
            errors.append(f"{relative(source)}: cyclic texture alias {value}")
            return
        target = textures.get(key)
        if target is None:
            errors.append(f"{relative(source)}: unresolved texture alias {value}")
            return
        resolve_texture(target, textures, source, resolving | {key})

    def find_face_textures(value, found):
        if isinstance(value, dict):
            for key, child in value.items():
                if key == "texture" and isinstance(child, str):
                    found.append(child)
                else:
                    find_face_textures(child, found)
        elif isinstance(value, list):
            for child in value:
                find_face_textures(child, found)

    for path, data in model_data.items():
        textures = inherited_textures(path)
        for value in textures.values():
            resolve_texture(value, textures, path)
        face_textures = []
        find_face_textures(data.get("elements", []), face_textures)
        for value in face_textures:
            resolve_texture(value, textures, path)

    for path in sorted((ASSETS / "textures").rglob("*.png")):
        if path not in png_sizes:
            continue
        try:
            validate_animation(path, png_sizes[path])
        except AssetError as error:
            errors.append(str(error))

    for path in sorted((ASSETS / "motion").glob("*.json")):
        try:
            data=read_json(path)
            if not data.get("groups"):
                errors.append(f"{relative(path)}: empty animation mesh")
            for group in data.get("groups",[]):
                for face in group.get("faces",[]):
                    if len(face)!=4 or any(len(vertex)!=5 for vertex in face):
                        errors.append(f"{relative(path)}: malformed animated quad")
        except AssetError as error:
            errors.append(str(error))

    if errors:
        for error in sorted(set(errors)):
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    texture_refs = set()
    for data in model_data.values():
        textures = data.get("textures", {})
        if isinstance(textures, dict):
            texture_refs.update(
                value for value in textures.values()
                if isinstance(value, str) and not value.startswith("#")
                and resource_parts(value)[0] == "rotarycraft"
            )
    animated = sum(
        1 for path in png_files if path.with_name(path.name + ".mcmeta").is_file()
    )
    print(
        f"Asset validation passed: {len(json_files)} JSON files, "
        f"{len(blockstate_files)} blockstates, {len(model_files)} models, "
        f"{len(texture_refs)} local texture references, {len(png_files)} PNGs, "
        f"{animated} animated texture(s), {len(list((ASSETS / 'motion').glob('*.json')))} motion meshes."
    )
    return 0


if __name__ == "__main__":
    sys.exit(validate())
