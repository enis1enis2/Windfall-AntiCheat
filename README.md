# Windfall Anti-Cheat

Enterprise-grade packet-based anti-cheat for **Minecraft 1.7 - 1.21+** (Spigot / Paper / Folia).

Windfall intercepts incoming packets via [PacketEvents 2](https://github.com/retrooper/packetevents) and evaluates player behaviour against a configurable set of checks. It ships with version-aware physics constants so a single JAR works across every supported server version — no separate builds required.

---

## Features

| Category | Checks |
|----------|--------|
| **Combat** | Aim, Criticals, Fast Heal (1.7-1.8), Kill Aura, Reach, Sword Block (1.7-1.8) |
| **Movement** | Elytra (1.9+), Flight, No Fall, Scaffold, Speed, Step, Timer, Velocity |
| **Packet** | Bad Packets, Chest Stealer, Creative |

### Version-Aware Registration

Each check declares a `minVersion` / `maxVersion` (Minecraft protocol version). On startup CheckManager reads the server's protocol and only registers checks that apply — legacy-only checks (Fast Heal, Sword Block) are automatically skipped on modern servers and vice-versa.

### Platform Compatibility

| Platform | Supported |
|----------|-----------|
| Spigot 1.8+ | Yes |
| Paper 1.13+ | Yes |
| Folia | Yes |
| ViaVersion / ViaBackwards | Yes (tested) |
| Geyser / Floodgate (Bedrock) | Yes (optional) |

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

## Commands

| Command | Alias | Description |
|---------|-------|-------------|
| `/windfall help` | `/wf`, `/wfall` | Show help |
| `/windfall reload` | | Reload configuration |
| `/windfall toggle <check>` | | Enable/disable a check |
| `/windfall alerts` | | Toggle alert delivery |

## Configuration

On first run Windfall generates `plugins/Windfall/config.yml`. All checks, thresholds, punishment settings, and Discord webhook URLs can be customised there. Use `/windfall reload` to apply changes without restarting.

## Building from Source

```bash
git clone https://github.com/windfall-anticheat/windfall.git
cd windfall
mvn clean package
```

Requires JDK 11+ and Maven 3.6+.

## CI/CD

Every push triggers a GitHub Actions workflow that compiles the plugin and uploads the JAR as a release asset. See [`.github/workflows/build.yml`](.github/workflows/build.yml).

## License

MIT
