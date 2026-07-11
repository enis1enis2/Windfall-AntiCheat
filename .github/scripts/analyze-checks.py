#!/usr/bin/env python3
"""
Windfall Anti-Cheat Monitor — Analysis Engine
Scans competitor repos for check implementations, diffs against Windfall,
and optionally generates skeleton check files for missing detections.
"""

import os
import re
import json
import hashlib
from pathlib import Path
from datetime import datetime, timezone
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Set, Tuple

WINDFALL_ROOT = Path(__file__).resolve().parents[2]
CACHE_DIR = WINDFALL_ROOT / ".cache" / "anticheats"
REPORT_PATH = WINDFALL_ROOT / ".github" / "anticheat-report.md"
DIFF_PATH = WINDFALL_ROOT / ".github" / "anticheat-diff.json"
WINDFALL_CHECKS_DIR = WINDFALL_ROOT / "src" / "main" / "java" / "io" / "windfall" / "anticheat" / "core" / "check" / "impl"

ANTI_CHEATS = {
    "Grim": {
        "repo": "https://github.com/GrimAnticheat/Grim.git",
        "branch": "2.0",
        "check_paths": [
            "common/src/main/java/ac/grim/grimac/checks/impl",
        ],
        "language": "java",
    },
    "TruthfulAC": {
        "repo": "https://github.com/TawnyE/TruthfulAC.git",
        "branch": "main",
        "check_paths": [
            "src/main/java/ret/tawny/truthful/checks/impl",
        ],
        "language": "java",
    },
    "CloudAC": {
        "repo": "https://github.com/0xntpower/CloudAC.git",
        "branch": "main",
        "check_paths": [
            "ComputationServer/sys",
        ],
        "language": "go",
    },
    "Arrow": {
        "repo": "https://github.com/StelGR/ArrowAntiCheat.git",
        "branch": "master",
        "check_paths": [
            "src/main/java/me/arrow/checks/impl",
        ],
        "language": "java",
    },
}

# Maps common check name patterns to Windfall categories
CHECK_CATEGORIES = {
    "movement": ["speed", "fly", "flight", "velocity", "timer", "nofall", "step", "scaffold",
                 "elytra", "jesus", "bhop", "strafe", "ground", "climb", "water", "sink",
                 "fastplace", "phase", "nostream", "spin", "glide", "airwalk",
                 "freecam", "keepfly", "maxheight", "longjump", "motion", "fastlatency",
                 "fastmove", "movementsound", "verticalspeed", "noslow", "simulation",
                 "baritone", "groundspoof", "break", "place"],
    "combat": ["reach", "aim", "killaura", "criticals", "fastheal", "swordblock", "autoclicker",
               "autobow", "autocrit", "crystalaura", "reachattack", "angle", "knockback",
               "pvp", "hbow", "hitbox", "fastswing", "combat", "aimassist", "aura",
               "wtap", "smooth", "blink", "backtrack", "macro", "nointeract"],
    "packet": ["badpackets", "cheststealer", "creative", "transaction", "keepalive",
               "invalid", "nospam", "overflow", "order", "spoofer", "payload",
               "overflowslot", "clientbrand", "hackedclient", "packetorder",
               "chat", "crash", "crasher", "exploit", "multiactions", "sprint"],
    "inventory": ["stealer", "inventory", "item", "swap", "nbt",
                  "durability", "fastdrop", "slowmotion", "fastplace"],
}

# Normalize competitor check names into comparable categories
CATEGORY_MAP = {
    "movement": "movement",
    "combat": "combat",
    "packet": "packet",
    "inventory": "inventory",
    "unknown": "movement",
}

# Check name normalization — strips prefixes/suffixes for comparison
STRIP_PATTERNS = [
    r"^(Check|Detection|Detector)", r"(Check|Detection|Detector)$",
    r"^(A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z)$",
    r"^(v\d+)$",
]


@dataclass
class CompetitorCheck:
    name: str
    raw_name: str
    file_path: str
    category: str
    stable_key: str
    methods: List[str] = field(default_factory=list)
    constants: Dict[str, str] = field(default_factory=dict)
    inner_classes: List[str] = field(default_factory=list)


@dataclass
class WindfallCheck:
    name: str
    stable_key: str
    category: str
    java_file: str


@dataclass
class DiffResult:
    competitor: str
    new_checks: List[CompetitorCheck]
    existing_matches: Dict[str, str]  # competitor_check -> windfall_check


def load_windfall_checks() -> List[WindfallCheck]:
    """Parse Windfall's existing checks from source code."""
    checks = []

    if not WINDFALL_CHECKS_DIR.exists():
        return checks

    for category_dir in WINDFALL_CHECKS_DIR.iterdir():
        if not category_dir.is_dir():
            continue
        category = category_dir.name

        for java_file in category_dir.glob("*.java"):
            content = java_file.read_text(encoding="utf-8", errors="ignore")

            name_match = re.search(r'@CheckData\s*\(\s*name\s*=\s*"([^"]+)"', content)
            key_match = re.search(r'stableKey\s*=\s*"([^"]+)"', content)

            if name_match and key_match:
                checks.append(WindfallCheck(
                    name=name_match.group(1),
                    stable_key=key_match.group(1),
                    category=category,
                    java_file=str(java_file),
                ))

    return checks


def normalize_check_name(name: str) -> str:
    """Strip suffixes like A, B, C and prefixes like Check, Detection."""
    base = name.strip()
    for pattern in STRIP_PATTERNS:
        base = re.sub(pattern, "", base).strip()
    if not base:
        base = name
    return base.lower()


def categorize_check(name: str, file_path: str) -> str:
    """Determine check category from name and file path."""
    name_lower = name.lower()
    path_lower = file_path.lower()

    for category, keywords in CHECK_CATEGORIES.items():
        for kw in keywords:
            if kw in name_lower or kw in path_lower:
                return category

    if "/combat/" in path_lower or "/pvp/" in path_lower:
        return "combat"
    if "/movement/" in path_lower or "/player/" in path_lower:
        return "movement"
    if "/packet/" in path_lower or "/network/" in path_lower or "/misc/" in path_lower:
        return "packet"
    if "/world/" in path_lower:
        return "movement"

    return "packet"


def extract_check_methods(content: str, language: str) -> List[str]:
    """Extract method names from check class content."""
    methods = []
    if language == "java":
        for match in re.finditer(r"(?:public|private|protected)\s+(?:void|boolean|int|double|float|long)\s+(\w+)\s*\(", content):
            methods.append(match.group(1))
    elif language == "go":
        for match in re.finditer(r"func\s+\([^)]+\)\s+(\w+)\s*\(", content):
            methods.append(match.group(1))
    elif language == "kotlin":
        for match in re.finditer(r"fun\s+(\w+)\s*\(", content):
            methods.append(match.group(1))
        for match in re.finditer(r"override\s+fun\s+(\w+)\s*\(", content):
            methods.append(match.group(1))
    return methods


def extract_constants(content: str, language: str) -> Dict[str, str]:
    """Extract constant fields from check class."""
    constants = {}
    if language == "java":
        for match in re.finditer(r"(?:private|public|protected|static|final)\s+(?:static\s+)?(?:final\s+)?(?:int|double|float|long|String)\s+(\w+)\s*=\s*([^;]+);", content):
            constants[match.group(1)] = match.group(2).strip()
    elif language == "go":
        for match in re.finditer(r"(\w+)\s*[:=]\s*(.+?)(?:\s*$)", content, re.MULTILINE):
            if match.group(1).isupper() or "_" in match.group(1):
                constants[match.group(1)] = match.group(2).strip()
    elif language == "kotlin":
        for match in re.finditer(r"(?:val|var)\s+(\w+)\s*[=:]\s*(.+)", content):
            if match.group(1).isupper() or "_" in match.group(1):
                constants[match.group(1)] = match.group(2).strip()
    return constants


def scan_competitor_checks(anti_cheat: str, config: dict) -> List[CompetitorCheck]:
    """Scan a competitor's repository for check implementations."""
    checks = []
    repo_path = CACHE_DIR / anti_cheat
    language = config["language"]

    for check_path in config["check_paths"]:
        full_path = repo_path / check_path
        if not full_path.exists():
            print(f"  [WARN] Check path not found: {full_path}")
            continue

        if language == "go":
            extensions = ["*.go"]
        elif language == "kotlin":
            extensions = ["*.kt", "*.java"]
        else:
            extensions = ["*.java"]

        for ext in extensions:
            for source_file in full_path.rglob(ext):
                content = source_file.read_text(encoding="utf-8", errors="ignore")

                # Find class/struct declarations that look like checks
                if language == "java":
                    class_match = re.search(
                        r"(?:public\s+)?class\s+(\w+)\s+(?:extends\s+\w+\s+)?(?:implements\s+[\w,\s]+)?\s*\{",
                        content
                    )
                elif language == "go":
                    # Go: look for type CheckXxx struct or func containing "Check" name
                    class_match = re.search(r"type\s+(Check\w+)\s+struct", content)
                    if not class_match:
                        # Also match standalone check functions
                        class_match = re.search(r"func\s+(Check\w+)\s*\(", content)
                else:
                    class_match = re.search(r"class\s+(\w+)(?:\s*:\s*[\w,\s]+)?\s*\{?", content)

                if not class_match:
                    continue

                class_name = class_match.group(1)

                # Filter: skip abstract classes, base classes, interfaces, data holders
                if any(skip in class_name.lower() for skip in
                       ["base", "abstract", "interface", "util", "helper", "data",
                        "config", "manager", "listener", "handler", "event", "task",
                        "cache", "storage", "database", "service", "provider",
                        "processor", "profiles", "packets", "cserver", "sysutils",
                        "compression", "wrapper", "bukkit", "transmitter"]):
                    continue

                raw_name = class_name
                normalized = normalize_check_name(class_name)
                category = categorize_check(class_name, str(source_file))
                stable_key = f"windfall.{category}.{normalized}"

                methods = extract_check_methods(content, language)
                constants = extract_constants(content, language)

                # Check if this file has violation detection logic
                has_detection = any(kw in content.lower() for kw in
                                   ["flag", "violation", "alert", "punish", "setback",
                                    "buffer", "threshold", "exceed", "exceeds", "exceeded"])

                if not has_detection and len(constants) < 2:
                    continue

                checks.append(CompetitorCheck(
                    name=normalized,
                    raw_name=raw_name,
                    file_path=str(source_file.relative_to(repo_path)),
                    category=category,
                    stable_key=stable_key,
                    methods=methods,
                    constants=constants,
                ))

    return checks


def match_competitor_to_windfall(
    competitor_checks: List[CompetitorCheck],
    windfall_checks: List[WindfallCheck],
) -> Tuple[List[CompetitorCheck], Dict[str, str]]:
    """Match competitor checks against Windfall's existing checks.
    Returns (unmatched_new, matched_pairs)."""

    windfall_names = {}
    windfall_keys = {}
    for wc in windfall_checks:
        norm = normalize_check_name(wc.name)
        windfall_names[norm] = wc
        windfall_keys[wc.stable_key] = wc

    new_checks = []
    matches = {}

    for cc in competitor_checks:
        # Direct name match
        if cc.name in windfall_names:
            matches[cc.raw_name] = windfall_names[cc.name].name
            continue

        # Reverse match: windfall name contains competitor name or vice versa
        matched = False
        for norm, wc in windfall_names.items():
            if cc.name in norm or norm in cc.name:
                matches[cc.raw_name] = wc.name
                matched = True
                break
            # Category match on same detection concept
            if cc.category == wc.category:
                # Both are speed checks, both are reach checks, etc.
                for kw in CHECK_CATEGORIES.get(cc.category, []):
                    if kw in cc.name and kw in norm:
                        matches[cc.raw_name] = wc.name
                        matched = True
                        break
                if matched:
                    break

        if not matched:
            new_checks.append(cc)

    return new_checks, matches


def build_windfall_check_skeleton(check: CompetitorCheck, anti_cheat: str) -> str:
    """Generate a Java check skeleton following Windfall conventions."""
    class_name = check.name.capitalize() + "Check"
    stable_key = check.stable_key
    category = check.category if check.category != "unknown" else "movement"

    # Choose base type based on competitor's methods
    has_packet = any(m.lower() in ["onpacketreceive", "onpacket", "handlepacket"]
                     for m in check.methods)
    has_tick = any(m.lower() in ["ontick", "tick", "onupdate", "update"]
                   for m in check.methods)

    extends = "Check"
    if has_packet:
        extends = "Check implements PacketCheck"

    # Build constant stubs from competitor's constants
    constant_lines = []
    for const_name, const_val in list(check.constants.items())[:8]:
        clean_val = const_val.rstrip(";").strip()
        if clean_val.startswith('"'):
            constant_lines.append(f'    private static final String {const_name} = {clean_val};')
        elif re.match(r"^-?\d+\.?\d*[fFdD]?$", clean_val):
            constant_lines.append(f'    private static final double {const_name} = {clean_val.rstrip("fFdD")};')
        elif re.match(r"^-?\d+$", clean_val):
            constant_lines.append(f'    private static final int {const_name} = {clean_val};')

    if not constant_lines:
        constant_lines = [
            "    private static final double FLAG_THRESHOLD = 1.0;",
            "    private static final double BUFFER_LIMIT = 5.0;",
        ]

    methods_section = ""
    if has_packet:
        methods_section = """
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        // TODO: Implement detection logic
        // Reference: {anti_cheat} implementation in {source_file}
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }"""
    else:
        methods_section = """
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }"""

    return f"""package io.windfall.anticheat.core.check.impl.{category};

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Detects {check.name} violations.
 *
 * Ported from {anti_cheat} ({check.raw_name}).
 * Source: {check.file_path}
 *
 * TODO: Verify detection logic, tune thresholds, test on live server.
 */
@CheckData(name = "{check.name.capitalize()} A", stableKey = "{stable_key}", decay = 0.01, setbackVl = 20)
public class {class_name} extends {extends} {{

{chr(10).join(constant_lines)}
{methods_section}
}}
"""


def generate_report(
    windfall_checks: List[WindfallCheck],
    all_diffs: Dict[str, DiffResult],
) -> str:
    """Generate the markdown monitoring report."""
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")

    lines = [
        "# Windfall Anti-Cheat Monitor Report",
        "",
        f"**Generated:** {now}",
        "",
        "---",
        "",
        "## Windfall Current Checks",
        "",
        f"**Total: {len(windfall_checks)} checks**",
        "",
    ]

    # Group by category
    by_category = {}
    for wc in windfall_checks:
        by_category.setdefault(wc.category, []).append(wc)

    for cat in ["combat", "movement", "packet"]:
        if cat in by_category:
            lines.append(f"### {cat.capitalize()}")
            for wc in by_category[cat]:
                lines.append(f"- `{wc.stable_key}` — {wc.name}")
            lines.append("")

    lines.extend([
        "---",
        "",
        "## Competitor Analysis",
        "",
    ])

    total_new = 0
    for name, diff in all_diffs.items():
        lines.append(f"### {name}")
        lines.append("")

        if diff.new_checks:
            lines.append(f"**Missing from Windfall ({len(diff.new_checks)} checks):**")
            lines.append("")
            for nc in diff.new_checks:
                lines.append(f"- `{nc.category}` **{nc.raw_name}** → `{nc.stable_key}`")
                lines.append(f"  - Source: `{nc.file_path}`")
                if nc.constants:
                    for k, v in list(nc.constants.items())[:3]:
                        lines.append(f"  - `{k} = {v}`")
            lines.append("")
            total_new += len(diff.new_checks)
        else:
            lines.append("**No new checks detected.**")
            lines.append("")

        if diff.existing_matches:
            lines.append("**Matched with existing Windfall checks:**")
            lines.append("")
            for comp, wf in diff.existing_matches.items():
                lines.append(f"- `{comp}` → `{wf}`")
            lines.append("")

    lines.extend([
        "---",
        "",
        f"## Summary",
        "",
        f"- Windfall has **{len(windfall_checks)} checks**",
        f"- Found **{total_new} new checks** across competitors that Windfall doesn't have",
        "",
    ])

    if total_new > 0:
        lines.extend([
            "## Recommendations",
            "",
            "1. Review generated skeleton files in `src/main/java/io/windfall/anticheat/core/check/impl/`",
            "2. Implement detection logic based on competitor reference",
            "3. Tune thresholds and buffer values for each check",
            "4. Register new checks in `CheckManager.java`",
            "5. Add config entries to `config.yml`",
            "6. Test on live server before enabling punishable mode",
            "",
        ])

    return "\n".join(lines)


def main():
    print("=" * 60)
    print("  Windfall Anti-Cheat Monitor — Analysis Engine")
    print("=" * 60)

    # Load Windfall's existing checks
    windfall_checks = load_windfall_checks()
    print(f"\nLoaded {len(windfall_checks)} Windfall checks:")
    for wc in windfall_checks:
        print(f"  [{wc.category}] {wc.name} ({wc.stable_key})")

    all_diffs = {}

    for anti_cheat, config in ANTI_CHEATS.items():
        print(f"\n{'─' * 50}")
        print(f"Scanning {anti_cheat}...")

        repo_path = CACHE_DIR / anti_cheat
        if not repo_path.exists():
            print(f"  [SKIP] Repository not cloned: {repo_path}")
            continue

        # Scan competitor checks
        comp_checks = scan_competitor_checks(anti_cheat, config)
        print(f"  Found {len(comp_checks)} checks:")
        for cc in comp_checks:
            print(f"    [{cc.category}] {cc.raw_name} ({cc.file_path})")

        # Diff against Windfall
        new_checks, matches = match_competitor_to_windfall(comp_checks, windfall_checks)

        if new_checks:
            print(f"  ⚠  {len(new_checks)} NEW checks (not in Windfall):")
            for nc in new_checks:
                print(f"      [{nc.category}] {nc.raw_name}")
        else:
            print("  ✓  All checks already covered by Windfall")

        if matches:
            print(f"  Matched {len(matches)} competitor checks to Windfall equivalents")

        all_diffs[anti_cheat] = DiffResult(
            competitor=anti_cheat,
            new_checks=new_checks,
            existing_matches=matches,
        )

    # Generate report
    report = generate_report(windfall_checks, all_diffs)
    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.write_text(report, encoding="utf-8")
    print(f"\nReport written to {REPORT_PATH}")

    # Save diff JSON for auto-implementation step
    diff_data = {}
    for name, diff in all_diffs.items():
        diff_data[name] = {
            "new_checks": [
                {
                    "name": nc.name,
                    "raw_name": nc.raw_name,
                    "category": nc.category,
                    "stable_key": nc.stable_key,
                    "file_path": nc.file_path,
                    "constants": nc.constants,
                    "methods": nc.methods,
                }
                for nc in diff.new_checks
            ],
            "existing_matches": diff.existing_matches,
        }

    DIFF_PATH.write_text(json.dumps(diff_data, indent=2), encoding="utf-8")
    print(f"Diff data written to {DIFF_PATH}")

    # Print summary
    total_new = sum(len(d.new_checks) for d in all_diffs.values())
    print(f"\n{'=' * 60}")
    print(f"  SUMMARY: {total_new} new checks found across all competitors")
    print(f"{'=' * 60}")

    return total_new


if __name__ == "__main__":
    exit(0 if main() == 0 else 0)
