# Windfall Anti-Cheat

Enterprise-grade packet-based anti-cheat for **Minecraft 1.7 – 26.2+** (Spigot / Paper / Folia).

Windfall intercepts incoming packets via [PacketEvents 2](https://github.com/retrooper/packetevents) and evaluates player behaviour against a configurable set of checks. A single JAR works across every supported server version — no separate builds required.

---

## Checks

| Category | Checks |
|----------|--------|
| **Combat** (12) | Aim, Autoclicker, Backtrack, Criticals, Fast Heal (1.7–1.8), Hitboxes, Kill Aura, Macro, Multi Interact, Reach, Self Interact, Sword Block (1.7–1.8) |
| **Movement** (29) | Air Liquid Break, Air Liquid Place, Baritone, Elytra (1.9+), Far Break, Far Place, Fast Break, Flight, Ground Spoof, Invalid Break, Invalid Place, Motion, Multi Break, Multi Place, NoFall, NoSlow, No Swing, Phase, Position Break, Position Place, Rotation Break, Rotation Place, Scaffold, Simulation, Speed, Step, Timer, Velocity, Wrong Break |
| **Packet** (10) | Bad Packets, Chat, Chest Stealer, Client Brand, Crash, Creative, Exploit, Packet Order, Sprint, Vehicle |
| **Inventory** (1) | Inventory |

### 5-Layer Compatibility System (v1.5.0)

Windfall uses a 5-layer compatibility system to adapt checks across all supported MC versions and server forks:

| Layer | What it does |
|-------|-------------|
| **Version Range** | `minVersion` / `maxVersion` — checks only load when the server protocol is in range |
| **Fork Detection** | `ServerFork` detects Folia / Purpur / Paper / Spigot / Bukkit via reflection; checks can declare `disableOnFolia`, `disableOnPurpur` |
| **Plugin Detection** | `PluginDetector` finds ViaVersion, ViaBackwards, ViaRewind, Geyser, OldCombatMechanics at runtime |
| **Player Profile** | Per-player: protocol version, Bedrock status, ViaVersion version gap → tolerance multipliers applied |
| **Config Override** | Server admins can manually disable any check via `config.yml` compatibility section |

Checks also declare `@CheckData(compat = {...})` flags (e.g. `VIAVERSION_SENSITIVE`, `PURPUR_KB_DEPENDENT`) that control whether the check is **disabled** or **relaxed** (higher thresholds) when conditions don't match.

---

## Requirements

- **Java 11** or newer
- [PacketEvents 2.13.0+](https://github.com/retrooper/packetevents) — must be installed as a separate plugin (not shaded)
- Minecraft server 1.8 or newer

## Installation

1. Build with Maven:

```bash
mvn clean package
```

2. Place `target/Windfall.jar` into your server's `plugins/` folder alongside `packetevents-spigot-2.x.x.jar`.
3. Restart the server.

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

| Platform | Supported |
|----------|-----------|
| Spigot 1.8+ | Yes |
| Paper 1.13+ | Yes |
| Folia (1.21.2+) | Yes |
| Purpur | Yes (custom KB detection) |
| ViaVersion / ViaBackwards | Yes (version gap tolerance) |
| Geyser / Floodgate (Bedrock) | Yes (with Bedrock-specific tolerances) |

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
