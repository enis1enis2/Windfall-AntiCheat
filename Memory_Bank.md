# Windfall Anti-Cheat — Memory Bank

## 1. Project Identity

| Field | Value |
|---|---|
| **Name** | Windfall AntiCheat |
| **Artifact** | `io.windfall:windfall-anticheat:2.3.2` |
| **Description** | Enterprise-grade packet-based anti-cheat for Minecraft 1.7–26.2+ |
| **Entry** | `io.windfall.anticheat.WindfallPlugin` |
| **Build** | Java 11, Maven 3.6+, shade bStats + Prometheus |
| **PacketEvents** | 2.13.0 (provided, never shaded) |

## 2. Dependency Graph (plugin.yml)

```
depend: [packetevents]
softdepend: [Geyser-Spigot, floodgate, ViaVersion]
folia-supported: true
api-version: 1.21
```

Permissions: `windfall.admin` (op), `windfall.alerts` (op), `windfall.gui` (op)

## 3. Build & Deployment

```bash
mvn clean package                        # produces target/Windfall.jar
mvn clean package -Pobfuscate            # also produces Windfall-obfuscated.jar via ProGuard
```

- **Shaded**: bStats 3.2.1 (→ `io.windfall.anticheat.lib.bstats`), Prometheus simpleclient 0.16.0 (→ `io.windfall.anticheat.lib.prometheus`)
- **External deps**: PacketEvents 2.13.0 (jar in plugins/), Geyser API 2.10.0-SNAPSHOT, WorldGuard 7.0.17
- **Test**: JUnit 5.6.1.2, Mockito 5.23.0, ByteBuddy agent for mocking
- **Security**: OWASP dependency-check (fail on CVSS ≥ 7), GPG signing (skip by default)

## 4. Architecture Overview

```
WindfallPlugin
├── WindfallConfig        — typed config accessors, per-check fallback
├── VersionManager        — Bukkit version string → protocol number
├── ServerFork            — Folia→Purpur→Paper→Spigot→Bukkit detection
├── PluginDetector        — ViaVersion, Geyser, OldCombatMechanics, etc.
├── PlatformScheduler     — Bukkit/Folia abstracted scheduler
├── FoliaCompat           — region-thread reflection layer
├── PurpurCompat          — purpur.yml knockback reader
├── PlayerManager         — ConcurrentHashMap<UUID, WindfallPlayer>
│   ├── WindfallPlayer    — per-player state (volatile immutable snapshots)
│   ├── PlayerProfile     — version/platform profile, tolerance multipliers
│   └── ActionData        — block-action exemptions for movement checks
├── Compensation Layer
│   ├── TransactionManager   — RTT measurement via Ping/WinConf packets
│   ├── PingPongManager      — dual-ping (pre/post tick) sandwich tracking
│   ├── LatencyCompensator   — deferred block changes per player latency
│   ├── SimulationEngine     — multi-scenario physics simulation (16 scenarios)
│   ├── CompensatedWorld     — per-player latency-aware world view
│   └── CompensatedEntities  — per-entity position history (40-tick ring buffer)
├── Physics Engine
│   ├── PhysicsConstants     — IEEE-exact MC physics values
│   ├── VersionPhysics       — version-dependent physics (height, reach, gravity)
│   ├── PredictionEngine     — stateless movement prediction (gravity, drag, friction)
│   ├── PredictionContext    — immutable per-tick snapshot shared by all checks
│   └── BoundingBox          — immutable AABB for collision & reach
├── Check Manager (53 checks)
│   ├── Check base class     — flag/buffer/VL/setback/reward pipeline
│   ├── CheckData annotation — name, stableKey, decay, compat, version range
│   ├── CompatFlag enum      — version/platform/mismatch flags
│   └── PacketCheck interface — default no-op packet handlers
├── Bedrock Layer
│   ├── GeyserManager        — Floodgate/GeyserApi reflection bridge
│   ├── BedrockInfo          — immutable device info snapshot
│   └── GeysersTracker       — Minecraft 26.2+ geyser push detection
├── SeverityManager          — VL-scaled violation increments (1.0x–2.0x)
├── ViolationPattern         — cross-session repeat offender detection
├── PacketFingerprint        — 5-vector cheat client fingerprinting
├── PunishmentEngine          — warn→kick→tempban→permban escalation
├── AlertManager             — in-game alerts + Discord webhook dispatch
├── DiscordWebhook           — zero-dep webhook client (HttpURLConnection)
├── WindfallMetrics          — bStats (5 custom charts)
├── WindfallPrometheus       — Prometheus HTTP endpoint (default :9211)
├── CommandManager           — /windfall (12 subcommands)
│   └── ChecklistGUI         — paginated inventory GUI
├── PacketListener           — central packet interceptor
├── WorldGuardCompat         — full-reflection WG region queries
├── API Layer
│   ├── WindfallAPI interface — read-only API
│   ├── WindfallAPIImpl      — implementation
│   ├── WindfallProvider     — static singleton accessor
│   └── PlayerData           — immutable snapshot
└── Util
    ├── MathUtil             — pure math helpers
    └── MaterialUtils        — cached material classification
```

## 5. Check Inventory — 53 Checks

### Combat (12)

| Check | Name | Key | Target | Key Technique | Version Range |
|---|---|---|---|---|---|
| AimCheck | Aim A | windfall.combat.aim | Aimbot/aim-assist | Instant-snap + yaw/pitch variance ratio | All |
| AutoclickerCheck | Autoclicker A | windfall.combat.autoclicker | Autoclickers | Std dev of click intervals + CPS bounds | All |
| BacktrackCheck | Backtrack A | windfall.combat.backtrack | Delayed position | Attack-to-movement delay >500ms | All |
| CriticalsCheck | Criticals A | windfall.combat.criticals | Faked crit hits | DeltaY outside [0.11, 0.5] while airborne | All |
| FastHealCheck | Fast Heal A | windfall.combat.fastheal | Fast regen | Frequency + spike in 500ms window | 5–107 (1.6–1.8) |
| HitboxesCheck | Hitboxes A | windfall.combat.hitboxes | Expanded hitboxes | Look projection + 80% hit ratio | All |
| KillAuraCheck | Kill Aura A | windfall.combat.killaura | Multi-aura + symmetry | Target counting + yaw symmetry ratio | All |
| MacroCheck | Macro A | windfall.combat.macro | Movement macros | Pattern string repetition detection | All |
| MultiInteractCheck | Multi Interact A | windfall.combat.multiinteract | Per-tick multi-aura | >2 entities in 60ms window | All |
| ReachCheck | Reach A | windfall.combat.reach | Reach extension | AABB clamp + multi-snapshot lag comp | All |
| SelfInteractCheck | Self Interact A | windfall.combat.selfinteract | Self-attack | Entity ID == self → instant kick | All |
| SwordBlockCheck | Sword Block A | windfall.combat.swordblock | Auto blocking | Block-attack timing + speed ratio | 5–107 |

### Movement (30)

| Check | Name | Key | Target |
|---|---|---|---|
| AirLiquidBreakCheck | Air Liquid Break | windfall.movement.airliquidbreak | Breaking blocks in air/liquid |
| AirLiquidPlaceCheck | Air Liquid Place | windfall.movement.airliquidplace | Placing blocks in air/liquid |
| BaritoneCheck | Baritone A | windfall.movement.baritone | Automated pathfinding (straight lines + constant speed) |
| ElytraCheck | Elytra A | windfall.movement.elytra | Illegal elytra flight (speed/hover/ascent) |
| FarBreakCheck | Far Break A | windfall.movement.farbreak | Breaking blocks beyond reach |
| FarPlaceCheck | Far Place A | windfall.movement.farplace | Placing blocks beyond reach |
| FastBreakCheck | Fast Break A | windfall.movement.fastbreak | Breaking faster than vanilla time |
| FlightCheck | Fly A | windfall.movement.fly | Flight/hover/no-fall |
| GroundSpoofCheck | Ground Spoof A | windfall.movement.groundspoof | False on-ground flag |
| IllegalMoveCheck | Illegal Move | windfall.movement.illegalmove | Teleportation + vertical clipping through blocks |
| InvalidBreakCheck | Invalid Break A | windfall.movement.invalidbreak | Breaking air/indestructible blocks |
| InvalidPlaceCheck | Invalid Place A | windfall.movement.invalidplace | Rate limit, occupied, self-intersection |
| MotionCheck | Motion A | windfall.movement.motion | Blatant speed boundaries |
| MultiBreakCheck | Multi Break | windfall.movement.multibreak | >1 break per tick (Nuker) |
| MultiPlaceCheck | Multi Place | windfall.movement.multiplace | >1 place per tick |
| NoFallCheck | NoFall A | windfall.movement.nofall | No-fall damage bypass |
| NoSlowCheck | NoSlow A | windfall.movement.noslow | Bypassing item-use slowdown |
| NoSwingCheck | No Swing A | windfall.movement.noswing | Missing arm-swing animation |
| PhaseCheck | Phase A | windfall.movement.phase | Wall noclipping |
| PositionBreakCheck | Position Break | windfall.movement.positionbreak | Squared-distance break check |
| PositionPlaceCheck | Position Place | windfall.movement.positionplace | Squared-distance place check |
| RotationBreakCheck | Rotation Break A | windfall.movement.rotationbreak | Rotation change during break >45° |
| RotationPlaceCheck | Rotation Place | windfall.movement.rotationplace | Rotation not facing placed block |
| ScaffoldCheck | Scaffold A | windfall.movement.scaffold | Auto-bridge (speed + tower + rotation) |
| SimulationCheck | Simulation A | windfall.movement.simulation | Physics simulation mismatch |
| SpeedCheck | Speed A | windfall.movement.speed | Horizontal speed prediction |
| StepCheck | Step A | windfall.movement.step | Step height violation |
| TimerCheck | Timer A | windfall.movement.timer | Packet rate (speed/slow hacks) |
| VelocityCheck | Velocity A | windfall.movement.velocity | Knockback rejection |
| WrongBreakCheck | Wrong Break | windfall.movement.wrongbreak | Spatial consistency + teleport |

### Packet (11)

| Check | Name | Key | Target |
|---|---|---|---|
| BadPacketsCheck | Bad Packets A | windfall.packet.bad | Malformed/duplicate/out-of-bounds packets |
| ChatCheck | Chat A | windfall.packet.chat | Chat flooding (per-minute + burst) |
| ChestStealerCheck | Chest Stealer A | windfall.packet.cheststealer | Automated inventory theft |
| ClientBrandCheck | Client Brand A | windfall.packet.brand | Known cheat client brand strings |
| CrashCheck | Crash A | windfall.packet.crash | Oversized chat + suspicious creative |
| CreativeCheck | Creative A | windfall.packet.creative | Creative packets in non-creative mode |
| ExploitCheck | Exploit A | windfall.packet.exploit | Out-of-range window/slot/entity IDs |
| PacketOrderCheck | Packet Order A | windfall.packet.order | Burst/duplicate/pre-login packets |
| SprintCheck | Sprint A | windfall.packet.sprint | Abnormal sprint toggling |
| TransactionCheck | Transaction A | windfall.packet.transaction | Skipped/fabricated transaction responses |
| VehicleCheck | Vehicle A | windfall.packet.vehicle | Vehicle speed + steering rate |

### Inventory (1)

| Check | Name | Key | Target |
|---|---|---|---|
| InventoryCheck | Inventory A | windfall.inventory.inventory | Click speed burst + per-second cap |

## 6. Common Check Patterns

### @CheckData annotation elements
```java
@interface CheckData {
    String name();                          // human-readable
    String stableKey();                     // maps to config.yml
    double decay() default 0.02;            // buffer decay per tick
    int setbackVl() default 20;            // VL threshold for teleport setback
    int minVersion() default 4;             // minimum protocol version
    int maxVersion() default 99999;         // maximum protocol version
    CompatFlag[] compat() default {};       // compatibility flags
    double relaxMultiplier() default 1.0;   // mismatch tolerance multiplier
    boolean disableOnFolia() default false; // Folia exclusion
    boolean disableOnPurpur() default false;// Purpur exclusion
}
```

### State management pattern
```java
// Every check stores per-player state in ConcurrentHashMap
// Inner class for player-scoped mutable fields:
private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

private static class PlayerState {
    // per-player mutable fields
}

// Accessor pattern:
private PlayerState getState(UUID uuid) {
    return stateMap.computeIfAbsent(uuid, k -> new PlayerState());
}

// Cleanup:
@Override
public void removePlayer(UUID uuid) {
    stateMap.remove(uuid);
}
```

### Flag pipeline (in Check.flag())
1. SeverityManager computes scaled VL increment from player's total VL
2. Merges into player's VL concurrent map (capped at maxVl)
3. Records violation in ViolationPattern
4. Increments Prometheus flag counter
5. Sends alert via AlertManager (or verbose log)
6. Evaluates punishment via PunishmentEngine
7. If VL ≥ setbackVl: resets VL to 0, teleports to last ground/teleport position

### Reward pipeline (per tick)
- Called once per tick for all online players from CheckManager.onTick()
- VL decays by 1 per tick
- Buffer decays by `decay` per tick (from @CheckData)

### Three-layer check filter (CheckManager.registerChecks())
1. **Version range**: `[minVersion, maxVersion]` must contain server protocol
2. **Fork detection**: `disableOnFolia` / `disableOnPurpur`
3. **Plugin detection**: e.g. OldCombatMechanics keeps SwordBlock on legacy

## 7. Version System

### Protocol mapping
```
5      → 1.7.10
47     → 1.8
107    → 1.9
393    → 1.13
477    → 1.14
756    → 1.17
757    → 1.18
763    → 1.20
767    → 1.21
768    → 1.21.2
770    → 1.21.5
800+   → 26.x (formula: 800 + (major-26)*10 + minor)
```

### VersionBracket groups
| Bracket | Protocols | MC Versions |
|---|---|---|
| LEGACY | 4–47 | 1.7–1.8 |
| COMBAT | 107–340 | 1.9–1.12 |
| FLAT | 393–498 | 1.13–1.14 |
| WORLD | 573–758 | 1.15–1.18.2 |
| MODERN | 759–766 | 1.19–1.20.4 |
| LATEST | 767–99999 | 1.21+ |

### ServerFork detection order (priority)
```
Folia (io.papermc.paper.threadedregions.RegionizedServer)
  → Purpur (org.purpurmc.purpur.PurpurConfig)
    → Paper (com.destroystokyo.paper.PaperConfig)
      → Spigot (org.spigotmc.SpigotConfig)
        → Bukkit (default)
```

## 8. CompatFlag enum (13 values)

**Version flags**: VERSION_LEGACY, VERSION_COMBAT, VERSION_FLAT, VERSION_WORLD, VERSION_MODERN, VERSION_LATEST

**Platform flags**: FOLIA_UNSAFE, PURPUR_KB_DEPENDENT, PAPER_CHUNK_DEPENDENT

**Plugin flags**: VIAVERSION_SENSITIVE, GEYSEIR_SENSITIVE

**Mismatch behavior**: DISABLE_ON_MISMATCH, RELAX_ON_MISMATCH

## 9. TPS-Adaptive Threshold System

```
TPS ≥ 19        → multiplier = 1.0
TPS < threshold  → multiplier = 1.0 + (threshold - TPS) * scaleFactor
TPS < safeMode   → multiplier = maxToleranceMultiplier (default 2.0), safe mode ON
```

- Rolling window of 50 TPS samples
- Configurable: threshold (19), scaleFactor (0.02), maxMultiplier (2.0), safeMode (12)

## 10. PlayerProfile Tolerance Multipliers

Used by checks via `getCombinedToleranceMultiplier()` = Bedrock × VersionGap

**Bedrock tolerances**: Touch 1.15x, Controller 1.10x, Keyboard 1.05x, Java 1.0x

**Version gap**: Each bracket of distance adds 5% (e.g., 1.8 client on 1.21 server ≈ 1.20x)

## 11. Compensation Layer

### TransactionManager
- Sends Ping packets (1.17+) or WindowConfirmation (pre-1.17)
- Transaction IDs masked to 15 bits (0x7FFF)
- RTT measured in nanoseconds, stored on WindfallPlayer as `transactionPing`
- Callback system: register callback to fire when specific transaction confirmed

### PingPongManager
- Dual-ping sandwich: pre-change + post-change per tick
- Pre-change ping confirms client saw state up to previous tick
- Post-change ping confirms client saw current tick's changes
- `getEstimatedLatencyMs()` = (preRTT + postRTT) / 4

### LatencyCompensator
- Two parallel systems: timestamp-based (CompensatedWorld updates) + tick-based (SimulationEngine)
- Block changes deferred per-player by their latency
- Queue capped at 1000 entries per player, force-applied after 2000ms
- Tick-based tracking for unconfirmed world change replay

### SimulationEngine
- Multi-scenario simulation (up to 16 scenarios = 2^n where n = unconfirmed changes)
- Each scenario represents one combination of unconfirmed changes seen/not-seen
- Returns best-matching scenario within 0.1 block threshold
- Physics replay: block break (remove ground), block place (create ground), velocity offset, potion modifiers

## 12. Physics Constants (IEEE 754 exact)

```
GRAVITY          = 0.08
AIR_DRAG         = 0.98
WATER_DRAG       = 0.800000011920929  (exact decompiled double)
LAVA_DRAG        = 0.5
GROUND_FRICTION  = 0.91
JUMP_MOMENTUM    = 0.42
WALK_SPEED       = 0.1
SPRINT_MULT      = 1.3
SNEAK_MULT       = 0.3
SWIM_BOOST       = 0.03999999910593033
STEP_HEIGHT      = 0.6
```

## 13. Packet Interception (PacketListener)

### Receiving (client→server)
- `PLAYER_POSITION`: update x,y,z, onGround, movedSinceTick
- `PLAYER_POSITION_AND_ROTATION`: same + yaw/pitch
- `PLAYER_ROTATION`: yaw, pitch, onGround
- `PLAYER_FLYING`: onGround only
- `KEEP_ALIVE`: → transactionManager (long masked to 16-bit short)
- `INTERACT_ENTITY`: record lastAttackTime
- Dispatches to CheckManager and ActionData

### Sending (server→client)
- `LOGIN_SUCCESS`: creates WindfallPlayer, detects version/Geyser
- `ENTITY_VELOCITY`: captures server-sent knockback
- `PLAYER_POSITION_AND_LOOK`: records teleport position
- `RESPAWN`: resets position, clears sprint/sneak, sets respawned flag
- `PING`: sends transaction for RTT measurement
- `PLAYER_ABILITIES`: updates flight state
- Dispatches to CheckManager and ActionData

## 14. Bedrock Support

### Detection hierarchy
1. Floodgate API (preferred) — full device info via reflection
2. GeyserApi (fallback) — Bedrock detection + client version

### BedrockInfo fields
- deviceOs (ANDROID, IOS, XBOX, PS4, SWITCH, etc.)
- inputMode (TOUCH, CONTROLLER, KEYBOARD_MOUSE)
- uiProfile, clientVersion, languageCode

### GeysersTracker
- Scans 5×37×5 area for `POTENT_SULFUR` in ERUPTING/CONTINUOUS states
- Returns 1.5x tolerance multiplier when player near geyser

## 15. Config Structure

### Sections
- `alerts`: enabled, prefix, permission, broadcast
- `discord`: webhook URL, server name, embed colors, rate-limit
- `bedrock`: Geyser/Floodgate config, per-check thresholds, tolerances
- `verbose`: detailed logging
- `severity`: VL thresholds + multipliers (1.0x/1.3x/1.6x/2.0x)
- `punishments`: warn/kick/tempban/permban thresholds + messages
- `compatibility`: auto-adapt, bedrock overrides, folia settings
- `prometheus`: host:port (default 127.0.0.1:9211)
- `checks`: defaults + per-check overrides (enabled, max-vl, setback-vl, punishable)

### Check config fallback pattern
```
checks.<stableKey>.enabled → checks.default.enabled (if missing)
checks.<stableKey>.max-vl  → checks.default.max-vl
same for setback-vl, decay, punishable
```

## 16. Commands

`/windfall` (aliases: `/wf`, `/wfall`) — 12 subcommands:
- `help` — list subcommands
- `reload` — reload config
- `info` — version, player count, check status
- `alerts` — toggle alerts
- `verbose` — verbose logging status
- `setback <player>` — teleport to last ground
- `checks` — list all checks
- `toggle <key>` — enable/disable check
- `version` — version + server info
- `debug <player>` — detailed state dump
- `severity <player>` — severity info
- `gui` — open ChecklistGUI

## 17. Prometheus Metrics (port 9211)

| Metric | Type | Description |
|---|---|---|
| windfall_ticks_per_second | Gauge | Server tick rate |
| windfall_checks_per_second | Gauge | Checks evaluated per tick |
| windfall_active_players | Gauge | Online players |
| windfall_check_buffer_sum | Gauge | Total buffer across all players |
| windfall_adaptive_threshold | Gauge | TPS-aware tolerance multiplier |
| windfall_server_tps | Gauge | Estimated server TPS |
| windfall_flags_total | Counter (labeled by check key) | Cumulative flags |

## 18. ViolationPattern (Repeat Offender Detection)

- Records violations per-player with timestamps to `.properties` files in `plugins/Windfall/violation-history/`
- **Repeat offender**: violations on ≥3 distinct days
- **Toggle pattern**: alternating violation/clean sessions in 6000-tick window
- **Escalation**: second-half VL peaks ≥5 higher than first half (≥6 violations required)
- History files pruned after `historyDays` (configurable)

## 19. PacketFingerprint (5-Vector Scoring, 0–100)

1. **Brand** (0–20): Known cheat vs. safe client databases
2. **Channels** (0–20): Suspicious plugin channels (4×count, capped)
3. **Protocol Extensions** (0–20): Custom packet types (5×count, capped)
4. **Movement Precision** (0–20): Decimal places (vanilla = 4, >6 → score)
5. **Packet Timing** (0–20): CV of intervals (<0.05 → 18 = bot-like)

Min severity to flag: default 60 (configurable)

## 20. Initialization Order (WindfallPlugin.onEnable)

```
1. WindfallConfig
2. VersionManager
3. ServerFork detection
4. PluginDetector
5. PlatformScheduler
6. FoliaCompat / PurpurCompat
7. PlayerManager
8. TransactionManager / PingPongManager / LatencyCompensator / SimulationEngine
9. GeyserManager / GeysersTracker
10. SeverityManager / PunishmentEngine
11. CheckManager (registers 52 checks)
12. CommandManager / AlertManager / ChecklistGUI
13. WorldGuard integration (conditional)
14. Register API, PacketListener, PlayerQuitListener
15. Start tick, bStats, Prometheus
```

## 21. Key Thread Safety Patterns

- `WindfallPlayer` compound states (PositionState, GroundState, RotationState) = immutable + volatile reference → no torn reads
- PlayerManager backed by `ConcurrentHashMap`
- Check state maps = `ConcurrentHashMap<UUID, PlayerState>`
- MaterialUtils caches = `ConcurrentHashMap<Material, Boolean>`
- ActionData tick counters = plain int (updated only from main thread, stale reads from Netty acceptable)
- Folia: all entity operations via `FoliaCompat.runOnEntity()` → region thread

## 22. Test Architecture

- **Framework**: JUnit 5 + Mockito 5
- **Base class**: `CheckTestBase` (Mocks WindfallPlugin, WindfallConfig, SeverityManager, AlertManager, PunishmentEngine; provides `createMockPlayer()`)
- **42 test classes** covering: version mapping, server fork, physics constants, prediction, bounding box, math utils, material utils, compensation (3), simulation engine, pattern detection, severity, adaptive thresholds, Prometheus, plugin detection, fingerprint, player profile, platform compat, all check state isolation patterns
- **Common test pattern for checks**: verify @CheckData annotation, ConcurrentHashMap state map, per-player state isolation (via reflection), per-player buffer isolation

## 23. File Manifest

### All source files (src/main/java/io/windfall/anticheat/)

```
WindfallPlugin.java
WindfallMetrics.java

api/
  WindfallAPI.java, WindfallAPIImpl.java, WindfallProvider.java, PlayerData.java

core/
  adaptive/AdaptiveThreshold.java
  alert/AlertManager.java, DiscordWebhook.java
  bedrock/BedrockInfo.java, GeyserManager.java, GeysersTracker.java
  check/Check.java, CheckData.java, CheckManager.java, CompatFlag.java
  check/type/PacketCheck.java
  check/impl/combat/ (Aim, Autoclicker, Backtrack, Criticals, FastHeal, Hitboxes, KillAura, Macro, MultiInteract, Reach, SelfInteract, SwordBlock)
  check/impl/movement/ (AirLiquidBreak, AirLiquidPlace, Baritone, Elytra, FarBreak, FarPlace, FastBreak, Flight, GroundSpoof, InvalidBreak, InvalidPlace, Motion, MultiBreak, MultiPlace, NoFall, NoSlow, NoSwing, Phase, PositionBreak, PositionPlace, RotationBreak, RotationPlace, Scaffold, Simulation, Speed, Step, Timer, Velocity, WrongBreak)
  check/impl/packet/ (BadPackets, Chat, ChestStealer, ClientBrand, Crash, Creative, Exploit, PacketOrder, Sprint, Transaction, Vehicle)
  check/impl/inventory/InventoryCheck.java
  command/CommandManager.java, ChecklistGUI.java
  compat/WorldGuardCompat.java
  compensation/CompensatedEntities.java, CompensatedWorld.java, LatencyCompensator.java, PingPongManager.java, SimulationEngine.java, TransactionManager.java, WorldChange.java
  config/WindfallConfig.java
  fingerprint/PacketFingerprint.java
  metrics/WindfallPrometheus.java
  network/PacketListener.java
  physics/BoundingBox.java, PhysicsConstants.java, PredictionContext.java, PredictionEngine.java, VersionPhysics.java
  platform/FoliaCompat.java, PurpurCompat.java
  player/WindfallPlayer.java, PlayerManager.java, PlayerProfile.java
  player/data/ActionData.java
  plugin/PluginDetector.java
  punishment/PunishmentEngine.java
  scheduler/PlatformScheduler.java
  severity/SeverityManager.java, ViolationPattern.java
  util/MathUtil.java, MaterialUtils.java
  version/ServerFork.java, VersionBracket.java, VersionManager.java
```

### Resources
- `src/main/resources/plugin.yml` — plugin descriptor
- `src/main/resources/config.yml` — default config

### Test files (src/test/java/io/windfall/anticheat/)
- 42 test classes (see architecture section for full list)

---

## 24. Coding Conventions & Rules

### Naming
| Category | Convention | Examples |
|---|---|---|
| Packages | Reverse-domain, lowercase, dot-separated | `io.windfall.anticheat.core.check.impl.combat` |
| Classes | PascalCase, domain-descriptive | `SpeedCheck`, `PredictionContext`, `SimulationEngine` |
| Check classes | PascalCase + `Check` suffix | `KillAuraCheck`, `BadPacketsCheck` |
| Inner classes | PascalCase | `PlayerState`, `TargetEvent`, `PendingVelocity` |
| Methods | camelCase | `getState()`, `increaseBuffer()`, `handleAttack()` |
| Constants | `UPPER_SNAKE_CASE` + `private static final` | `MAX_TARGETS_PER_SECOND_LEGACY`, `SPEED_TOLERANCE` |
| Variables | camelCase, descriptive | `state`, `player`, `event`, `velX`, `protocol` |
| Boolean getters | `is`/`has` prefix | `isEnabled()`, `hasCompatFlag()` |
| Packet routing helpers | `handle` prefix | `handlePosition()`, `handleClickWindow()` |
| Detection helpers | `check`/`validate` prefix | `checkMultiAura()`, `validateCoordinates()` |

### File structure (top → bottom)
1. Package declaration
2. Import block (grouped by dependency origin, alphabetical within groups)
3. Class Javadoc (overview, algorithm, thresholds via `{@value}`, `@see` references)
4. `@CheckData` annotation
5. Class declaration: `extends Check implements PacketCheck`
6. Constants block (each with Javadoc)
7. Inner `PlayerState` class (`private static final class`)
8. Instance state map: `private final ConcurrentHashMap<UUID, PlayerState>`
9. `getState()` accessor
10. `removePlayer()` override
11. `onPacketReceive()` / `onPacketSend()` overrides
12. Private helper methods (handlers, detection algorithms, utilities)
13. Additional inner classes if needed
14. Public utility/static methods

### Method body ordering
1. Guard clauses: `if (!enabled) return;` / `if (type != X) return;`
2. State retrieval: `PlayerState state = getState(player);`
3. Pruning/cleanup loops before analysis
4. Version/platform branching for threshold selection
5. Detection logic with buffer operations
6. Flag evaluation at end

### Import ordering (separated by blank lines)
```
1. com.github.retrooper.packetevents.*
2. io.windfall.anticheat.WindfallPlugin
3. io.windfall.anticheat.core.check.*
4. io.windfall.anticheat.core.check.type.*
5. io.windfall.anticheat.core.compensation.*
6. io.windfall.anticheat.core.config.*
7. io.windfall.anticheat.core.physics.*
8. io.windfall.anticheat.core.platform.*
9. io.windfall.anticheat.core.player.*
10. io.windfall.anticheat.core.player.data.*
11. io.windfall.anticheat.core.version.*
12. java.util.*
13. java.util.concurrent.*
— blank line —
14. static imports (test files)
```

### Exception handling
- **No checked exceptions** — methods do not declare `throws`
- **No try-catch in check logic** — detection flows via buffer accumulation, not exceptions
- **Guard clauses** replace try-catch: `if (x == null) return;`
- **Reflection/compat failures** caught silently at `FINE` log level
- **Missing `@CheckData`** triggers `IllegalStateException` crash at startup (fail-fast design)
- **Bad data** (NaN, Infinite) triggers kick as a validation step, not an exception

### Thread safety
- Per-player state: `ConcurrentHashMap<UUID, PlayerState>` in every check
- Compound states: immutable snapshots via `volatile` reference swap (no torn reads)
- Simple flags: `volatile` directly
- VL/buffer maps: `ConcurrentHashMap<String, Integer/Double>`
- Sliding window deques: `ArrayDeque` (single-threaded per player, protected by ConcurrentHashMap)
- Folia entity ops: via `FoliaCompat.runOnEntity()` → region thread

### Constants
- Always `private static final` (never `public` or `protected` for check internals)
- Always `UPPER_SNAKE_CASE`
- Each constant has a Javadoc explaining purpose and rationale
- Named for domain concept, not implementation detail: `MIN_CLICKS_FOR_EVAL`, not `TWENTY_CLICKS`

### Inner class conventions
- `PlayerState` in every check: per-player mutable state
- `private static final class` (never non-static)
- Fields are package-private, accessed directly from outer class
- Immutable data classes (`TrackedEntity`, `TargetEvent`, etc.) have `final` fields + constructor

### Parameter ordering
```
(WindfallPlayer player, PacketReceiveEvent event)       — packet handlers
(WindfallPlayer player, PlayerState state)              — delegation methods
(PlayerState state, value, threshold)                   — detection methods
```

---

## 25. Development Workflows

### Adding a new check
1. Create class in `core/check/impl/<category>/` extending `Check implements PacketCheck`
2. Add `@CheckData` annotation with `name`, `stableKey`, `decay`, `setbackVl`, version range, compat flags
3. Add `private static final class PlayerState` for per-player mutable state
4. Add `private final ConcurrentHashMap<UUID, PlayerState> stateMap`
5. Implement `getState()`, `removePlayer()`, `onPacketReceive()`/`onPacketSend()`
6. Register in `CheckManager.registerChecks()` (add to the `registerChecks()` list)
7. Add config section in `WindfallConfig.setDefaults()` for the stableKey
8. Add to `config.yml` defaults under `checks:`
9. Create test class extending `CheckTestBase` in `src/test/java/`
10. Add to `CompatFlagTest` if using new compat flags

### Config reload workflow
```
operator: /windfall reload
  → CommandManager.handleReload()
    → CheckManager.reloadChecks()
      → WindfallConfig.reload()
        → plugin.reloadConfig()           # Bukkit: re-reads config.yml from disk
        → MaterialUtils.clearCaches()
      → for each check:
          check.setEnabled(cfg.isCheckEnabled(check.getStableKey()))
          check.setPunishable(cfg.isCheckPunishable(check.getStableKey()))
```
**Note**: Reload does NOT re-run `registerChecks()` — the set of active checks stays fixed. Only `enabled` and `punishable` toggles are refreshed. `maxVl`, `decay`, `setbackVl` retain construction-time values.

### Punishment escalation chain
```
total VL ≥ permbanVl (30)  → permanent ban   [fires once per player]
total VL ≥ tempbanVl (20)  → temp ban (1d)   [fires once per player]
total VL ≥ kickVl (10)     → kick            [fires once per player]
total VL ≥ warnVl (5)      → warn message    [fires once per player]
```
Tier decays: if VL drops below the tier threshold (via per-tick decay of 1 VL/tick), the tier is un-applied and can fire again on re-escalation.

### Tick loop (50ms interval)
```
1. AdaptiveThreshold.onTick(50ms)           # TPS calculation
2. WindfallPrometheus.tick()                # Metric updates
3. For each online player:
   a. PingPongManager.onTickStart()         # Pre-change ping
   b. player.resetTickState()               # Clear tick-local data
   c. player.getActionData().tick()         # Process action timers
   d. player.updateCachedState()            # Refresh Bukkit API state cache
   e. LatencyCompensator.processDeferredChanges()
   f. For each enabled check:
        check.reward(player)                # VL -1, buffer -decay
        ScaffoldCheck.onTick() / TransactionCheck.onTick()
   g. PunishmentEngine.decayTierIfNeeded()
   h. PingPongManager.onTickEnd()           # Post-change ping
4. Every 200 ticks: ReachCheck.cleanup()
5. Every 6000 ticks: Discord ratelimit cleanup, Fingerprint eviction,
                     ViolationPattern prune, LatencyCompensator prune
```

### Alert → Discord pipeline
```
Check.flag()
  → ViolationPattern.recordViolation()
  → Prometheus.incrementFlags()
  → AlertManager.sendAlert()
    → Rate limit check (per player+check, configurable cooldown)
    → Broadcast to all online staff with "windfall.alerts" permission
    → DiscordWebhook.sendAlert() (async via runAsync())
      → HTTP POST to webhook URL with JSON embed
      → Player head via mc-heads.net
      → @everyone mention if VL ≥ threshold
```

### Bedrock detection at login
```
PacketListener.handleLogin() at LOGIN_SUCCESS:
  → GeyserManager.isBedrockPlayer(uuid)
    → FloodgateApi.isFloodgatePlayer(uuid)   [preferred]
    → GeyserApi.api().isBedrockPlayer(uuid)  [fallback]
  → If bedrock:
      → GeyserManager.getBedrockInfo(uuid)
        → Floodgate player data (device OS, input mode, etc.)
      → PlayerProfile with bedrock tolerance multipliers
```

### Debugging a player
```
/windfall debug <player>
  → Shows: position, ground state, deltas, flying, sprinting, sneaking,
           ping, tick count, protocol version, VL per check, severity,
           bedrock info, buffer per check
/windfall severity <player>
  → Shows: total VL, severity label, multiplier, bedrock discount,
           per-check VL breakdown
```

---

## 26. Design Principles & Architecture Decisions

### Pillars
| Pillar | Principle |
|---|---|
| Latency Compensation | Simulate what the *client* sees, not what the *server* knows |
| Physics Prediction | Validate movement by replaying Minecraft's exact physics engine, not heuristics |
| Multi-Vector Detection | No single indicator is trusted — behavioral, statistical, fingerprint, pattern converge |
| Platform Agnosticism | Compile once, adapt at runtime to any fork, version, or proxy |
| Defense in Depth | Multiple independent checks per category; compensation + simulation + prediction layers |

### Key design decisions

| Component | Why this approach | Tradeoff accepted |
|---|---|---|
| **PingPongManager** (dual-ping) | Single ping can't distinguish pre/post-change. Dual sandwich gives precise `confirmedTick` boundary | 2 extra packets/player/tick (40pps at 20 TPS) |
| **SimulationEngine** (multi-scenario) | Defeats "pulse toggle" bypass by accepting movement if ANY scenario of unconfirmed changes matches | 0.1 block match threshold balances FP/FN; max 16 scenarios |
| **LatencyCompensator** (dual tracking) | Timestamp-based for CompensatedWorld, tick-based for SimulationEngine — separate consumers, separate data structures | Double write overhead for block changes (infrequent) |
| **PredictionEngine** (stateless static) | Pure functions are testable in isolation and safe for concurrent Netty threads | Overloads for swim boost add minor code duplication |
| **PredictionContext** (immutable snapshot) | Eagerly computed once per packet, shared across all checks — consistency + performance | Computes values even if no check uses them |
| **WindfallPlayer** (volatile snapshots) | Lock-free thread safety for compound state — readers always see consistent state | 3-deep position history adds 18 bytes per snapshot |
| **PlayerProfile** (login-time profile) | Centralizes tolerance multipliers — each check doesn't need bedrock/version logic | Version gap heuristic (5%/bracket) could mask cheaters |
| **PacketFingerprint** (5-vector scoring) | Composite scoring provides confidence levels, not binary classification | Brand substring matching is trivially spoofed |
| **ViolationPattern** (properties files) | Zero-dependency persistence, trivially readable by admins | File I/O on main thread; one file per flagged player |
| **SeverityManager** (escalation loop) | Positive feedback: more VL → faster VL accumulation → quicker punishment | Can punish legitimate players with accumulated false positives |
| **FoliaCompat** (reflection adapter) | No compile-time dependency on Folia; degrades gracefully on non-Folia | Reflection API may break with Folia version changes |
| **WorldGuardCompat** (full reflection) | No compile-time dependency on WorldEdit/WorldGuard | 9 reflective calls per query; silent degradation on failure |
| **Check registration** (3-layer filter) | Version range → Fork detection → Plugin detection | Doesn't re-filter on reload (only enabled/punishable toggle) |

### Fault tolerance philosophy
- **Every non-critical operation** is wrapped in try/catch
- **A single check's failure must not crash the pipeline**
- Errors logged at `FINE` level to avoid console spam (not `WARNING`)
- Reflection failures degrade gracefully with fallback values or disabled state
- Guard clauses replace defensive try-catch in detection paths

---

## 27. Check Implementation Guide

### Skeleton
```java
package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(
    name = "Example A",
    stableKey = "windfall.combat.example",
    decay = 0.01,
    setbackVl = 10,
    compat = {CompatFlag.RELAX_ON_MISMATCH},
    relaxMultiplier = 1.2
)
public class ExampleCheck extends Check implements PacketCheck {

    /** Maximum threshold description */
    private static final double MAX_THRESHOLD = 5.0;

    private static final class PlayerState {
        final ArrayDeque<Long> timestamps = new ArrayDeque<>();
        int counter;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!enabled) return;
        // filter packet type
        PlayerState state = getState(player);
        // prune window
        // detection logic
        // buffer += amount; if buffer > threshold → flag(player); resetBuffer(player);
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
        // empty if not needed
    }
}
```

### Must-have checklist
- [ ] `@CheckData` annotation with `name`, `stableKey`, `decay`, `setbackVl`
- [ ] `private static final class PlayerState` for mutable per-player state
- [ ] `private final ConcurrentHashMap<UUID, PlayerState> stateMap`
- [ ] `getState()` accessor with `computeIfAbsent`
- [ ] `removePlayer(UUID)` cleanup
- [ ] `enabled` guard in every packet handler
- [ ] Packet type filter before state retrieval
- [ ] Buffer detection pattern: increase → check threshold → flag → reset (or decay on clean)
- [ ] Javadoc on class and all constants

### State management rules
- **DO**: Use `ConcurrentHashMap<UUID, PlayerState>` for per-player state
- **DO**: Use `ArrayDeque` for sliding window timestamps
- **DO**: Make PlayerState fields package-private, access directly
- **DO**: Call `removePlayer()` on disconnect via CheckManager
- **DON'T**: Use static/singleton state (leaks across players)
- **DON'T**: Use synchronized blocks (ConcurrentHashMap handles it)
- **DON'T**: Store Bukkit `Player` references (use UUID + WindfallPlayer)

### Detection pattern rules
- **DO**: Use `increaseBuffer(player, amount)` / `decreaseBuffer(player, amount)` / `resetBuffer(player)`
- **DO**: Call `flag(player)` or `flagWithSetback(player)` when buffer exceeds threshold
- **DO**: Decay buffer on clean observations
- **DO**: Add version-aware branching via `player.getProtocolVersion()` or `VersionBracket`
- **DO**: Add bedrock-aware branching via `player.isBedrock()` or `player.getProfile().getCombinedToleranceMultiplier()`
- **DON'T**: Call `player.kickPlayer()` directly (use `performSetback()`)
- **DON'T**: Access Bukkit API from Netty threads (use `player.updateCachedState()` or `WindfallPlayer` cached fields)
- **DON'T**: Store mutable state outside of PlayerState inner class

### Version range rules
| Range | Meaning | Example checks |
|---|---|---|
| `min=5, max=107` | Pre-1.9 only (legacy combat) | FastHealCheck, SwordBlockCheck |
| `min=107, max=999` | 1.9+ only (modern combat) | ElytraCheck |
| `min=4, max=99999` | All versions (default) | Most checks |

---

## 28. Common Pitfalls & FAQ

### Pitfalls

| Pitfall | Symptom | Fix |
|---|---|---|
| Bukkit API call from Netty thread | `IllegalStateException` / data race | Use `WindfallPlayer.updateCachedState()` on tick thread; read cached volatile fields from Netty |
| Missing `@CheckData` annotation | `IllegalStateException` at startup | Always annotate check class |
| Static mutable state | State leaks across players / concurrent modification | Use `ConcurrentHashMap<UUID, PlayerState>` |
| Not filtering packet type | Wrong handler fires, NPE on cast | Always check `event.getPacketType()` before processing |
| Forgetting `removePlayer()` | Memory leak on player quit | Override `removePlayer()` to clean `stateMap` |
| Using `if/else` instead of `if/return` for packet routing | Missed packet type handling | Each type check should `return` after routing to avoid fallthrough |
| Not checking `enabled` flag | Check runs when disabled | First line of every packet handler: `if (!enabled) return;` |
| Direct `player.teleport()` for setback | Folia crash / cross-region error | Use `Check.performSetback()` which uses `FoliaCompat.teleportAsync()` |
| Accessing `event.getPlayer()` before LOGIN_SUCCESS | NullPointerException | Only create WindfallPlayer at LOGIN_SUCCESS in PacketListener |
| Assuming default config keys exist | `NullPointerException` on first load | `WindfallConfig.setDefaults()` registers all keys; use fallback pattern |
| Not capping buffer/VL | Buffer or VL overflow | Cap at `maxVl` via `Math.min()`; reset on flag |

### FAQ

**Q: How do I add a new config option for a check?**
A: Add the key in `WindfallConfig.setDefaults()` under the `checks.*` section, add a typed accessor method (e.g., `getCheckMaxVl(String key)`), and read it in the `Check()` constructor.

**Q: How do I send a custom punishment message?**
A: Configure `punishments.warn-message` / `kick-message` / `tempban-reason` / `permban-reason` in `config.yml`.

**Q: How do I exempt a WorldGuard region from anti-cheat?**
A: Call `WorldGuardCompat.isInRegion(player)` in the check and skip detection or widen thresholds. WorldGuard integration is optional and reflection-based.

**Q: How does the plugin handle ViaVersion?**
A: `PluginDetector` detects ViaVersion at startup. `PlayerProfile` computes version gap tolerance. `VersionPhysics` provides version-dependent constants. `@CheckData(minVersion/maxVersion)` filters checks incompatible with the client version.

**Q: Why does reload not re-register checks?**
A: `registerChecks()` with version/fork/plugin filtering is design-time (startup). Reload only toggles `enabled`/`punishable` to avoid disrupting running checks. Restart the server to pick up new checks after code changes.

**Q: How do I test a new check?**
A: Extend `CheckTestBase` which provides Mockito infrastructure. Test `@CheckData` annotation values, per-player state isolation (via reflection), buffer isolation, and detection logic with mock `WindfallPlayer`.

**Q: What does `CompatFlag.RELAX_ON_MISMATCH` do?**
A: When a version or platform mismatch is detected, the check's thresholds are multiplied by `relaxMultiplier` (e.g., 1.3x) to reduce false positives instead of being disabled entirely.

**Q: How does TPS adaptive scaling work?**
A: `AdaptiveThreshold` maintains a 50-tick rolling TPS average. Below 19 TPS, tolerances scale up linearly (default 0.02 per TPS below threshold). Below 12 TPS, safe mode activates and tolerances cap at 2.0x.

**Q: What information goes into a Discord webhook alert?**
A: Player name (with mc-heads.net avatar), check name, VL, server name, ping, device/platform info, position (x/y/z/world), and details string. Embed color varies by VL (yellow < 10, orange 10-24, red ≥ 25).

**Q: What's the difference between `flag()` and `flagWithSetback()`?**
A: `flag()` performs setback only when VL ≥ `setbackVl`. `flagWithSetback()` always performs setback regardless of VL. Use `flagWithSetback()` for critical/inventory exploits.

---

## 29. Debug & Investigation Guide

### Common false positive causes
| Cause | Affected checks | Mitigation |
|---|---|---|
| Server lag (TPS drop) | Timer, Speed, Flight, Reach | AdaptiveThreshold increases tolerances |
| ViaVersion version gap | Reach, KillAura, Speed | PlayerProfile version gap multiplier (5%/bracket) |
| Bedrock touch input | Aim, Reach, Speed, Scaffold | Bedrock tolerance multipliers (1.05x-1.15x) |
| WorldGuard regions | Flight, Speed, Phase | WorldGuardCompat region exemption |
| Purpur custom knockback | Velocity | PurpurCompat reads purpur.yml values |
| Respawn desync (ViaVersion) | All movement | `WindfallPlayer.respawned` flag suppresses setbacks |
| Fast respawn / spawn-tp | GroundSpoof, Flight | `respawned` flag clears after first post-respawn position |
| Elytra + slow falling | Flight, Simulation | PredictionEngine slow-falling gravity override (0.01) |
| Geyser push (26.2+) | Flight, Speed, Motion | GeysersTracker scans for `POTENT_SULFUR` blocks within 5×37×5 |
| Piston movement | Speed, Flight, Simulation | ActionData.hasRecentPistonUpdate() exemption |
| Block place under player | Flight, GroundSpoof | ActionData.hasRecentConfirmedUnderPlace() exemption |

### Investigation commands
```
/windfall debug <player>    — Full state dump (VLs, buffers, position, protocol, bedrock)
/windfall severity <player> — Severity tier and breakdown per check
/windfall checks            — List all checks with enabled/punishable status
/windfall info              — Overview: version, player count, check counts, geyser status
/windfall toggle <key>      — Disable a suspected false-flagging check
```

### Log patterns
```
[Windfall] player flagged for Speed A (VL: 5)
[Windfall] player flagged for Fly A (VL: 12) (SETBACK)
[Windfall] Punishment: Banned player (TEMP BAN)
```
- `(SETBACK)` suffix = flagWithSetback() was used (critical violation)
- Punishment messages at INFO level
- Check exceptions at FINE level (NOT WARNING — intentional)

### Config tuning for false positives
```yaml
# Increase tolerance for specific checks
checks:
  windfall.movement.speed:
    max-vl: 150        # Allow higher VL before punishment
    setback-vl: 30     # Higher setback threshold
  windfall.combat.reach:
    max-vl: 120
    setback-vl: 15

# Widen adaptive scaling
adaptive:
  tps-threshold: 18    # Start scaling at 18 TPS instead of 19
  scale-factor: 0.03   # Faster tolerance increase under lag

# Increase bedrock tolerance
bedrock:
  bedrock-tolerance: 1.20  # 20% extra tolerance instead of 10%
```

---

## 30. Configuration Reference

### Section map
| Section | Purpose | Key fields |
|---|---|---|
| `alerts` | In-game alert broadcast | enabled, prefix, staff-permission |
| `discord` | Discord webhook integration | enabled, webhook-url, rate-limit-ms, embed colors |
| `bedrock` | Bedrock player thresholds | enabled, per-check multipliers, tolerances |
| `verbose` | Detailed logging | enabled (boolean) |
| `severity` | VL escalation multipliers | enabled, per-tier VL thresholds + multipliers, bedrock-discount |
| `punishments` | Global punishment tiers | enabled, per-tier VL thresholds, messages, durations |
| `compatibility` | Fork/proxy adaptation | auto-adapt, bedrock overrides, folia packet-delay-tolerance |
| `prometheus` | Metrics endpoint | enabled, host, port (default 9211) |
| `checks` | Per-check settings | defaults section + per-stableKey overrides |

### Per-check fallback
```yaml
checks:
  default:
    enabled: true
    max-vl: 100
    setback-vl: 20
    punishable: true

  # Override only what differs:
  windfall.movement.velocity:
    max-vl: 150        # Override: higher cap
    setback-vl: 30     # Override: higher threshold
    # enabled, punishable, decay → inherit from default
```

### Config reload behavior
- Reload (`/windfall reload`) re-reads config.yml from disk
- Only `enabled` and `punishable` per-check toggles refresh
- `maxVl`, `setbackVl`, `decay` retain construction-time values
- MaterialUtils caches are cleared on reload
- New checks added to code require server restart (reload doesn't re-register)

---

## 31. File Layout Reference

### All check source locations
| Category | Package | File count |
|---|---|---|
| Combat | `core/check/impl/combat/` | 12 |
| Movement | `core/check/impl/movement/` | 30 |
| Packet | `core/check/impl/packet/` | 11 |
| Inventory | `core/check/impl/inventory/` | 1 |

### Key configuration files
| File | Location | Purpose |
|---|---|---|
| `config.yml` | `src/main/resources/config.yml` | Default config shipped with plugin |
| `plugin.yml` | `src/main/resources/plugin.yml` | Bukkit plugin descriptor |
| `pom.xml` | Project root | Maven build configuration |
| `proguard.pro` | Project root | ProGuard obfuscation rules |
| `dependency-check-suppressions.xml` | Project root | OWASP CVE false-positive suppressions |

### Build outputs
| File | Command | Description |
|---|---|---|
| `target/Windfall.jar` | `mvn clean package` | Standard build (bStats + Prometheus shaded) |
| `target/Windfall-obfuscated.jar` | `mvn clean package -Pobfuscate` | ProGuard obfuscated build |
| `plugins/Windfall/config.yml` | Server first run | Generated runtime config |
| `plugins/Windfall/violation-history/*.properties` | Runtime | Per-player violation history files |
