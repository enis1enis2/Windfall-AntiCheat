# Windfall Anti-Cheat Monitor Report

**Generated:** 2026-10-07 03:10:51 UTC

---

## Windfall Current Checks

**Total: 58 checks**

### Combat
- `windfall.combat.criticals` — Criticals A
- `windfall.combat.selfinteract` — Self Interact A
- `windfall.combat.macro` — Macro A
- `windfall.combat.aim` — Aim A
- `windfall.combat.multiinteract` — Multi Interact A
- `windfall.combat.autoclicker` — Autoclicker A
- `windfall.combat.fastheal` — Fast Heal A
- `windfall.combat.hitboxes` — Hitboxes A
- `windfall.combat.killaura` — Kill Aura A
- `windfall.combat.backtrack` — Backtrack A
- `windfall.combat.reach` — Reach A
- `windfall.combat.swordblock` — Sword Block A
- `windfall.combat.interactcursor` — Interact Cursor

### Movement
- `windfall.movement.scaffoldsupport` — Scaffold Support
- `windfall.movement.noslow` — NoSlow A
- `windfall.movement.phase` — Phase A
- `windfall.movement.noswing` — No Swing A
- `windfall.movement.step` — Step A
- `windfall.movement.motion` — Motion A
- `windfall.movement.invalidplacecursor` — Invalid Place Cursor
- `windfall.movement.baritone` — Baritone A
- `windfall.movement.simulation` — Simulation A
- `windfall.movement.multibreak` — Multi Break
- `windfall.movement.invalidplace` — Invalid Place A
- `windfall.movement.multiplace` — Multi Place
- `windfall.movement.fly` — Fly A
- `windfall.movement.rotationplace` — Rotation Place
- `windfall.movement.farbreak` — Far Break A
- `windfall.movement.gravity` — Gravity
- `windfall.movement.fastbreak` — Fast Break A
- `windfall.movement.positionplace` — Position Place
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.rotationbreak` — Rotation Break A
- `windfall.movement.timer` — Timer A
- `windfall.movement.groundspoof` — Ground Spoof A
- `windfall.movement.airliquidplace` — Air Liquid Place
- `windfall.movement.velocity` — Velocity A
- `windfall.movement.scaffold` — Scaffold A
- `windfall.movement.invalidbreak` — Invalid Break A
- `windfall.movement.wrongbreak` — Wrong Break
- `windfall.movement.positionbreak` — Position Break
- `windfall.movement.speed` — Speed A
- `windfall.movement.illegalmove` — Illegal Move
- `windfall.movement.elytra` — Elytra A
- `windfall.movement.farplace` — Far Place A
- `windfall.movement.airliquidbreak` — Air Liquid Break

### Packet
- `windfall.packet.creative` — Creative A
- `windfall.packet.order` — Packet Order A
- `windfall.packet.crash` — Crash A
- `windfall.packet.vehicle` — Vehicle A
- `windfall.packet.cheststealer` — Chest Stealer A
- `windfall.packet.exploit` — Exploit A
- `windfall.packet.sprint` — Sprint A
- `windfall.packet.bad` — Bad Packets A
- `windfall.packet.brand` — Client Brand A
- `windfall.packet.chat` — Chat A
- `windfall.packet.transaction` — Transaction A

---

## Competitor Analysis

### Grim

**Missing from Windfall (1 checks):**

- `movement` **InvalidPlaceFace** → `windfall.movement.invalid place face`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceFace.java`

**Matched with existing Windfall checks:**

- `SprintA` → `Sprint A`
- `SprintG` → `Sprint A`
- `SprintC` → `Sprint A`
- `SprintB` → `Sprint A`
- `SprintF` → `Sprint A`
- `SprintD` → `Sprint A`
- `SprintE` → `Sprint A`
- `SelfInteract` → `Self Interact A`
- `Reach` → `Reach A`
- `InvalidInteractCursor` → `Interact Cursor`
- `MultiInteractA` → `Multi Interact A`
- `MultiInteractB` → `Multi Interact A`
- `Post` → `No Swing A`
- `ClientBrand` → `Client Brand A`
- `ElytraD` → `Elytra A`
- `ElytraF` → `Elytra A`
- `ElytraE` → `Elytra A`
- `ElytraA` → `Elytra A`
- `ElytraC` → `Elytra A`
- `ElytraH` → `Elytra A`
- `ElytraB` → `Elytra A`
- `ElytraG` → `Elytra A`
- `ElytraI` → `Elytra A`
- `VehicleE` → `Vehicle A`
- `VehicleF` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `VehicleD` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `ChatA` → `Chat A`
- `ChatB` → `Chat A`
- `ChatC` → `Chat A`
- `ChatD` → `Chat A`
- `Baritone` → `Baritone A`
- `InvalidBreak` → `Invalid Break A`
- `PositionBreakA` → `Position Break`
- `NoSwingBreak` → `No Swing A`
- `FastBreak` → `Fast Break A`
- `AirLiquidBreak` → `Air Liquid Break`
- `FarBreak` → `Far Break A`
- `WrongBreak` → `Wrong Break`
- `MultiBreak` → `Multi Break`
- `PositionBreakB` → `Position Break`
- `RotationBreak` → `Rotation Break A`
- `AirLiquidPlace` → `Air Liquid Place`
- `PositionPlace` → `Position Place`
- `InvalidPlaceCursor` → `Invalid Place Cursor`
- `FarPlace` → `Far Place A`
- `MultiPlace` → `Multi Place`
- `RotationPlace` → `Rotation Place`
- `DuplicateRotPlace` → `Rotation Place`
- `FabricatedPlace` → `Invalid Place Cursor`
- `CrashH` → `Crash A`
- `CrashE` → `Crash A`
- `CrashC` → `Crash A`
- `CrashD` → `Crash A`
- `CrashG` → `Crash A`
- `CrashA` → `Crash A`
- `CrashF` → `Crash A`
- `CrashI` → `Crash A`
- `CrashB` → `Crash A`
- `ExploitA` → `Exploit A`
- `ExploitB` → `Exploit A`
- `Phase` → `Phase A`
- `GroundSpoof` → `Ground Spoof A`
- `TimerLimit` → `Timer A`
- `TickTimer` → `Timer A`
- `VehicleTimer` → `Timer A`
- `NegativeTimer` → `Timer A`
- `Timer` → `Timer A`
- `NoFall` → `NoFall A`
- `NoSlow` → `NoSlow A`
- `AimModulo360` → `Aim A`
- `AimDuplicateLook` → `Aim A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderL` → `Packet Order A`
- `PacketOrderF` → `Packet Order A`
- `PacketOrderO` → `Packet Order A`
- `PacketOrderK` → `Packet Order A`
- `PacketOrderM` → `Packet Order A`
- `PacketOrderG` → `Packet Order A`
- `PacketOrderI` → `Packet Order A`
- `PacketOrderJ` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderN` → `Packet Order A`
- `PacketOrderH` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderP` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `MultiActionsG` → `Multi Interact A`
- `MultiActionsB` → `Multi Interact A`
- `MultiActionsD` → `Multi Interact A`
- `MultiActionsE` → `Multi Interact A`
- `MultiActionsA` → `Multi Interact A`
- `MultiActionsF` → `Multi Interact A`
- `MultiActionsC` → `Multi Interact A`
- `FlightA` → `Fly A`

### TruthfulAC

**Missing from Windfall (3 checks):**

- `combat` **LagB** → `windfall.combat.lag b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/lag/LagB.java`
  - `MIN_REACH_TO_TRIGGER = 3.1D`
  - `WINDOW_MS = 8000`
  - `MIN_DESYNC_HITS = 3`
- `combat` **LagC** → `windfall.combat.lag c`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/lag/LagC.java`
  - `MIN_SAMPLES = 20`
  - `MIN_CORRELATION = 0.85D`
  - `PING_BIN_COUNT = 4`
- `combat` **LagA** → `windfall.combat.lag a`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/lag/LagA.java`
  - `MAX_ATTACK_AGE_TICKS = 40`
  - `BACKTRACK_TOLERANCE = 0.45D`
  - `HARD_CEILING = 7.5D`

**Matched with existing Windfall checks:**

- `AnchorAuraA` → `Kill Aura A`
- `HitboxA` → `Hitboxes A`
- `AutoClickerE` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerA` → `Autoclicker A`
- `ReachA` → `Reach A`
- `CrystalAuraA` → `Kill Aura A`
- `KillAuraD` → `Kill Aura A`
- `KillAuraH` → `Kill Aura A`
- `KillAuraF` → `Kill Aura A`
- `KillAuraE` → `Kill Aura A`
- `KillAuraB` → `Kill Aura A`
- `KillAuraG` → `Kill Aura A`
- `KillAuraC` → `Kill Aura A`
- `RaycastA` → `Reach A`
- `MovementCheckSupport` → `Phase A`
- `BaritoneB` → `Baritone A`
- `BaritoneC` → `Baritone A`
- `BaritoneA` → `Baritone A`
- `VelocityD` → `Velocity A`
- `VelocityC` → `Velocity A`
- `VelocityA` → `Velocity A`
- `VelocityB` → `Velocity A`
- `SimulationE` → `Simulation A`
- `SimulationC` → `Simulation A`
- `SimulationB` → `Simulation A`
- `SimulationD` → `Simulation A`
- `SimulationF` → `Simulation A`
- `SimulationA` → `Simulation A`
- `GroundSpoofC` → `Ground Spoof A`
- `GroundSpoofF` → `Ground Spoof A`
- `GroundSpoofE` → `Ground Spoof A`
- `GroundSpoofB` → `Ground Spoof A`
- `GroundSpoofD` → `Ground Spoof A`
- `GroundSpoofG` → `Ground Spoof A`
- `InventoryA` → `Inventory A`
- `SprintA` → `Sprint A`
- `SprintB` → `Sprint A`
- `CrasherA` → `Crash A`
- `BadPacketI` → `Bad Packets A`
- `BadPacketA` → `Bad Packets A`
- `BadPacketE` → `Bad Packets A`
- `BadPacketK` → `Bad Packets A`
- `BadPacketG` → `Bad Packets A`
- `BadPacketD` → `Bad Packets A`
- `BadPacketJ` → `Bad Packets A`
- `BadPacketH` → `Bad Packets A`
- `BadPacketC` → `Bad Packets A`
- `TimerA` → `Timer A`
- `InvalidA` → `Invalid Place Cursor`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PhaseA` → `Phase A`
- `ScaffoldA` → `Scaffold A`
- `ScaffoldG` → `Scaffold A`
- `ScaffoldH` → `Scaffold A`
- `ScaffoldF` → `Scaffold A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldE` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `ScaffoldD` → `Scaffold A`
- `ScaffoldSupport` → `Scaffold Support`
- `FastBreakA` → `Fast Break A`
- `BReachA` → `Reach A`
- `BSpeedA` → `Speed A`
- `BFlyA` → `Fly A`

### CloudAC

**No new checks detected.**

**Matched with existing Windfall checks:**

- `CheckAbilties` → `Exploit A`

### Arrow

**Missing from Windfall (4 checks):**

- `combat` **AimH2** → `windfall.combat.aim h2`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/AimH2.java`
- `combat` **LinearRegression** → `windfall.combat.linear regression`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/aimassistUtil/LinearRegression.java`
  - `n = x.length`
  - `xbar = sumx / n`
  - `ybar = sumy / n`
- `combat` **Vector3dm** → `windfall.combat.vector3dm`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/aimassistUtil/Vector3dm.java`
  - `serialVersionUID = -2657651106777219169L`
  - `epsilon = 1.0E-6`
- `movement` **Movement** → `windfall.movement.movement`
  - Source: `src/main/java/me/arrow/checks/impl/simulation/Movement.java`

**Matched with existing Windfall checks:**

- `HitboxA` → `Hitboxes A`
- `BackTrackB` → `Backtrack A`
- `BackTrackA` → `Backtrack A`
- `MacroB` → `Macro A`
- `MacroA` → `Macro A`
- `AutoClickerH` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerG` → `Autoclicker A`
- `AutoClickerF` → `Autoclicker A`
- `VelocityA` → `Velocity A`
- `VelocityB` → `Velocity A`
- `ReachA` → `Reach A`
- `AimG` → `Aim A`
- `AimJ` → `Aim A`
- `AimL` → `Aim A`
- `AimH` → `Aim A`
- `AimC` → `Aim A`
- `AimA` → `Aim A`
- `AimB` → `Aim A`
- `AimF` → `Aim A`
- `AimD` → `Aim A`
- `AimE` → `Aim A`
- `KillauraA` → `Kill Aura A`
- `InteractC` → `Self Interact A`
- `InteractD` → `Reach A`
- `InteractA` → `Self Interact A`
- `VehicleA` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `VehicleC` → `Vehicle A`
- `ScaffoldA` → `Scaffold A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `ScaffoldD` → `Scaffold A`
- `TimerC` → `Timer A`
- `TimerA` → `Timer A`
- `TimerB` → `Timer A`
- `InventoryA` → `Inventory A`
- `SpeedA` → `Speed A`
- `OmniSprintA` → `Sprint A`
- `SpeedB` → `Speed A`
- `IllegalMoveB` → `Phase A`
- `IllegalMoveC` → `Illegal Move`
- `GroundA` → `Ground Spoof A`
- `GroundB` → `Ground Spoof A`
- `GroundC` → `Ground Spoof A`
- `GravityB` → `Gravity`
- `FlyA` → `Fly A`
- `GravityA` → `Gravity`
- `ElytraA` → `Elytra A`
- `ElytraB` → `Elytra A`
- `FlyB` → `Fly A`
- `GravityD` → `Gravity`
- `GravityC` → `Gravity`
- `MotionA` → `Motion A`
- `MotionB` → `Motion A`
- `MotionD` → `Motion A`
- `MotionE` → `Motion A`

---

## Summary

- Windfall has **58 checks**
- Found **8 new checks** across competitors that Windfall doesn't have

## Recommendations

1. Review generated skeleton files in `src/main/java/io/windfall/anticheat/core/check/impl/`
2. Implement detection logic based on competitor reference
3. Tune thresholds and buffer values for each check
4. Register new checks in `CheckManager.java`
5. Add config entries to `config.yml`
6. Test on live server before enabling punishable mode
