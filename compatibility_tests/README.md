# Windfall Compatibility Ports

Standalone, self-contained ports of features that were identified during the **Feature Audit &
Compatibility Porting** pass as missing or incompatible in Windfall, extracted from the three
anti-cheats credited in the Windfall README `Credits` section:

| Source repo | Cloned at HEAD | Ported here |
|---|---|---|
| [ArrowAntiCheat](https://github.com/StelGR/ArrowAntiCheat) | `810d8f2` (2026-10-05) | TrustFactor, OmniSprint, StatisticalMetrics |
| [GrimAC](https://github.com/GrimAnticheat/Grim) | `abb95b6` (2026-10-01) | GhostBlockResolver, ExplosionQueue |
| [TruthfulAC](https://github.com/TawnyE/TruthfulAC) | `17214d2` (2026-10-04) | FireworkBoostModel, CrystalAttackWindow |

This module does **not** modify any Windfall core source file and is **not** part of the parent Maven
build. Build and test it on its own:

```
mvn -B -o -f compatibility_tests/pom.xml test
```

Run the full parent suite to confirm Windfall core is untouched:

```
mvn -B -o test -q
```

## Layout

```
compatibility_tests/
├── pom.xml                 standalone module (JUnit 5 only, no Windfall/Bukkit/PacketEvents deps)
├── README.md               this file
├── audit_report.md         the full comparative feature audit (deliverable)
└── src/
    ├── main/java/io/windfall/anticheat/compat/
    │   ├── trust/TrustFactorModel.java        Arrow TrustFactor (dynamic trust -> required buffer)
    │   ├── sprint/DirectionalMovement.java    Arrow MovementPredictionUtil (direction math)
    │   ├── sprint/OmniSprintModel.java        Arrow OmniSprintA (directional sprint detection)
    │   ├── stats/StatisticalMetrics.java      Arrow autoclicker Stats (entropy/kurtosis/skewness/...)
    │   ├── elytra/FireworkBoostModel.java     TruthfulAC FireworkBoostTracker (physics-verified boosts)
    │   ├── combat/CrystalAttackWindow.java    TruthfulAC CrystalAuraA (place->break + ID-predict)
    │   ├── world/GhostBlockResolver.java      Grim GhostBlockMitigation (resync on ghost-block build)
    │   └── velocity/ExplosionQueue.java       Grim ExplosionHandler (first-bread explosion queue)
    └── test/java/io/windfall/anticheat/compat/
        └── (matching parity tests per module)
```

## Parity notes

- Ports are pure JDK logic. No Bukkit/PacketEvents/Geyser APIs, so the module compiles and tests
  offline with no server dependencies.
- Where a reference implementation is coupled to its host framework, the coupling was injected as a
  small interface or plain method parameters. Every divergence from the source behaviour is listed in
  `audit_report.md` § 7 ("Compatibility notes").
- Notable reference quirks are preserved deliberately so parity tests prove the port matches the
  source:
  - `StatisticalMetrics.modeFrequency(...)` mirrors Arrow's `Mode`, which returns the maximum
    frequency, not the mode value. Arrow's `Mode` is effectively a bug; Windfall integration should
    return the most frequent value instead.
  - `OmniSprintModel` mirrors Arrow's active decision path. Arrow's `AIR_HORIZONTAL_FRICTION`
    comment describes an input estimate the current code does not compute (the commented-out
    `airInput*` variables); the constant is kept for reference.
- TruthfulAC caveat: the cloned `17214d2` HEAD registers AimA-AimX in `CheckRegistry` but the
  `checks/impl/combat/aim/` package is empty, so the reference would not compile. No aim logic was
  ported from it.

## Windfall mapping (integration candidate wiring, not applied here)

| Module | Windfall consumer |
|---|---|
| `TrustFactorModel` | Scale per-check buffer thresholds / setbacks by player trust instead of static values |
| `StatisticalMetrics` | `AutoclickerCheck` interval-buffer : add entropy/kurtosis/skewness signals |
| `OmniSprintModel` | `SprintCheck` : make it direction-aware on ground + air |
| `FireworkBoostModel` | `ElytraCheck` / `SimulationCheck` : exempt legitimate glowstone-boosted elytra |
| `CrystalAttackWindow` | new check for crystal / respawn-anchor PvP (currently absent) |
| `GhostBlockResolver` | `GroundSpoofCheck` / `PhaseCheck` : defer ground probes until client block state resolves |
| `ExplosionQueue` | `VelocityCheck` / `TransactionCheck` : include explosion knockback in expected velocity |