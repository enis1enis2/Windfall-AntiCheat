# Windfall Anti-Cheat Monitor Report

**Generated:** 2026-09-05 01:52:33 UTC

---

## Windfall Current Checks

**Total: 54 checks**

### Combat
- `windfall.combat.hitboxes` — Hitboxes A
- `windfall.combat.killaura` — Kill Aura A
- `windfall.combat.multiinteract` — Multi Interact A
- `windfall.combat.autoclicker` — Autoclicker A
- `windfall.combat.selfinteract` — Self Interact A
- `windfall.combat.aim` — Aim A
- `windfall.combat.backtrack` — Backtrack A
- `windfall.combat.criticals` — Criticals A
- `windfall.combat.reach` — Reach A
- `windfall.combat.fastheal` — Fast Heal A
- `windfall.combat.swordblock` — Sword Block A
- `windfall.combat.macro` — Macro A

### Movement
- `windfall.movement.airliquidbreak` — Air Liquid Break
- `windfall.movement.phase` — Phase A
- `windfall.movement.rotationbreak` — Rotation Break A
- `windfall.movement.fly` — Fly A
- `windfall.movement.airliquidplace` — Air Liquid Place
- `windfall.movement.positionbreak` — Position Break
- `windfall.movement.fastbreak` — Fast Break A
- `windfall.movement.step` — Step A
- `windfall.movement.noslow` — NoSlow A
- `windfall.movement.timer` — Timer A
- `windfall.movement.invalidbreak` — Invalid Break A
- `windfall.movement.speed` — Speed A
- `windfall.movement.invalidplace` — Invalid Place A
- `windfall.movement.illegalmove` — Illegal Move
- `windfall.movement.velocity` — Velocity A
- `windfall.movement.noswing` — No Swing A
- `windfall.movement.simulation` — Simulation A
- `windfall.movement.multiplace` — Multi Place
- `windfall.movement.elytra` — Elytra A
- `windfall.movement.multibreak` — Multi Break
- `windfall.movement.farbreak` — Far Break A
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.scaffold` — Scaffold A
- `windfall.movement.motion` — Motion A
- `windfall.movement.rotationplace` — Rotation Place
- `windfall.movement.baritone` — Baritone A
- `windfall.movement.farplace` — Far Place A
- `windfall.movement.wrongbreak` — Wrong Break
- `windfall.movement.positionplace` — Position Place
- `windfall.movement.groundspoof` — Ground Spoof A

### Packet
- `windfall.packet.chat` — Chat A
- `windfall.packet.transaction` — Transaction A
- `windfall.packet.vehicle` — Vehicle A
- `windfall.packet.order` — Packet Order A
- `windfall.packet.cheststealer` — Chest Stealer A
- `windfall.packet.exploit` — Exploit A
- `windfall.packet.brand` — Client Brand A
- `windfall.packet.creative` — Creative A
- `windfall.packet.bad` — Bad Packets A
- `windfall.packet.crash` — Crash A
- `windfall.packet.sprint` — Sprint A

---

## Competitor Analysis

### Grim

**Missing from Windfall (3 checks):**

- `combat` **InvalidInteractCursor** → `windfall.combat.invalid interact cursor`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/combat/InvalidInteractCursor.java`
  - `scale = (float) packetEntity.getAttributeValue(Attributes.SCALE)`
- `movement` **InvalidPlaceFace** → `windfall.movement.invalid place face`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceFace.java`
- `movement` **InvalidPlaceCursor** → `windfall.movement.invalid place cursor`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceCursor.java`

**Matched with existing Windfall checks:**

- `PacketOrderE` → `Packet Order A`
- `PacketOrderM` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderG` → `Packet Order A`
- `PacketOrderP` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderN` → `Packet Order A`
- `PacketOrderH` → `Packet Order A`
- `PacketOrderL` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderJ` → `Packet Order A`
- `PacketOrderF` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderI` → `Packet Order A`
- `PacketOrderK` → `Packet Order A`
- `PacketOrderO` → `Packet Order A`
- `MultiInteractA` → `Multi Interact A`
- `MultiInteractB` → `Multi Interact A`
- `SelfInteract` → `Self Interact A`
- `Reach` → `Reach A`
- `MultiActionsE` → `Multi Interact A`
- `MultiActionsD` → `Multi Interact A`
- `MultiActionsA` → `Multi Interact A`
- `MultiActionsG` → `Multi Interact A`
- `MultiActionsB` → `Multi Interact A`
- `MultiActionsC` → `Multi Interact A`
- `MultiActionsF` → `Multi Interact A`
- `CrashF` → `Crash A`
- `CrashB` → `Crash A`
- `CrashD` → `Crash A`
- `CrashC` → `Crash A`
- `CrashH` → `Crash A`
- `CrashG` → `Crash A`
- `CrashE` → `Crash A`
- `CrashA` → `Crash A`
- `CrashI` → `Crash A`
- `ExploitA` → `Exploit A`
- `ExploitB` → `Exploit A`
- `VehicleF` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `VehicleE` → `Vehicle A`
- `VehicleD` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `ChatC` → `Chat A`
- `ChatA` → `Chat A`
- `ChatD` → `Chat A`
- `ChatB` → `Chat A`
- `VehicleTimer` → `Timer A`
- `Timer` → `Timer A`
- `NegativeTimer` → `Timer A`
- `TimerLimit` → `Timer A`
- `TickTimer` → `Timer A`
- `SprintG` → `Sprint A`
- `SprintF` → `Sprint A`
- `SprintC` → `Sprint A`
- `SprintB` → `Sprint A`
- `SprintE` → `Sprint A`
- `SprintA` → `Sprint A`
- `SprintD` → `Sprint A`
- `Baritone` → `Baritone A`
- `ClientBrand` → `Client Brand A`
- `Post` → `No Swing A`
- `NoSlow` → `NoSlow A`
- `AimModulo360` → `Aim A`
- `AimDuplicateLook` → `Aim A`
- `Phase` → `Phase A`
- `GroundSpoof` → `Ground Spoof A`
- `RotationBreak` → `Rotation Break A`
- `PositionBreakA` → `Position Break`
- `FarBreak` → `Far Break A`
- `NoSwingBreak` → `No Swing A`
- `PositionBreakB` → `Position Break`
- `AirLiquidBreak` → `Air Liquid Break`
- `InvalidBreak` → `Invalid Break A`
- `MultiBreak` → `Multi Break`
- `WrongBreak` → `Wrong Break`
- `FastBreak` → `Fast Break A`
- `MultiPlace` → `Multi Place`
- `FarPlace` → `Far Place A`
- `DuplicateRotPlace` → `Rotation Place`
- `RotationPlace` → `Rotation Place`
- `PositionPlace` → `Position Place`
- `FabricatedPlace` → `Invalid Place A`
- `AirLiquidPlace` → `Air Liquid Place`
- `ElytraH` → `Elytra A`
- `ElytraI` → `Elytra A`
- `ElytraG` → `Elytra A`
- `ElytraF` → `Elytra A`
- `ElytraE` → `Elytra A`
- `ElytraA` → `Elytra A`
- `ElytraC` → `Elytra A`
- `ElytraB` → `Elytra A`
- `ElytraD` → `Elytra A`
- `FlightA` → `Fly A`
- `NoFall` → `NoFall A`

### TruthfulAC

**Missing from Windfall (4 checks):**

- `combat` **LagA** → `windfall.combat.lag a`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/lag/LagA.java`
  - `MAX_ATTACK_AGE_TICKS = 40`
  - `BACKTRACK_TOLERANCE = 0.45D`
  - `HARD_CEILING = 7.5D`
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
- `movement` **ScaffoldSupport** → `windfall.movement.scaffold support`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldSupport.java`
  - `MAX_SLOTS = 40`
  - `MAX = 60`

**Matched with existing Windfall checks:**

- `CrystalAuraA` → `Kill Aura A`
- `ReachA` → `Reach A`
- `AnchorAuraA` → `Kill Aura A`
- `HitboxA` → `Hitboxes A`
- `KillAuraD` → `Kill Aura A`
- `KillAuraB` → `Kill Aura A`
- `KillAuraF` → `Kill Aura A`
- `KillAuraG` → `Kill Aura A`
- `KillAuraC` → `Kill Aura A`
- `KillAuraH` → `Kill Aura A`
- `KillAuraE` → `Kill Aura A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerA` → `Autoclicker A`
- `AutoClickerE` → `Autoclicker A`
- `RaycastA` → `Reach A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldF` → `Scaffold A`
- `ScaffoldE` → `Scaffold A`
- `ScaffoldH` → `Scaffold A`
- `ScaffoldD` → `Scaffold A`
- `ScaffoldA` → `Scaffold A`
- `ScaffoldG` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `PhaseA` → `Phase A`
- `FastBreakA` → `Fast Break A`
- `BFlyA` → `Fly A`
- `BSpeedA` → `Speed A`
- `BReachA` → `Reach A`
- `MovementCheckSupport` → `Phase A`
- `InventoryA` → `Inventory A`
- `VelocityC` → `Velocity A`
- `VelocityB` → `Velocity A`
- `VelocityD` → `Velocity A`
- `VelocityA` → `Velocity A`
- `BaritoneA` → `Baritone A`
- `BaritoneC` → `Baritone A`
- `BaritoneB` → `Baritone A`
- `SimulationC` → `Simulation A`
- `SimulationF` → `Simulation A`
- `SimulationD` → `Simulation A`
- `SimulationE` → `Simulation A`
- `SimulationB` → `Simulation A`
- `SimulationA` → `Simulation A`
- `GroundSpoofF` → `Ground Spoof A`
- `GroundSpoofE` → `Ground Spoof A`
- `GroundSpoofC` → `Ground Spoof A`
- `GroundSpoofD` → `Ground Spoof A`
- `GroundSpoofB` → `Ground Spoof A`
- `GroundSpoofG` → `Ground Spoof A`
- `BadPacketK` → `Bad Packets A`
- `BadPacketA` → `Bad Packets A`
- `BadPacketI` → `Bad Packets A`
- `BadPacketC` → `Bad Packets A`
- `BadPacketE` → `Bad Packets A`
- `BadPacketD` → `Bad Packets A`
- `BadPacketG` → `Bad Packets A`
- `BadPacketH` → `Bad Packets A`
- `BadPacketJ` → `Bad Packets A`
- `InvalidA` → `Invalid Break A`
- `CrasherA` → `Crash A`
- `TimerA` → `Timer A`
- `SprintB` → `Sprint A`
- `SprintA` → `Sprint A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`

### CloudAC

**No new checks detected.**

**Matched with existing Windfall checks:**

- `CheckAbilties` → `Exploit A`

### Arrow

**Missing from Windfall (8 checks):**

- `combat` **AimH2** → `windfall.combat.aim h2`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/AimH2.java`
- `combat` **LinearRegression** → `windfall.combat.linear regression`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/aimassistUtil/LinearRegression.java`
  - `n = x.length`
  - `xbar = sumx / n`
  - `ybar = sumy / n`
- `movement` **Movement** → `windfall.movement.movement`
  - Source: `src/main/java/me/arrow/checks/impl/simulation/Movement.java`
- `packet` **InteractE** → `windfall.packet.interact e`
  - Source: `src/main/java/me/arrow/checks/impl/misc/interact/InteractE.java`
- `movement` **GravityA** → `windfall.movement.gravity a`
  - Source: `src/main/java/me/arrow/checks/impl/movement/fly/GravityA.java`
- `movement` **GravityD** → `windfall.movement.gravity d`
  - Source: `src/main/java/me/arrow/checks/impl/movement/fly/GravityD.java`
  - `transTicks = getLagCompensationTicks()`
  - `DEFAULT_GRAVITY = 0.08D`
  - `AIR_DRAG = 0.9800000190734863D`
- `movement` **GravityB** → `windfall.movement.gravity b`
  - Source: `src/main/java/me/arrow/checks/impl/movement/fly/GravityB.java`
- `movement` **GravityC** → `windfall.movement.gravity c`
  - Source: `src/main/java/me/arrow/checks/impl/movement/fly/GravityC.java`
  - `JUMP_TOL = 0.046D`
  - `G = 0.08D`
  - `DRAG = 0.9800000190734863D`

**Matched with existing Windfall checks:**

- `ReachA` → `Reach A`
- `VelocityB` → `Velocity A`
- `VelocityA` → `Velocity A`
- `HitboxA` → `Hitboxes A`
- `KillauraA` → `Kill Aura A`
- `AimB` → `Aim A`
- `AimG` → `Aim A`
- `AimA` → `Aim A`
- `AimC` → `Aim A`
- `AimH` → `Aim A`
- `AimD` → `Aim A`
- `AimF` → `Aim A`
- `AimE` → `Aim A`
- `BackTrackB` → `Backtrack A`
- `BackTrackA` → `Backtrack A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `MacroB` → `Macro A`
- `AutoClickerH` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerG` → `Autoclicker A`
- `MacroA` → `Macro A`
- `AutoClickerF` → `Autoclicker A`
- `InventoryA` → `Inventory A`
- `InteractD` → `Reach A`
- `InteractA` → `Multi Interact A`
- `InteractC` → `Multi Interact A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldA` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `VehicleA` → `Vehicle A`
- `TimerB` → `Timer A`
- `TimerC` → `Timer A`
- `TimerA` → `Timer A`
- `SpeedA` → `Speed A`
- `OmniSprintA` → `Sprint A`
- `SpeedB` → `Speed A`
- `ElytraA` → `Elytra A`
- `FlyA` → `Fly A`
- `FlyB` → `Fly A`
- `MotionA` → `Motion A`
- `MotionB` → `Motion A`
- `MotionE` → `Motion A`
- `MotionD` → `Motion A`
- `IllegalMoveC` → `Illegal Move`
- `IllegalMoveB` → `Phase A`
- `GroundB` → `Ground Spoof A`
- `GroundA` → `Ground Spoof A`
- `GroundC` → `Ground Spoof A`

---

## Summary

- Windfall has **54 checks**
- Found **15 new checks** across competitors that Windfall doesn't have

## Recommendations

1. Review generated skeleton files in `src/main/java/io/windfall/anticheat/core/check/impl/`
2. Implement detection logic based on competitor reference
3. Tune thresholds and buffer values for each check
4. Register new checks in `CheckManager.java`
5. Add config entries to `config.yml`
6. Test on live server before enabling punishable mode
