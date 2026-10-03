#!/usr/bin/env python3
"""Static resource cross-checker for CustomBoard.

Scans every Kotlin and XML source file for R.<type>.<name> / @<type>/<name>
references and reports the ones that are not defined anywhere under res/.
Run from the repository root:  python3 tools/check_resources.py
"""
from __future__ import annotations

import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict

RES = os.path.join("app", "src", "main", "res")
SRC = os.path.join("app", "src", "main", "java")

VALUE_TAGS = {
    "string": "string",
    "string-array": "array",
    "integer-array": "array",
    "array": "array",
    "plurals": "plurals",
    "color": "color",
    "dimen": "dimen",
    "bool": "bool",
    "integer": "integer",
    "style": "style",
    "declare-styleable": "styleable",
    "item": None,  # resolved from the type attribute
}

defined: dict[str, set[str]] = defaultdict(set)
implicit_style_parents: list[tuple[str, str]] = []


def collect_values() -> None:
    for root, _dirs, files in os.walk(RES):
        folder = os.path.basename(root)
        if not folder.startswith("values"):
            continue
        for name in files:
            if not name.endswith(".xml"):
                continue
            tree = ET.parse(os.path.join(root, name))
            for child in tree.getroot():
                if not isinstance(child.tag, str):
                    continue
                res_name = child.get("name")
                if not res_name:
                    continue
                kind = VALUE_TAGS.get(child.tag, child.tag)
                if child.tag == "item":
                    kind = child.get("type") or "item"
                if kind:
                    defined[kind].add(res_name.replace(".", "_") if kind == "style" else res_name)
                    if child.tag == "style" and "." in res_name and not child.get("parent"):
                        # Android infers a parent from dotted style names when `parent` is omitted.
                        implicit_style_parents.append(
                            (os.path.join(root, name), res_name.rsplit(".", 1)[0])
                        )
                if child.tag == "declare-styleable":
                    for attr in child:
                        attr_name = attr.get("name")
                        if attr_name and not attr_name.startswith("android:"):
                            defined["attr"].add(attr_name)


def collect_files() -> None:
    for root, _dirs, files in os.walk(RES):
        folder = os.path.basename(root)
        kind = folder.split("-")[0]
        if kind.startswith("values"):
            continue
        for name in files:
            base = name.rsplit(".", 1)[0]
            defined[kind].add(base)


def collect_ids() -> None:
    pattern = re.compile(r'@\+id/([A-Za-z0-9_]+)')
    for root, _dirs, files in os.walk(RES):
        for name in files:
            if not name.endswith(".xml"):
                continue
            with open(os.path.join(root, name), encoding="utf-8") as handle:
                for match in pattern.finditer(handle.read()):
                    defined["id"].add(match.group(1))


R_PATTERN = re.compile(r'\bR\.(string|array|plurals|drawable|mipmap|color|dimen|layout|id|xml|raw|anim|style|bool|integer|font|styleable)\.([A-Za-z0-9_]+)')
XML_PATTERN = re.compile(r'@(?:android:)?(string|array|plurals|drawable|mipmap|color|dimen|layout|id|xml|raw|anim|style|bool|integer|font)/([A-Za-z0-9_.]+)')
APP_ATTR_PATTERN = re.compile(r'\bapp:([A-Za-z0-9_]+)')
ANDROID_PREFIX = re.compile(r'@android:')

# Names provided by AndroidX libraries rather than this module.
LIBRARY_PROVIDED = {
    "style": {"PreferenceThemeOverlay", "Widget_AppCompat_ActionBar"},
}


def check() -> int:
    problems: list[str] = []

    for path, parent in implicit_style_parents:
        key = parent.replace(".", "_")
        if key not in defined.get("style", set()) and parent not in LIBRARY_PROVIDED.get("style", set()):
            problems.append(f"{path}: implicit parent style `{parent}`")

    for root, _dirs, files in os.walk(SRC):
        for name in files:
            if not name.endswith(".kt"):
                continue
            path = os.path.join(root, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            for match in R_PATTERN.finditer(text):
                kind, res = match.group(1), match.group(2)
                start = match.start()
                prefix = text[max(0, start - 30):start]
                if prefix.endswith("android.") or prefix.endswith("androidx.preference."):
                    continue
                if kind == "styleable":
                    continue
                if res not in defined.get(kind, set()):
                    problems.append(f"{path}: R.{kind}.{res}")

    for root, _dirs, files in os.walk(RES):
        for name in files:
            if not name.endswith(".xml"):
                continue
            path = os.path.join(root, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            for match in XML_PATTERN.finditer(text):
                if ANDROID_PREFIX.match(match.group(0)):
                    continue
                kind, res = match.group(1), match.group(2)
                if "/" in res or res.startswith("android:"):
                    continue
                key = res.replace(".", "_") if kind == "style" else res
                if res in LIBRARY_PROVIDED.get(kind, set()):
                    continue
                if key not in defined.get(kind, set()):
                    problems.append(f"{path}: @{kind}/{res}")
            for match in APP_ATTR_PATTERN.finditer(text):
                attr = match.group(1)
                if attr not in defined.get("attr", set()):
                    problems.append(f"{path}: app:{attr}")

    manifest = os.path.join("app", "src", "main", "AndroidManifest.xml")
    with open(manifest, encoding="utf-8") as handle:
        text = handle.read()
    for match in XML_PATTERN.finditer(text):
        if ANDROID_PREFIX.match(match.group(0)):
            continue
        kind, res = match.group(1), match.group(2)
        key = res.replace(".", "_") if kind == "style" else res
        if key not in defined.get(kind, set()):
            problems.append(f"{manifest}: @{kind}/{res}")

    unique = sorted(set(problems))
    for problem in unique:
        print(problem)
    print(f"\n{len(unique)} unresolved reference(s)")
    return 1 if unique else 0


if __name__ == "__main__":
    collect_values()
    collect_files()
    collect_ids()
    sys.exit(check())
