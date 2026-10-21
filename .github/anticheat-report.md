# Windfall Anti-Cheat Monitor Report

**Generated:** 2026-10-21 02:24:36 UTC

---

## Windfall Current Checks

**Total: 60 checks**

### Combat
- `windfall.combat.swordblock` — Sword Block A
- `windfall.combat.macro` — Macro A
- `windfall.combat.aim` — Aim A
- `windfall.combat.interactcursor` — Interact Cursor
- `windfall.combat.fastheal` — Fast Heal A
- `windfall.combat.killaura` — Kill Aura A
- `windfall.combat.selfinteract` — Self Interact A
- `windfall.combat.hitboxes` — Hitboxes A
- `windfall.combat.autoclicker` — Autoclicker A
- `windfall.combat.reach` — Reach A
- `windfall.combat.crystal` — Crystal A
- `windfall.combat.backtrack` — Backtrack A
- `windfall.combat.criticals` — Criticals A
- `windfall.combat.multiinteract` — Multi Interact A

### Movement
- `windfall.movement.simulation` — Simulation A
- `windfall.movement.fly` — Fly A
- `windfall.movement.fastbreak` — Fast Break A
- `windfall.movement.airliquidplace` — Air Liquid Place
- `windfall.movement.rotationplace` — Rotation Place
- `windfall.movement.sprint` — Sprint B
- `windfall.movement.multiplace` — Multi Place
- `windfall.movement.illegalmove` — Illegal Move
- `windfall.movement.noslow` — NoSlow A
- `windfall.movement.rotationbreak` — Rotation Break A
- `windfall.movement.noswing` — No Swing A
- `windfall.movement.velocity` — Velocity A
- `windfall.movement.step` — Step A
- `windfall.movement.positionbreak` — Position Break
- `windfall.movement.multibreak` — Multi Break
- `windfall.movement.groundspoof` — Ground Spoof A
- `windfall.movement.scaffoldsupport` — Scaffold Support
- `windfall.movement.farplace` — Far Place A
- `windfall.movement.airliquidbreak` — Air Liquid Break
- `windfall.movement.positionplace` — Position Place
- `windfall.movement.baritone` — Baritone A
- `windfall.movement.phase` — Phase A
- `windfall.movement.gravity` — Gravity
- `windfall.movement.timer` — Timer A
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.invalidbreak` — Invalid Break A
- `windfall.movement.speed` — Speed A
- `windfall.movement.invalidplacecursor` — Invalid Place Cursor
- `windfall.movement.elytra` — Elytra A
- `windfall.movement.scaffold` — Scaffold A
- `windfall.movement.invalidplace` — Invalid Place A
- `windfall.movement.motion` — Motion A
- `windfall.movement.farbreak` — Far Break A
- `windfall.movement.wrongbreak` — Wrong Break

### Packet
- `windfall.packet.vehicle` — Vehicle A
- `windfall.packet.exploit` — Exploit A
- `windfall.packet.brand` — Client Brand A
- `windfall.packet.creative` — Creative A
- `windfall.packet.sprint` — Sprint A
- `windfall.packet.order` — Packet Order A
- `windfall.packet.bad` — Bad Packets A
- `windfall.packet.transaction` — Transaction A
- `windfall.packet.cheststealer` — Chest Stealer A
- `windfall.packet.chat` — Chat A
- `windfall.packet.crash` — Crash A

---

## Competitor Analysis

### Grim

**Missing from Windfall (1 checks):**

- `movement` **InvalidPlaceFace** → `windfall.movement.invalid place face`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceFace.java`

**Matched with existing Windfall checks:**

- `NoFall` → `NoFall A`
- `PacketOrderH` → `Packet Order A`
- `PacketOrderN` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderI` → `Packet Order A`
- `PacketOrderM` → `Packet Order A`
- `PacketOrderJ` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderF` → `Packet Order A`
- `PacketOrderO` → `Packet Order A`
- `PacketOrderG` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderP` → `Packet Order A`
- `PacketOrderL` → `Packet Order A`
- `PacketOrderK` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `ClientBrand` → `Client Brand A`
- `Post` → `No Swing A`
- `TickTimer` → `Timer A`
- `Timer` → `Timer A`
- `TimerLimit` → `Timer A`
- `NegativeTimer` → `Timer A`
- `VehicleTimer` → `Timer A`
- `SprintE` → `Sprint B`
- `SprintF` → `Sprint B`
- `SprintG` → `Sprint B`
- `SprintC` → `Sprint B`
- `SprintA` → `Sprint A`
- `SprintD` → `Sprint B`
- `SprintB` → `Sprint B`
- `DuplicateRotPlace` → `Rotation Place`
- `FabricatedPlace` → `Invalid Place Cursor`
- `PositionPlace` → `Position Place`
- `MultiPlace` → `Multi Place`
- `AirLiquidPlace` → `Air Liquid Place`
- `FarPlace` → `Far Place A`
- `InvalidPlaceCursor` → `Invalid Place Cursor`
- `RotationPlace` → `Rotation Place`
- `GroundSpoof` → `Ground Spoof A`
- `Phase` → `Phase A`
- `Baritone` → `Baritone A`
- `ExploitB` → `Exploit A`
- `ExploitA` → `Exploit A`
- `FlightA` → `Fly A`
- `NoSlow` → `NoSlow A`
- `MultiInteractB` → `Multi Interact A`
- `Reach` → `Reach A`
- `SelfInteract` → `Self Interact A`
- `InvalidInteractCursor` → `Interact Cursor`
- `MultiInteractA` → `Multi Interact A`
- `AimDuplicateLook` → `Aim A`
- `AimModulo360` → `Aim A`
- `VehicleE` → `Vehicle A`
- `VehicleF` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `VehicleD` → `Vehicle A`
- `WrongBreak` → `Wrong Break`
- `NoSwingBreak` → `No Swing A`
- `PositionBreakB` → `Position Break`
- `InvalidBreak` → `Invalid Break A`
- `FarBreak` → `Far Break A`
- `RotationBreak` → `Rotation Break A`
- `FastBreak` → `Fast Break A`
- `MultiBreak` → `Multi Break`
- `AirLiquidBreak` → `Air Liquid Break`
- `PositionBreakA` → `Position Break`
- `MultiActionsB` → `Multi Interact A`
- `MultiActionsF` → `Multi Interact A`
- `MultiActionsA` → `Multi Interact A`
- `MultiActionsC` → `Multi Interact A`
- `MultiActionsD` → `Multi Interact A`
- `MultiActionsG` → `Multi Interact A`
- `MultiActionsE` → `Multi Interact A`
- `ElytraC` → `Elytra A`
- `ElytraA` → `Elytra A`
- `ElytraI` → `Elytra A`
- `ElytraH` → `Elytra A`
- `ElytraE` → `Elytra A`
- `ElytraF` → `Elytra A`
- `ElytraB` → `Elytra A`
- `ElytraD` → `Elytra A`
- `ElytraG` → `Elytra A`
- `ChatA` → `Chat A`
- `ChatB` → `Chat A`
- `ChatD` → `Chat A`
- `ChatC` → `Chat A`
- `CrashC` → `Crash A`
- `CrashG` → `Crash A`
- `CrashE` → `Crash A`
- `CrashB` → `Crash A`
- `CrashH` → `Crash A`
- `CrashI` → `Crash A`
- `CrashF` → `Crash A`
- `CrashA` → `Crash A`
- `CrashD` → `Crash A`

### TruthfulAC

**Missing from Windfall (3 checks):**

- `combat` **LagB** → `windfall.combat.lag b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/lag/LagB.java`
  - `MIN_REACH_TO_TRIGGER = 3.1D`
  - `WINDOW_MS = 8000`
  - `MIN_DESYNC_HITS = 3`
- `combat` **LagA** → `windfall.combat.lag a`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/lag/LagA.java`
  - `MAX_ATTACK_AGE_TICKS = 40`
  - `BACKTRACK_TOLERANCE = 0.45D`
  - `HARD_CEILING = 7.5D`
- `combat` **LagC** → `windfall.combat.lag c`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/lag/LagC.java`
  - `MIN_SAMPLES = 20`
  - `MIN_CORRELATION = 0.85D`
  - `PING_BIN_COUNT = 4`

**Matched with existing Windfall checks:**

- `PhaseA` → `Phase A`
- `FastBreakA` → `Fast Break A`
- `ScaffoldD` → `Scaffold A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldSupport` → `Scaffold Support`
- `ScaffoldE` → `Scaffold A`
- `ScaffoldH` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `ScaffoldG` → `Scaffold A`
- `ScaffoldF` → `Scaffold A`
- `ScaffoldA` → `Scaffold A`
- `TimerA` → `Timer A`
- `SprintA` → `Sprint A`
- `SprintB` → `Sprint B`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `InvalidA` → `Invalid Break A`
- `CrasherA` → `Crash A`
- `BadPacketJ` → `Bad Packets A`
- `BadPacketH` → `Bad Packets A`
- `BadPacketI` → `Bad Packets A`
- `BadPacketA` → `Bad Packets A`
- `BadPacketK` → `Bad Packets A`
- `BadPacketG` → `Bad Packets A`
- `BadPacketC` → `Bad Packets A`
- `BadPacketD` → `Bad Packets A`
- `BadPacketE` → `Bad Packets A`
- `RaycastA` → `Reach A`
- `MovementCheckSupport` → `Phase A`
- `GroundSpoofD` → `Ground Spoof A`
- `GroundSpoofE` → `Ground Spoof A`
- `GroundSpoofB` → `Ground Spoof A`
- `GroundSpoofG` → `Ground Spoof A`
- `GroundSpoofC` → `Ground Spoof A`
- `GroundSpoofF` → `Ground Spoof A`
- `InventoryA` → `Inventory A`
- `BaritoneB` → `Baritone A`
- `BaritoneA` → `Baritone A`
- `BaritoneC` → `Baritone A`
- `VelocityB` → `Velocity A`
- `VelocityD` → `Velocity A`
- `VelocityA` → `Velocity A`
- `VelocityC` → `Velocity A`
- `SimulationA` → `Simulation A`
- `SimulationB` → `Simulation A`
- `SimulationC` → `Simulation A`
- `SimulationD` → `Simulation A`
- `SimulationF` → `Simulation A`
- `SimulationE` → `Simulation A`
- `HitboxA` → `Hitboxes A`
- `KillAuraB` → `Kill Aura A`
- `KillAuraH` → `Kill Aura A`
- `KillAuraF` → `Kill Aura A`
- `KillAuraC` → `Kill Aura A`
- `KillAuraD` → `Kill Aura A`
- `KillAuraE` → `Kill Aura A`
- `KillAuraG` → `Kill Aura A`
- `ReachA` → `Reach A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerA` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerE` → `Autoclicker A`
- `CrystalAuraA` → `Kill Aura A`
- `AnchorAuraA` → `Kill Aura A`
- `BFlyA` → `Fly A`
- `BSpeedA` → `Speed A`
- `BReachA` → `Reach A`

### CloudAC

**No new checks detected.**

**Matched with existing Windfall checks:**

- `CheckAbilties` → `Exploit A`

### Arrow

**Missing from Windfall (4 checks):**

- `packet` **Suspicion** → `windfall.packet.suspicion`
  - Source: `src/main/java/me/arrow/checks/impl/suspicion/Suspicion.java`
  - `WINDOW_SIZE = 30`
  - `EVAL_INTERVAL = 10`
  - `NUM_FEATURES = 15`
- `combat` **AimH2** → `windfall.combat.aim h2`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/AimH2.java`
- `combat` **LinearRegression** → `windfall.combat.linear regression`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/aimassistUtil/LinearRegression.java`
  - `n = x.length`
  - `xbar = sumx / n`
  - `ybar = sumy / n`
- `movement` **Movement** → `windfall.movement.movement`
  - Source: `src/main/java/me/arrow/checks/impl/simulation/Movement.java`

**Matched with existing Windfall checks:**

- `TimerA` → `Timer A`
- `TimerC` → `Timer A`
- `TimerB` → `Timer A`
- `InventoryA` → `Inventory A`
- `ExploitB` → `Exploit A`
- `ExploitA` → `Exploit A`
- `ScaffoldD` → `Scaffold A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `ScaffoldA` → `Scaffold A`
- `VehicleC` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `FastBreakB` → `Fast Break A`
- `InteractD` → `Reach A`
- `InteractC` → `Interact Cursor`
- `InteractA` → `Interact Cursor`
- `FastBreakA` → `Fast Break A`
- `GroundC` → `Ground Spoof A`
- `GroundA` → `Ground Spoof A`
- `GroundB` → `Ground Spoof A`
- `MotionA` → `Motion A`
- `MotionB` → `Motion A`
- `MotionE` → `Motion A`
- `MotionD` → `Motion A`
- `GravityD` → `Gravity`
- `ElytraA` → `Elytra A`
- `FlyB` → `Fly A`
- `GravityB` → `Gravity`
- `GravityA` → `Gravity`
- `ElytraB` → `Elytra A`
- `GravityC` → `Gravity`
- `FlyA` → `Fly A`
- `SpeedB` → `Speed A`
- `SpeedA` → `Speed A`
- `OmniSprintA` → `Sprint A`
- `IllegalMoveB` → `Phase A`
- `IllegalMoveC` → `Illegal Move`
- `AimC` → `Aim A`
- `AimF` → `Aim A`
- `AimE` → `Aim A`
- `AimD` → `Aim A`
- `AimG` → `Aim A`
- `AimA` → `Aim A`
- `AimH` → `Aim A`
- `AimB` → `Aim A`
- `HitboxA` → `Hitboxes A`
- `KillauraA` → `Kill Aura A`
- `BackTrackB` → `Backtrack A`
- `BackTrackA` → `Backtrack A`
- `ReachA` → `Reach A`
- `VelocityB` → `Velocity A`
- `VelocityA` → `Velocity A`
- `AutoClickerH` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerG` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `MacroA` → `Macro A`
- `MacroB` → `Macro A`
- `AutoClickerF` → `Autoclicker A`

---

## Summary

- Windfall has **60 checks**
- Found **8 new checks** across competitors that Windfall doesn't have

## Recommendations

1. Review generated skeleton files in `src/main/java/io/windfall/anticheat/core/check/impl/`
2. Implement detection logic based on competitor reference
3. Tune thresholds and buffer values for each check
4. Register new checks in `CheckManager.java`
5. Add config entries to `config.yml`
6. Test on live server before enabling punishable mode
