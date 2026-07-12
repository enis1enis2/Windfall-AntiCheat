<h1 align="center">⚔️ Windfall Anti-Cheat</h1>

<p align="center">
  <strong>Enterprise-grade packet-based anti-cheat for Minecraft 1.7 – 26.2+</strong><br>
  <sub>Spigot &middot; Paper &middot; Folia &middot; Purpur &middot; ViaVersion &middot; Geyser</sub>
</p>

<p align="center">
  <a href="https://github.com/enis1enis2/Windfall-AntiCheat/actions"><img src="https://github.com/enis1enis2/Windfall-AntiCheat/actions/workflows/build.yml/badge.svg" alt="Build Status"></a>
  <a href="https://github.com/enis1enis2/Windfall-AntiCheat/releases"><img src="https://img.shields.io/github/v/release/enis1enis2/Windfall-AntiCheat" alt="Release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue" alt="License"></a>
  <img src="https://img.shields.io/badge/java-11%2B-orange" alt="Java 11+">
  <img src="https://img.shields.io/badge/minecraft-1.7--26.2%2B-green" alt="Minecraft">
</p>

---

## Overview

Windfall intercepts incoming packets via [PacketEvents 2](https://github.com/retrooper/packetevents) and evaluates player behaviour against a configurable set of checks. A **single JAR** works across every supported server version — no separate builds required.

**52 checks** across 4 categories, with a **5-layer compatibility system** that adapts detection thresholds per-player based on protocol version, server fork, installed plugins, and Bedrock status.

---

## Checks

<details>
<summary><strong>Combat</strong> — 12 checks</summary>

| Check | Description |
|-------|-------------|
| Aim | Instant-snap and yaw/pitch variance ratio detection |
| Autoclicker | Standard deviation-based click pattern analysis |
| Backtrack | Movement-to-attack timing gap detection |
| Criticals | Vanilla crit-hit motion requirement validation |
| Fast Heal | Frequency-based and spike-based food consumption detection |
| Hitboxes | Look-vector projection and hit-ratio analysis |
| Kill Aura | Multi-aura and rotation symmetry detection |
| Macro | Movement pattern repetition detection |
| Multi Interact | Multi-entity-per-tick detection |
| Reach | AABB-to-eye distance measurement with lag compensation |
| Self Interact | Self-targeting packet detection |
| Sword Block | Block+attack timing abuse (1.7–1.8) |

</details>

<details>
<summary><strong>Movement</strong> — 29 checks</summary>

| Check | Description |
|-------|-------------|
| Air Liquid Break | Breaking blocks while falling through air/liquid |
| Air Liquid Place | Placing blocks while falling through air/liquid |
| Baritone | Automated pathfinding: straight lines + constant speed |
| Elytra | Illegal elytra flight: speed, hover, kick-boost, ascent |
| Far Break | Breaking blocks beyond vanilla reach |
| Far Place | Placing blocks beyond vanilla reach |
| Fast Break | Breaking blocks faster than vanilla break times |
| Flight | Vertical velocity prediction and hover detection |
| Ground Spoof | Server-side ground state validation |
| Invalid Break | Breaking air or indestructible blocks |
| Invalid Place | Placing blocks in invalid positions |
| Motion | General motion anomaly detection |
| Multi Break | Multiple START_DIGGING packets per tick |
| Multi Place | Multiple block placements per tick |
| NoFall | Missing or incorrect fall packets |
| NoSlow | Bypassing item-use movement slowdown |
| No Swing | Missing arm-swing animation on block interactions |
| Phase | Wall clipping / phase detection |
| Position Break | Squared-distance reach validation |
| Position Place | Squared-distance placement validation |
| Rotation Break | Excessive view rotation during block break |
| Rotation Place | Excessive view rotation during block place |
| Scaffold | Automated block placement (tower/bridge) |
| Simulation | Full movement simulation comparison |
| Speed | Horizontal acceleration prediction |
| Step | Step-up height validation |
| Timer | Client tick rate manipulation |
| Velocity | Knockback velocity validation |
| Wrong Break | Breaking blocks at wrong positions |

</details>

<details>
<summary><strong>Packet</strong> — 10 checks</summary>

| Check | Description |
|-------|-------------|
| Bad Packets | Invalid packet structure and field values |
| Chat | Chat message rate and pattern analysis |
| Chest Stealer | Abnormal chest inventory interaction speed |
| Client Brand | Client modification detection via brand string |
| Crash | Oversized packets and creative exploits |
| Creative | Creative mode validation and rate limiting |
| Exploit | Known exploit packet detection |
| Packet Order | Invalid packet sequence and burst detection |
| Sprint | Impossible sprint state changes |
| Vehicle | Vehicle interaction exploit detection |

</details>

<details>
<summary><strong>Inventory</strong> — 1 check</summary>

| Check | Description |
|-------|-------------|
| Inventory | Abnormal inventory interaction patterns |

</details>

---

## 5-Layer Compatibility System

Windfall adapts checks across all supported MC versions and server forks through five layers:

```
┌─────────────────────────────────────────────────────────────┐
│  1. Version Range         minVersion / maxVersion           │
│  2. Fork Detection        Folia / Purpur / Paper / Spigot   │
│  3. Plugin Detection      ViaVersion / Geyser / OCM         │
│  4. Player Profile        Protocol / Bedrock / Via gap       │
│  5. Config Override       Manual disable per check           │
└─────────────────────────────────────────────────────────────┘
```

| Layer | Mechanism |
|-------|-----------|
| **Version Range** | `minVersion` / `maxVersion` — checks only load when the server protocol is in range |
| **Fork Detection** | `ServerFork` detects Folia / Purpur / Paper / Spigot / Bukkit via reflection; checks can declare `disableOnFolia`, `disableOnPurpur` |
| **Plugin Detection** | `PluginDetector` finds ViaVersion, ViaBackwards, ViaRewind, Geyser, OldCombatMechanics at runtime |
| **Player Profile** | Per-player: protocol version, Bedrock status, ViaVersion version gap → tolerance multipliers applied |
| **Config Override** | Server admins can manually disable any check via `config.yml` compatibility section |

Checks declare `@CheckData(compat = {...})` flags (e.g. `VIAVERSION_SENSITIVE`, `PURPUR_KB_DEPENDENT`) that control whether the check is **disabled** or **relaxed** (higher thresholds) when conditions don't match.

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Java | 11+ |
| Minecraft Server | 1.8+ |
| [PacketEvents](https://github.com/retrooper/packetevents) | 2.13.0+ (separate plugin) |

## Installation

1. **Build** with Maven:

```bash
mvn clean package
```

2. **Install** — place both files in `plugins/`:
   - `target/Windfall.jar`
   - `packetevents-spigot-2.x.x.jar`

3. **Restart** the server.

---

## Commands

| Command | Alias | Description |
|---------|-------|-------------|
| `/windfall help` | `/wf`, `/wfall` | Show help |
| `/windfall reload` | | Reload configuration |
| `/windfall toggle <check>` | | Enable / disable a check |
| `/windfall alerts` | | Toggle alert delivery |

---

## Configuration

On first run Windfall generates `plugins/Windfall/config.yml`. All checks, thresholds, punishment settings, and Discord webhook URLs can be customised there. Use `/windfall reload` to apply changes without restarting.

---

## Platform Compatibility

| Platform | Status | Notes |
|----------|--------|-------|
| Spigot 1.8+ | Supported | Base compatibility |
| Paper 1.13+ | Supported | Async chunk loading, EAR 2.0 |
| Folia 1.21.2+ | Supported | Regionised multithreading via EntityScheduler |
| Purpur | Supported | Custom knockback detection from `purpur.yml` |
| ViaVersion / ViaBackwards | Supported | Per-player protocol adaptation |
| ViaRewind | Supported | Legacy client support |
| Geyser / Floodgate | Supported | Bedrock-specific movement tolerances |
| OldCombatMechanics | Supported | 1.8 combat emulation awareness |

---

## Architecture

```
windfall/
├── core/
│   ├── check/           # Check registration, base classes, @CheckData
│   │   └── impl/
│   │       ├── combat/      # 12 combat checks
│   │       ├── movement/    # 29 movement checks
│   │       ├── packet/      # 10 packet checks
│   │       └── inventory/   # 1 inventory check
│   ├── physics/         # Prediction engine, physics constants, version branching
│   ├── player/          # WindfallPlayer state, PlayerManager, PlayerProfile
│   ├── network/         # PacketListener, PacketCheck
│   ├── config/          # WindfallConfig, config.yml parsing
│   ├── punishment/      # PunishmentEngine (warn → kick → ban)
│   ├── severity/        # SeverityManager, VL escalation
│   ├── alert/           # AlertManager, Discord webhook
│   ├── bedrock/         # Geyser detection, BedrockInfo
│   ├── platform/        # FoliaCompat, PurpurCompat
│   ├── version/         # VersionManager, VersionBracket, ServerFork
│   ├── plugin/          # PluginDetector
│   ├── compensation/    # TransactionManager (ping measurement)
│   └── util/            # MaterialUtils, MathUtil
├── gui/                 # CheckListGui (admin inventory UI)
└── WindfallPlugin.java  # Plugin entry point
```

---

## Building from Source

```bash
git clone https://github.com/enis1enis2/Windfall-AntiCheat.git
cd Windfall-AntiCheat
mvn clean package
```

Requires JDK 11+ and Maven 3.6+.

---

## Credits

Windfall's architecture draws inspiration from several open-source anti-cheat projects. Huge respect to their authors for pioneering these patterns in the community:

- **[ArrowAntiCheat](https://github.com/StelGR/ArrowAntiCheat)** by **StelGR** — foundational check framework, `@CheckData` annotation system, and packet-based detection patterns that Windfall's check registration is built on
- **[GrimAC](https://github.com/GrimAnticheat/Grim)** by **GrimAnticheat** — movement prediction engine, physics constants (gravity, drag, friction), and the vertical/horizontal delta validation model used across Windfall's movement checks
- **[TruthfulAC](https://github.com/TawnyE/TruthfulAC)** by **TawnyE** — per-player state isolation pattern, buffer escalation mechanics, and the decay-based violation level system

If you're building your own anti-cheat, these projects are excellent references for understanding how modern packet-level detection works.

---

## License

MIT License — Copyright (c) 2026 Enis Polat

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
