#!/usr/bin/env python3
"""Lightweight static sanity checks for the Kotlin sources.

1. every `import com.customboard.keyboard.*` resolves to a declared top-level symbol
2. every `binding.<field>` matches an id declared in the bound layout
3. no TODO/FIXME/stub markers slipped in

Run from the repository root:  python3 tools/check_kotlin_refs.py
"""
from __future__ import annotations

import os
import re
import sys
from collections import defaultdict

SRC = os.path.join("app", "src", "main", "java")
RES = os.path.join("app", "src", "main", "res")
PKG = "com.customboard.keyboard"

declared: dict[str, set[str]] = defaultdict(set)
# Matches top-level declarations, including extension functions and properties
# (`fun Context.dpToPx(...)`), whose receiver type must be skipped.
TOP_LEVEL = re.compile(
    r'^(?:@\w+\s+)?(?:public |internal |private |open |abstract |sealed |data |enum |annotation |value )*'
    r'(?:class|interface|object|typealias)\s+([A-Za-z_][A-Za-z0-9_]*)'
    r'|^(?:public |internal |private |inline |suspend |const )*(?:fun|val|var)\s+'
    r'(?:<[^>\n]+>\s*)?(?:[\w.<>?]+\.)?([A-Za-z_][A-Za-z0-9_]*)',
    re.MULTILINE,
)


def collect() -> None:
    for root, _dirs, files in os.walk(SRC):
        for name in files:
            if not name.endswith(".kt"):
                continue
            path = os.path.join(root, name)
            text = open(path, encoding="utf-8").read()
            package = re.search(r'^package\s+([\w.]+)', text, re.MULTILINE)
            if not package:
                continue
            for match in TOP_LEVEL.finditer(text):
                declared[package.group(1)].add(match.group(1) or match.group(2))


def layout_ids(layout: str) -> set[str]:
    ids: set[str] = set()
    for folder in os.listdir(RES):
        if not folder.startswith("layout"):
            continue
        path = os.path.join(RES, folder, layout + ".xml")
        if os.path.exists(path):
            text = open(path, encoding="utf-8").read()
            ids.update(re.findall(r'@\+id/([A-Za-z0-9_]+)', text))
    return ids


def camel(name: str) -> str:
    head, *tail = name.split("_")
    return head + "".join(part.capitalize() for part in tail)


def check() -> int:
    problems: list[str] = []
    for root, _dirs, files in os.walk(SRC):
        for name in files:
            if not name.endswith(".kt"):
                continue
            path = os.path.join(root, name)
            text = open(path, encoding="utf-8").read()

            for match in re.finditer(r'^import\s+(' + PKG.replace(".", r"\.") + r'[\w.]*)', text, re.MULTILINE):
                full = match.group(1)
                parts = full.split(".")
                for cut in range(len(parts) - 1, 3, -1):
                    package, symbol = ".".join(parts[:cut]), parts[cut]
                    if symbol in declared.get(package, set()):
                        break
                else:
                    if not full.endswith(".BuildConfig") and ".databinding." not in full and ".R" != full[-2:]:
                        problems.append(f"{path}: unresolved import {full}")

            binding = re.search(r'(\w+)Binding\.inflate', text)
            if binding:
                layout = re.sub(r'(?<!^)(?=[A-Z])', '_', binding.group(1)).lower()
                ids = {camel(i) for i in layout_ids(layout)}
                if ids:
                    for match in re.finditer(r'\bbinding\.([a-z][A-Za-z0-9]*)', text):
                        field = match.group(1)
                        if field in {"root", "isInitialized"} or field in ids:
                            continue
                        problems.append(f"{path}: binding.{field} not in {layout}.xml")

            for marker in ("TODO(", "FIXME", "not implemented", "XXX:"):
                if marker in text:
                    problems.append(f"{path}: contains marker {marker!r}")

    unique = sorted(set(problems))
    for problem in unique:
        print(problem)
    print(f"\n{len(unique)} problem(s)")
    return 1 if unique else 0


if __name__ == "__main__":
    collect()
    sys.exit(check())
