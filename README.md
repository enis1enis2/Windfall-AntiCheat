# Windfall Anti-Cheat

Enterprise-grade packet-based anti-cheat for **Minecraft 1.7 – 26.2+** (Spigot / Paper / Folia).

Windfall intercepts incoming packets via [PacketEvents 2](https://github.com/retrooper/packetevents) and evaluates player behaviour against a configurable set of checks. A single JAR works across every supported server version — no separate builds required.

---

## Checks

| Category | Checks |
|----------|--------|
| **Combat** (11) | Aim, Autoclicker, Backtrack, Criticals, Fast Heal (1.7–1.8), Hitboxes, Kill Aura, Multi Interact, Reach, Self Interact, Sword Block (1.7–1.8) |
| **Movement** (14) | Baritone, Elytra (1.9+), Flight, Ground Spoof, Motion, NoFall, NoSlow, Phase, Scaffold, Simulation, Speed, Step, Timer, Velocity |
| **Packet** (8) | Bad Packets, Chest Stealer, Chat, Crash, Creative, Exploit, Packet Order, Sprint |

### Version-Aware Registration

Each check declares a `minVersion` / `maxVersion` (Minecraft protocol version). On startup `CheckManager` reads the server's protocol and only registers checks that apply — legacy-only checks (Fast Heal, Sword Block) are automatically skipped on modern servers and vice-versa.

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
| Folia | Yes |
| ViaVersion / ViaBackwards | Yes |
| Geyser / Floodgate (Bedrock) | Yes (optional) |

---

## Building from Source

```bash
git clone https://github.com/enis1enis2/Windfall-AntiCheat.git
cd Windfall-AntiCheat
mvn clean package
```

Requires JDK 11+ and Maven 3.6+.

---

## License

MIT
