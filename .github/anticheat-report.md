# Windfall Anti-Cheat Monitor Report

**Generated:** 2026-10-24 02:23:30 UTC

---

## Windfall Current Checks

**Total: 60 checks**

### Combat
- `windfall.combat.backtrack` — Backtrack A
- `windfall.combat.selfinteract` — Self Interact A
- `windfall.combat.macro` — Macro A
- `windfall.combat.multiinteract` — Multi Interact A
- `windfall.combat.aim` — Aim A
- `windfall.combat.killaura` — Kill Aura A
- `windfall.combat.reach` — Reach A
- `windfall.combat.criticals` — Criticals A
- `windfall.combat.fastheal` — Fast Heal A
- `windfall.combat.crystal` — Crystal A
- `windfall.combat.swordblock` — Sword Block A
- `windfall.combat.interactcursor` — Interact Cursor
- `windfall.combat.hitboxes` — Hitboxes A
- `windfall.combat.autoclicker` — Autoclicker A

### Movement
- `windfall.movement.rotationbreak` — Rotation Break A
- `windfall.movement.wrongbreak` — Wrong Break
- `windfall.movement.motion` — Motion A
- `windfall.movement.farbreak` — Far Break A
- `windfall.movement.noslow` — NoSlow A
- `windfall.movement.multiplace` — Multi Place
- `windfall.movement.illegalmove` — Illegal Move
- `windfall.movement.fly` — Fly A
- `windfall.movement.farplace` — Far Place A
- `windfall.movement.velocity` — Velocity A
- `windfall.movement.groundspoof` — Ground Spoof A
- `windfall.movement.scaffoldsupport` — Scaffold Support
- `windfall.movement.airliquidplace` — Air Liquid Place
- `windfall.movement.airliquidbreak` — Air Liquid Break
- `windfall.movement.baritone` — Baritone A
- `windfall.movement.multibreak` — Multi Break
- `windfall.movement.scaffold` — Scaffold A
- `windfall.movement.timer` — Timer A
- `windfall.movement.fastbreak` — Fast Break A
- `windfall.movement.invalidplace` — Invalid Place A
- `windfall.movement.speed` — Speed A
- `windfall.movement.invalidbreak` — Invalid Break A
- `windfall.movement.sprint` — Sprint B
- `windfall.movement.positionbreak` — Position Break
- `windfall.movement.rotationplace` — Rotation Place
- `windfall.movement.elytra` — Elytra A
- `windfall.movement.gravity` — Gravity
- `windfall.movement.phase` — Phase A
- `windfall.movement.simulation` — Simulation A
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.positionplace` — Position Place
- `windfall.movement.step` — Step A
- `windfall.movement.invalidplacecursor` — Invalid Place Cursor
- `windfall.movement.noswing` — No Swing A

### Packet
- `windfall.packet.brand` — Client Brand A
- `windfall.packet.order` — Packet Order A
- `windfall.packet.cheststealer` — Chest Stealer A
- `windfall.packet.transaction` — Transaction A
- `windfall.packet.sprint` — Sprint A
- `windfall.packet.bad` — Bad Packets A
- `windfall.packet.exploit` — Exploit A
- `windfall.packet.chat` — Chat A
- `windfall.packet.crash` — Crash A
- `windfall.packet.creative` — Creative A
- `windfall.packet.vehicle` — Vehicle A

---

## Competitor Analysis

### Grim

**Missing from Windfall (1 checks):**

- `movement` **InvalidPlaceFace** → `windfall.movement.invalid place face`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceFace.java`

**Matched with existing Windfall checks:**

- `AirLiquidPlace` → `Air Liquid Place`
- `InvalidPlaceCursor` → `Invalid Place Cursor`
- `DuplicateRotPlace` → `Rotation Place`
- `PositionPlace` → `Position Place`
- `MultiPlace` → `Multi Place`
- `FarPlace` → `Far Place A`
- `FabricatedPlace` → `Invalid Place A`
- `RotationPlace` → `Rotation Place`
- `MultiActionsB` → `Multi Interact A`
- `MultiActionsD` → `Multi Interact A`
- `MultiActionsA` → `Multi Interact A`
- `MultiActionsG` → `Multi Interact A`
- `MultiActionsE` → `Multi Interact A`
- `MultiActionsC` → `Multi Interact A`
- `MultiActionsF` → `Multi Interact A`
- `AimDuplicateLook` → `Aim A`
- `AimModulo360` → `Aim A`
- `PositionBreakA` → `Position Break`
- `FastBreak` → `Fast Break A`
- `NoSwingBreak` → `No Swing A`
- `RotationBreak` → `Rotation Break A`
- `InvalidBreak` → `Invalid Break A`
- `WrongBreak` → `Wrong Break`
- `FarBreak` → `Far Break A`
- `PositionBreakB` → `Position Break`
- `AirLiquidBreak` → `Air Liquid Break`
- `MultiBreak` → `Multi Break`
- `MultiInteractB` → `Multi Interact A`
- `InvalidInteractCursor` → `Interact Cursor`
- `SelfInteract` → `Self Interact A`
- `MultiInteractA` → `Multi Interact A`
- `Reach` → `Reach A`
- `ExploitA` → `Exploit A`
- `ExploitB` → `Exploit A`
- `Baritone` → `Baritone A`
- `SprintA` → `Sprint A`
- `SprintG` → `Sprint B`
- `SprintD` → `Sprint B`
- `SprintC` → `Sprint B`
- `SprintF` → `Sprint B`
- `SprintE` → `Sprint B`
- `SprintB` → `Sprint B`
- `NoSlow` → `NoSlow A`
- `ClientBrand` → `Client Brand A`
- `Post` → `No Swing A`
- `PacketOrderP` → `Packet Order A`
- `PacketOrderG` → `Packet Order A`
- `PacketOrderH` → `Packet Order A`
- `PacketOrderF` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderN` → `Packet Order A`
- `PacketOrderL` → `Packet Order A`
- `PacketOrderO` → `Packet Order A`
- `PacketOrderJ` → `Packet Order A`
- `PacketOrderK` → `Packet Order A`
- `PacketOrderI` → `Packet Order A`
- `PacketOrderM` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `VehicleE` → `Vehicle A`
- `VehicleF` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `VehicleD` → `Vehicle A`
- `ChatC` → `Chat A`
- `ChatA` → `Chat A`
- `ChatB` → `Chat A`
- `ChatD` → `Chat A`
- `Timer` → `Timer A`
- `VehicleTimer` → `Timer A`
- `TickTimer` → `Timer A`
- `NegativeTimer` → `Timer A`
- `TimerLimit` → `Timer A`
- `ElytraF` → `Elytra A`
- `ElytraC` → `Elytra A`
- `ElytraG` → `Elytra A`
- `ElytraD` → `Elytra A`
- `ElytraE` → `Elytra A`
- `ElytraH` → `Elytra A`
- `ElytraI` → `Elytra A`
- `ElytraB` → `Elytra A`
- `ElytraA` → `Elytra A`
- `FlightA` → `Fly A`
- `NoFall` → `NoFall A`
- `Phase` → `Phase A`
- `GroundSpoof` → `Ground Spoof A`
- `CrashD` → `Crash A`
- `CrashH` → `Crash A`
- `CrashB` → `Crash A`
- `CrashG` → `Crash A`
- `CrashA` → `Crash A`
- `CrashE` → `Crash A`
- `CrashI` → `Crash A`
- `CrashC` → `Crash A`
- `CrashF` → `Crash A`

### TruthfulAC

**Missing from Windfall (3 checks):**

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
- `combat` **LagB** → `windfall.combat.lag b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/lag/LagB.java`
  - `MIN_REACH_TO_TRIGGER = 3.1D`
  - `WINDOW_MS = 8000`
  - `MIN_DESYNC_HITS = 3`

**Matched with existing Windfall checks:**

- `HitboxA` → `Hitboxes A`
- `CrystalAuraA` → `Kill Aura A`
- `ReachA` → `Reach A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerA` → `Autoclicker A`
- `AutoClickerE` → `Autoclicker A`
- `AnchorAuraA` → `Kill Aura A`
- `KillAuraH` → `Kill Aura A`
- `KillAuraB` → `Kill Aura A`
- `KillAuraE` → `Kill Aura A`
- `KillAuraF` → `Kill Aura A`
- `KillAuraG` → `Kill Aura A`
- `KillAuraC` → `Kill Aura A`
- `KillAuraD` → `Kill Aura A`
- `RaycastA` → `Reach A`
- `FastBreakA` → `Fast Break A`
- `PhaseA` → `Phase A`
- `ScaffoldE` → `Scaffold A`
- `ScaffoldH` → `Scaffold A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldSupport` → `Scaffold Support`
- `ScaffoldA` → `Scaffold A`
- `ScaffoldG` → `Scaffold A`
- `ScaffoldD` → `Scaffold A`
- `ScaffoldF` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `BadPacketH` → `Bad Packets A`
- `BadPacketD` → `Bad Packets A`
- `BadPacketJ` → `Bad Packets A`
- `BadPacketC` → `Bad Packets A`
- `BadPacketG` → `Bad Packets A`
- `BadPacketA` → `Bad Packets A`
- `BadPacketE` → `Bad Packets A`
- `BadPacketI` → `Bad Packets A`
- `BadPacketK` → `Bad Packets A`
- `SprintA` → `Sprint A`
- `SprintB` → `Sprint B`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `CrasherA` → `Crash A`
- `TimerA` → `Timer A`
- `InvalidA` → `Invalid Place A`
- `MovementCheckSupport` → `Phase A`
- `VelocityC` → `Velocity A`
- `VelocityD` → `Velocity A`
- `VelocityB` → `Velocity A`
- `VelocityA` → `Velocity A`
- `BaritoneC` → `Baritone A`
- `BaritoneA` → `Baritone A`
- `BaritoneB` → `Baritone A`
- `InventoryA` → `Inventory A`
- `GroundSpoofD` → `Ground Spoof A`
- `GroundSpoofF` → `Ground Spoof A`
- `GroundSpoofC` → `Ground Spoof A`
- `GroundSpoofG` → `Ground Spoof A`
- `GroundSpoofB` → `Ground Spoof A`
- `GroundSpoofE` → `Ground Spoof A`
- `SimulationC` → `Simulation A`
- `SimulationB` → `Simulation A`
- `SimulationD` → `Simulation A`
- `SimulationF` → `Simulation A`
- `SimulationA` → `Simulation A`
- `SimulationE` → `Simulation A`
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
- `packet` **Suspicion** → `windfall.packet.suspicion`
  - Source: `src/main/java/me/arrow/checks/impl/suspicion/Suspicion.java`
  - `WINDOW_SIZE = 30`
  - `EVAL_INTERVAL = 10`
  - `NUM_FEATURES = 15`
- `movement` **Movement** → `windfall.movement.movement`
  - Source: `src/main/java/me/arrow/checks/impl/simulation/Movement.java`

**Matched with existing Windfall checks:**

- `HitboxA` → `Hitboxes A`
- `ReachA` → `Reach A`
- `VelocityB` → `Velocity A`
- `VelocityA` → `Velocity A`
- `AutoClickerF` → `Autoclicker A`
- `MacroB` → `Macro A`
- `MacroA` → `Macro A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerG` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerH` → `Autoclicker A`
- `KillauraA` → `Kill Aura A`
- `AimG` → `Aim A`
- `AimH` → `Aim A`
- `AimD` → `Aim A`
- `AimA` → `Aim A`
- `AimF` → `Aim A`
- `AimE` → `Aim A`
- `AimC` → `Aim A`
- `AimB` → `Aim A`
- `BackTrackB` → `Backtrack A`
- `BackTrackA` → `Backtrack A`
- `IllegalMoveB` → `Phase A`
- `IllegalMoveC` → `Illegal Move`
- `MotionD` → `Motion A`
- `MotionE` → `Motion A`
- `MotionB` → `Motion A`
- `MotionA` → `Motion A`
- `OmniSprintA` → `Sprint A`
- `SpeedB` → `Speed A`
- `SpeedA` → `Speed A`
- `GroundB` → `Ground Spoof A`
- `GroundA` → `Ground Spoof A`
- `GroundC` → `Ground Spoof A`
- `FlyB` → `Fly A`
- `FlyA` → `Fly A`
- `GravityD` → `Gravity`
- `GravityB` → `Gravity`
- `ElytraB` → `Elytra A`
- `GravityA` → `Gravity`
- `GravityC` → `Gravity`
- `ElytraA` → `Elytra A`
- `InteractC` → `Self Interact A`
- `FastBreakB` → `Fast Break A`
- `FastBreakA` → `Fast Break A`
- `InteractA` → `Self Interact A`
- `InteractD` → `Reach A`
- `InventoryA` → `Inventory A`
- `VehicleC` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `TimerA` → `Timer A`
- `TimerB` → `Timer A`
- `TimerC` → `Timer A`
- `ExploitA` → `Exploit A`
- `ExploitB` → `Exploit A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldA` → `Scaffold A`
- `ScaffoldD` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`

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
