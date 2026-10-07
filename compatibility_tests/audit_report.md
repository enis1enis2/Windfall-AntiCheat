# Windfall — Feature Audit & Compatibility Porting Report

## 1. Executive summary

This pass audited the three anti-cheat projects credited in the Windfall README `Credits` section —
**ArrowAntiCheat**, **GrimAC** and **TruthfulAC** — against Windfall `2.4.3` (MC 1.7–26.2+, Java 11,
PacketEvents `2.13.0`). The goal was to (a) produce a comparative feature report, (b) identify
missing or incompatible features worth borrowing, and (c) land those features as pure-JDK, parity-tested
ports in a standalone module so **no Windfall core source file is modified**.

Deliverables:

- `compatibility_tests/*` — standalone build with **8 port modules and 59 parity tests** (all green,
  offline, JUnit 5 only).
- This report — inventory, feature matrix, gap analysis, port documentation, dependency refresh,
  integration recommendations.

**Headline numbers** (Windfall target):

| Metric | Value |
|---|---|
| Total registered checks | 60 |
| — Combat | 14 |
| — Movement | 34 |
| — Packet | 11 |
| — Inventory | 1 |
| README claim | "52 checks" — resolved in `V:2.4.4`: README reports **60** (see § 5, § 12) |
| Parent test suite | 722 (`V:2.4.4`, master) |
| Repo shape (repowise) | 176 files / 34,066 lines / 2 import cycles / 256 open findings |
| Worst health performer | `InvalidPlaceCheck` (2.85) |

## 2. Audit environment

| Item | Value |
|---|---|
| Target repo | Windfall `2.4.3` (groupId `io.windfall`) |
| Build | Maven, Java 11, PacketEvents `2.13.0` (`provided`) |
| Reference — Arrow | clone @ `810d8f2` (2026-10-05) |
| Reference — Grim | clone @ `abb95b6` (2026-10-01) |
| Reference — TruthfulAC | clone @ `17214d2` (2026-10-04) |
| Live server | **unavailable** — gameplay behaviour is verified via mirrored math + unit tests, not live flags |

## 3. Reference repo inventories

### 3.1 ArrowAntiCheat — ~85 check classes plus a statistics suite

- **combat**: `aimassist/` AimA–AimL + AimH2 (13) with `LinearRegression`, `RotationFrame`, `Vector3dm`
  helpers; `autoclicker/` AutoClickerA–K + B2 (13), MacroA/B; `backtrack/` A/B; `hitbox/` HitboxA;
  `killaura/` A/B; `reach/` ReachA; `velocity/` A/B.
- **movement**: `badpackets/` A–F; `interact/` A–E; `inventory/` A; `scaffold/` A–D; `timer/` A–C;
  `vehicle/` A–C + VehicleSupport; `fly/` ElytraA/B, FlyA/B, GravityA–D; `ground/` A–C;
  `illegalmove/` A–C; `motion/` A–F; `speed/` NoSlowdown, OmniSprintA, SpeedA/B + SpeedMath;
  `simulation/` Combat, Movement, Network, World; `MovementPredictionUtil`.
- **support**: `Stats` + entropy/kurtosis/skewness/stddev/variance/mean/min/max/range/midpoint/sum/mode,
  `TrustFactor` (dynamic per-player trust → dynamic required buffer).

### 3.2 GrimAC — feature-complete 2.0 open-source branch (MC 1.8–26.3)

Aim, badPackets (A–Z), baritone, breaking, chat, combat, crash, elytra (armor/firework), exploit (info/plr)
, flight, groundspoof (`noFall`), misc (**GhostBlockMitigation**, **Post**, **TransactionOrder**),
movement (`PredictionRunner`, **SetbackBlocker**, block reach check), multiactions, packetorder, sprint,
timer, vehicle, velocity, verbose.

Notable design: **configuration-driven severity** (each check bundles an `alert`/`ban` severity), full
**Geyser player exemption** for cross-edition play, transaction-first **explosion knockback queue**
(`ExplosionHandler`).

### 3.3 TruthfulAC — "v0.9 … not in a good area, false flags a lot, not recommended" (owner's own README)

73 classes: `bedrock/` BFlyA, BReachA, BSpeedA; `combat/` anchor AnchorAuraA, autoclicker A–E,
**crystal CrystalAuraA**, hitbox HitboxA, killaura B–H, lag A–C, reach ReachA; `movement/` baritone A–C,
inventory A, simulation A–F, groundspoof B–G, velocity A–D; `packet/` badpacket A/C/D/E/G/H/I/J/K,
crasher A, invalid A, packetorder A–E, sprint A/B, timer A; `raycast/` RaycastA; `world/` fastbreak A,
phase A, scaffold A–H + ScaffoldSupport.

Standout ideas worth noticing despite overall quality: **FireworkBoostTracker** (physics-verified
firework boosts for elytra), **CrystalAuraA** (place→break timing + 0-tick ID-predict), **BanwaveManager**,
`SensitivityUtil`, `CompensationTracker` (delayed AC compensation), and `aim/` … which is notable because it
is **empty while `CheckRegistry` registers AimA–AimL + AimX**: the checked-out HEAD would not compile.

## 4. Feature matrix

Coverage key: ✅ built-in · 🟡 partial / single config · ❌ absent · ✅ ported here (in `compatibility_tests`)

| Detection category | Windfall | Arrow | Grim | TruthfulAC | Port |
|---|---|---|---|---|---|
| Aim assist / rotation | AimCheck | AimA–L (13) | Aim | ❌ (aim/ empty) | – |
| Autoclicker | AutoclickerCheck | AutoClickerA–K + Stats | ❌ | A–E | ✅ StatisticalMetrics |
| Backtrack | BacktrackCheck | A/B | ✅ | ❌ | – |
| Hitbox / reach | HitboxesCheck, ReachCheck | HitboxA, ReachA | ✅ | HitboxA, ReachA | – |
| Kill aura | KillAuraCheck | A/B | ✅ | B–H | – |
| Velocity | VelocityCheck | A/B | ✅ (ExplosionHandler) | A–D | ✅ ExplosionQueue |
| Bad packets / NMS | BadPacketsCheck | A–F | A–Z | badpacket + Crasher | – |
| Sprint (directional) | SprintCheck | **OmniSprintA** | ✅ | A/B | ✅ OmniSprintModel |
| Timer | TimerCheck | A–C | ✅ | A | – |
| Scaffold | ScaffoldCheck (usage/SD) | A–D | ✅ | A–H + Support | – |
| Phase / clip | PhaseCheck | ❌ | ✅ | A | – |
| NoFall / ground spoof | NoFallCheck, GroundSpoofCheck | ground A–C | groundspoof | B–G | – |
| Speed | SpeedCheck, IllegalMoveCheck | SpeedA/B + Math | ✅ | ✅ | – |
| Server-side simulation | SimulationCheck | Simulation(M/N/W/C) | PredictionRunner | simulation A–F | – |
| Elytra / flight | ElytraCheck, FlightCheck, GravityCheck | ElytraA/B, Fly, Gravity | ✅ | BFlyA | ✅ FireworkBoostModel |
| Baritone | BaritoneCheck | ❌ | ✅ | A–C | – |
| Crystal / anchor PvP | ❌ | ❌ | ✅ | CrystalAuraA | ✅ CrystalAttackWindow |
| Ghost-block handling | ❌ (PhaseCheck/GroundSpoof assume client state) | ❌ | GhostBlockMitigation | ❌ | ✅ GhostBlockResolver |
| Reputation / trust | ❌ | TrustFactor → dynamic buffer | ❌ | ❌ | ✅ TrustFactorModel |
| Ban wave | ❌ | ❌ | ❌ | BanwaveManager | 🟡 recommended, not ported |
| First-bread transaction queue | TransactionCheck only | ❌ | ExplosionHandler | ❌ | ✅ ExplosionQueue |

## 5. Gap analysis (priority order)

`§` marks the gaps resolved by this pass.

| # | Gap | Where missing | Impact | Priority |
|---|---|---|---|---|
| `§` GAP-1 | **No trust/reputation layer.** All players share the same buffer/setback thresholds, so repeat offenders spend more server time than needed while trusted builders get false-flagged equally. | Arrow `TrustFactor` | Tolerance tuning | HIGH |
| `§` GAP-2 | **Sprint check is not direction-aware.** Sprinting sideways/backwards is not detected; legit turning hard-counters based purely on yaw deltas, causing either FPs or evasion. | Arrow `OmniSprintA` + `MovementPredictionUtil` | Common cheat | HIGH |
| `§` GAP-3 | **Autoclicker has no statistical layer.** Interval heuristics alone miss click distributions that mimic humans; entropy/kurtosis/skewness discriminate scripted patterns. | Arrow `Stats` suite | Common cheat | MEDIUM |
| `§` GAP-4 | **Elytra false positives on firework boosts.** A legit glowstone boost reads as a huge un-accounted horizontal delta; no physics-pattern verification. | TruthfulAC `FireworkBoostTracker` | FP source | HIGH |
| `§` GAP-5 | **No crystal / respawn-anchor PvP check.** Windfall has no fast place→break or 0-tick ID-predict detection, a core exploit for this metas era. | TruthfulAC `CrystalAuraA` | Common cheat | HIGH |
| `§` GAP-6 | **Ghost blocks are not resolved.** After 1.8+ bridging/breaking, client block state diverges; ground/phase/speed checks assume the client's ghost state and can FP. | Grim `GhostBlockMitigation` | FP source | MEDIUM |
| `§` GAP-7 | **Explosion knockback is not in expected velocity.** The velocity check cannot tell client-side "no knockback" from missing first-bread explosion transactions. | Grim `ExplosionHandler` | Common cheat | HIGH |
| GAP-8 | No ban-wave player queuing for mass-exemption / mass-ban sweeps. | TruthfulAC `BanwaveManager` | Ops tooling | LOW |
| ~~GAP-9~~ | **README claimed 52 checks; reality was 58**, then 60 after `V:2.4.4` — README/`CONTRIBUTING`/`Memory_Bank` counts and tables updated to the real 60 (14 combat / 34 movement / 11 packet / 1 inventory). | Windfall README | Doc | PATCH — **RESOLVED** |
| GAP-10 | Dependency / doc refresh: PacketEvents `2.13.0` pinned (docs last updated 2026-09-23); verify Geyser-exemption scope vs. Grim (Grim exempts Geyser fully; Windfall must decide parity for cross-edition play). | Windfall deps/docs | Doc | PATCH |

## 6. Ported modules (in this pass)

All pure JDK 11, no Bukkit/PacketEvents/Geyser runtime deps, so the standalone module builds offline.
Each is covered by a parity test in `src/test/...` asserting the exact source behaviour.

| Module | Source | Purpose | Windfall integration candidate |
|---|---|---|---|
| `trust.TrustFactorModel` | Arrow `TrustFactor` | Dynamic trust (−100..100) lowering/buffering required flags | Scale check buffers/setbacks per player |
| `sprint.DirectionalMovement` | Arrow `MovementPredictionUtil` | Forward/back/sideway classification, sector math, wrapped-angle diff | Any head-direction-sensitive check |
| `sprint.OmniSprintModel` | Arrow `OmniSprintA` | Direction-aware ground+air sprint validation with turn grace + environment exemptions | `SprintCheck` |
| `stats.StatisticalMetrics` | Arrow `Stats` + 12 metric classes | entropy/kurtosis/skewness/stddev/variance/… | `AutoclickerCheck` interval buffer |
| `elytra.FireworkBoostModel` | TruthfulAC `FireworkBoostTracker` | Physics-verified firework boost (1.15–1.25×, 10/20/30 tick durations, 10-tick grace) | `ElytraCheck` exemption |
| `combat.CrystalAttackWindow` | TruthfulAC `CrystalAuraA` | Fast place→break (<40 ms) and 0-tick ID-predict scoring | New crystal check |
| `world.GhostBlockResolver` | Grim `GhostBlockMitigation` | Post-place cube scan; resync if the neighbourhood is all air | Ground/phase/place checks |
| `velocity.ExplosionQueue` | Grim `ExplosionHandler` | First-bread transaction→explosion queue with merge/poll/`justTesting` semantics | `VelocityCheck` / `TransactionCheck` |

### 6.1 Behavioural-fidelity notes (divergences are deliberate + documented)

- **`StatisticalMetrics.modeFrequency` returns the maximum *frequency*, not the mode value** — exactly what
  Arrow's `Mode` class does. That is effectively a reference bug; the parity test pins it, and integration
  should compute the actual mode instead.
- **`OmniSprintModel` keeps Arrow's constants even where unused** (`AIR_HORIZONTAL_FRICTION` documents an
  input estimate the current code does not compute; the commented-out `airInput*` maths stay out).
- **`CrystalAttackWindow` floors the buffer at `0`** — Windfall convention. TruthfulAC's `CheckBuffer`
  semantics are not fully verifiable from source, so exact lower-bound behaviour is approximated and
  documented. Time is injected as method parameters (`System.currentTimeMillis()` removed) for testability.
- **System clock, per-player maps, and Bukkit/Grim state were knocked out** of all ports (single-instance,
  clock-injected, `WorldView` interface). `ExplosionQueue.Vec3` copies defensively so callers cannot alias
  internal state.
- **`ExplosionQueue.handlePredictionAnalysis` takes the min offset** against a first-call baseline
  (0 = "unset"), mirroring Grim's min-accumulation while staying garbage-free at rest.

## 7. Reference quirks to review before integration

1. **TruthfulAC `aim/` is empty while registered (AimA–AimL, AimX)** → that HEAD cannot compile. Do not
   port anything from it; treat any TruthfulAC aim logic as nonexistent.
2. **Arrow `Mode`** returns a frequency, not a value (see § 6.1).
3. **Grim `GhostBlockMitigation` default `allow: true`** means the scanner is disabled until explicitly
   turned on; `distance` outside `[2, 4]` resets to `2`. If Windfall adopts it, ship it `allow: false`
   (enabled) or gate behind the world-rule config.
4. **Grim fully exempts Geyser players.** Decide whether Windfall wants the same cross-edition stance.

## 8. Dependency & documentation refresh (GAP-10)

| Item | Current | Latest checked | Action |
|---|---|---|---|
| PacketEvents | `2.13.0` (provided) | docs.packetevents.com (2026-09-23) | Keep pinned; re-verify API on upgrade |
| MC support | 1.7–26.2+ | Grim: 1.8–26.3 | Parity acceptable; document Geyser scope |
| check count | README "52" | 60 actual | **DONE** — README/`CONTRIBUTING`/`Memory_Bank` updated; tables list all checks |
| Repowise health | 256 open findings | `InvalidPlaceCheck` worst (2.85) | Fold into existing follow-up queue |

## 9. Integration recommendations (not applied here)

1. **`TrustFactorModel`** — give check config a `required-buffer` modifier that scales by rank
   (`SUPER_UNTRUSTWORTHY…LEGIT`) before enforcing the static threshold.
2. **`OmniSprintModel`** — replace/augment `SprintCheck` state with the `Context` producer feeding
   `handle(ctx)` per tick; bind environment flags to Windfall's world-rule probes (water/webs/climb/ice/
   slime/soul-sand/honey/boat/wall/ghost-block).
3. **`StatisticalMetrics`** — feed the autoclicker interval buffer; flag on `kurtosis`/`entropy` windows
   rather than raw CPS.
4. **`FireworkBoostModel`** — exempt elytra while `isInBoostOrGrace()`, and use `isFakeFirework()` to raise
   the elytra buffer on suspicious claims.
5. **`CrystalAttackWindow`** — new check wired to packet-level crystal throw/interact/attack,
   place→break diff via packet timestamps (injected clock), ID-predict via weak-NBT-free entity tracking.
6. **`GhostBlockResolver`** — on `shouldResync(...)` true after a block place, emit a chunk-resync; defer
   ground/phase validation for `POST_BOOST_GRACE` ticks.
7. **`ExplosionQueue`** — feed explosion velocity into `VelocityCheck` expected velocity aligned with
   transaction IDs; use `justTesting` probe before applying setbacks.

## 10. Build & verification

```
mvn -B -o -f compatibility_tests/pom.xml test        # 59/59 green, offline
mvn -B -o test -q                                    # parent suite 722/722 green
```

The standalone module is **not** declared in the parent `pom.xml`; the parent build remains
byte-for-byte unchanged in module shape (`compatibility_tests/` only). The `V:2.4.4` core integration
adds **19 tests** to the parent suite (703 → 722): `CheckTrustTest` (10), `CrystalCheckTest` (5),
`OmniSprintCheckTest` (4).

## 11. Integration into core (`V:2.4.4`)

All three `§`-flagged build/test milestones green before the integration commit:

| § 9 item | Outcome in `V:2.4.4` |
|---|---|
| 1. TrustFactorModel → buffer scaling | `TrustFactorModel` on `WindfallPlayer`; `Check.trustAdjustedThreshold(player, base)` scales thresholds by rank (0.5 / 0.75 / 1.0 / 1.0 / 1.1 / 1.3); `flag()`/`flagWithSetback()` decrease trust, `reward()` restores it. |
| 2. OmniSprintModel | New **`OmniSprintCheck`** (`windfall.movement.sprint`, "Sprint B") — a separate movement check. Existing `SprintCheck` (packet, `windfall.packet.sprint`) intentionally untouched. |
| 3. StatisticalMetrics → autoclicker | `StatisticalMetrics.entropy()` feeds `AutoclickerCheck`'s interval buffer (entropy < 1.0 → +1.8 else +1.5); `intervalsOf` helper added. |
| 4. FireworkBoostModel → elytra | `ElytraCheck` exempts `isInBoostOrGrace()`, raises buffer via `isFakeFirework()` (+0.4), trust-adjusts speed/ascent thresholds, and tracks `SPAWN_ENTITY` firework rockets. |
| 5. CrystalAttackWindow | New **`CrystalCheck`** (`windfall.combat.crystal`, "Crystal A") — END_CRYSTAL spawn/place timing, fast place→break (<40 ms), and 0-tick ID-predict via the ported window model. |
| 6. GhostBlockResolver | Wired into `GroundSpoofCheck` + `PhaseCheck` (per-check resolver, 10-tick grace, Folia-guarded chunk view, `PAPER_CHUNK_DEPENDENT`). |
| 7. ExplosionQueue → velocity | `VelocityCheck` captures explosion packets, suppresses while `getPendingCount() > 0`, uses `getPossibleExplosions(txId, true)` and adds +0.25 buffer on matched knockback. |

`V:2.4.4` commit: README/audit-report check counts → 60; config keys
`windfall.movement.sprint` + `windfall.combat.crystal`; all ported models + new checks default-on
(player-tolerance verified solely by unit tests — live server unavailable, per § 2).

## 12. References

| Project | URL | Commit |
|---|---|---|
| ArrowAntiCheat | https://github.com/StelGR/ArrowAntiCheat | `810d8f2` |
| GrimAC | https://github.com/GrimAnticheat/Grim | `abb95b6` |
| TruthfulAC | https://github.com/TawnyE/TruthfulAC | `17214d2` |
| PacketEvents docs | https://docs.packetevents.com/ | 2026-09-23 |
| Parent builder | `pom.xml` (Java 11, junit-jupiter `6.1.2`, surefire `3.5.6`) | — |