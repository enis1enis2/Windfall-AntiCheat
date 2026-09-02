# Windfall Anti-Cheat Monitor Report

**Generated:** 2026-09-02 01:51:15 UTC

---

## Windfall Current Checks

**Total: 54 checks**

### Combat
- `windfall.combat.aim` — Aim A
- `windfall.combat.swordblock` — Sword Block A
- `windfall.combat.criticals` — Criticals A
- `windfall.combat.multiinteract` — Multi Interact A
- `windfall.combat.backtrack` — Backtrack A
- `windfall.combat.killaura` — Kill Aura A
- `windfall.combat.hitboxes` — Hitboxes A
- `windfall.combat.autoclicker` — Autoclicker A
- `windfall.combat.selfinteract` — Self Interact A
- `windfall.combat.macro` — Macro A
- `windfall.combat.reach` — Reach A
- `windfall.combat.fastheal` — Fast Heal A

### Movement
- `windfall.movement.illegalmove` — Illegal Move
- `windfall.movement.speed` — Speed A
- `windfall.movement.positionbreak` — Position Break
- `windfall.movement.scaffold` — Scaffold A
- `windfall.movement.multibreak` — Multi Break
- `windfall.movement.groundspoof` — Ground Spoof A
- `windfall.movement.phase` — Phase A
- `windfall.movement.multiplace` — Multi Place
- `windfall.movement.wrongbreak` — Wrong Break
- `windfall.movement.rotationbreak` — Rotation Break A
- `windfall.movement.invalidbreak` — Invalid Break A
- `windfall.movement.positionplace` — Position Place
- `windfall.movement.airliquidbreak` — Air Liquid Break
- `windfall.movement.rotationplace` — Rotation Place
- `windfall.movement.fastbreak` — Fast Break A
- `windfall.movement.noswing` — No Swing A
- `windfall.movement.noslow` — NoSlow A
- `windfall.movement.invalidplace` — Invalid Place A
- `windfall.movement.baritone` — Baritone A
- `windfall.movement.farbreak` — Far Break A
- `windfall.movement.fly` — Fly A
- `windfall.movement.motion` — Motion A
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.timer` — Timer A
- `windfall.movement.simulation` — Simulation A
- `windfall.movement.airliquidplace` — Air Liquid Place
- `windfall.movement.farplace` — Far Place A
- `windfall.movement.step` — Step A
- `windfall.movement.velocity` — Velocity A
- `windfall.movement.elytra` — Elytra A

### Packet
- `windfall.packet.creative` — Creative A
- `windfall.packet.transaction` — Transaction A
- `windfall.packet.crash` — Crash A
- `windfall.packet.order` — Packet Order A
- `windfall.packet.bad` — Bad Packets A
- `windfall.packet.exploit` — Exploit A
- `windfall.packet.chat` — Chat A
- `windfall.packet.cheststealer` — Chest Stealer A
- `windfall.packet.sprint` — Sprint A
- `windfall.packet.brand` — Client Brand A
- `windfall.packet.vehicle` — Vehicle A

---

## Competitor Analysis

### Grim

**Missing from Windfall (3 checks):**

- `movement` **InvalidPlaceCursor** → `windfall.movement.invalid place cursor`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceCursor.java`
- `movement` **InvalidPlaceFace** → `windfall.movement.invalid place face`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceFace.java`
- `combat` **InvalidInteractCursor** → `windfall.combat.invalid interact cursor`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/combat/InvalidInteractCursor.java`
  - `scale = (float) packetEntity.getAttributeValue(Attributes.SCALE)`

**Matched with existing Windfall checks:**

- `VehicleTimer` → `Timer A`
- `TickTimer` → `Timer A`
- `TimerLimit` → `Timer A`
- `NegativeTimer` → `Timer A`
- `Timer` → `Timer A`
- `ChatB` → `Chat A`
- `ChatD` → `Chat A`
- `ChatC` → `Chat A`
- `ChatA` → `Chat A`
- `NoFall` → `NoFall A`
- `VehicleF` → `Vehicle A`
- `VehicleD` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `VehicleE` → `Vehicle A`
- `SprintC` → `Sprint A`
- `SprintE` → `Sprint A`
- `SprintA` → `Sprint A`
- `SprintB` → `Sprint A`
- `SprintD` → `Sprint A`
- `SprintF` → `Sprint A`
- `SprintG` → `Sprint A`
- `PacketOrderO` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderL` → `Packet Order A`
- `PacketOrderK` → `Packet Order A`
- `PacketOrderI` → `Packet Order A`
- `PacketOrderG` → `Packet Order A`
- `PacketOrderF` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderM` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderP` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderJ` → `Packet Order A`
- `PacketOrderH` → `Packet Order A`
- `PacketOrderN` → `Packet Order A`
- `Baritone` → `Baritone A`
- `AimDuplicateLook` → `Aim A`
- `AimModulo360` → `Aim A`
- `RotationPlace` → `Rotation Place`
- `FabricatedPlace` → `Invalid Place A`
- `AirLiquidPlace` → `Air Liquid Place`
- `FarPlace` → `Far Place A`
- `DuplicateRotPlace` → `Rotation Place`
- `MultiPlace` → `Multi Place`
- `PositionPlace` → `Position Place`
- `ElytraC` → `Elytra A`
- `ElytraE` → `Elytra A`
- `ElytraG` → `Elytra A`
- `ElytraH` → `Elytra A`
- `ElytraF` → `Elytra A`
- `ElytraI` → `Elytra A`
- `ElytraB` → `Elytra A`
- `ElytraD` → `Elytra A`
- `ElytraA` → `Elytra A`
- `Phase` → `Phase A`
- `GroundSpoof` → `Ground Spoof A`
- `MultiActionsG` → `Multi Interact A`
- `MultiActionsA` → `Multi Interact A`
- `MultiActionsE` → `Multi Interact A`
- `MultiActionsD` → `Multi Interact A`
- `MultiActionsB` → `Multi Interact A`
- `MultiActionsF` → `Multi Interact A`
- `MultiActionsC` → `Multi Interact A`
- `ExploitA` → `Exploit A`
- `ExploitB` → `Exploit A`
- `FlightA` → `Fly A`
- `NoSlow` → `NoSlow A`
- `RotationBreak` → `Rotation Break A`
- `PositionBreakA` → `Position Break`
- `WrongBreak` → `Wrong Break`
- `PositionBreakB` → `Position Break`
- `MultiBreak` → `Multi Break`
- `FastBreak` → `Fast Break A`
- `NoSwingBreak` → `No Swing A`
- `AirLiquidBreak` → `Air Liquid Break`
- `InvalidBreak` → `Invalid Break A`
- `FarBreak` → `Far Break A`
- `Post` → `No Swing A`
- `ClientBrand` → `Client Brand A`
- `CrashB` → `Crash A`
- `CrashA` → `Crash A`
- `CrashF` → `Crash A`
- `CrashG` → `Crash A`
- `CrashD` → `Crash A`
- `CrashE` → `Crash A`
- `CrashI` → `Crash A`
- `CrashC` → `Crash A`
- `CrashH` → `Crash A`
- `MultiInteractA` → `Multi Interact A`
- `SelfInteract` → `Self Interact A`
- `Reach` → `Reach A`
- `MultiInteractB` → `Multi Interact A`

### TruthfulAC

**Missing from Windfall (4 checks):**

- `movement` **ScaffoldSupport** → `windfall.movement.scaffold support`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldSupport.java`
  - `MAX_SLOTS = 40`
  - `MAX = 60`
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

**Matched with existing Windfall checks:**

- `TimerA` → `Timer A`
- `CrasherA` → `Crash A`
- `SprintA` → `Sprint A`
- `SprintB` → `Sprint A`
- `InvalidA` → `Invalid Break A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `BadPacketI` → `Bad Packets A`
- `BadPacketE` → `Bad Packets A`
- `BadPacketK` → `Bad Packets A`
- `BadPacketC` → `Bad Packets A`
- `BadPacketG` → `Bad Packets A`
- `BadPacketA` → `Bad Packets A`
- `BadPacketD` → `Bad Packets A`
- `BadPacketH` → `Bad Packets A`
- `BadPacketJ` → `Bad Packets A`
- `ScaffoldF` → `Scaffold A`
- `ScaffoldE` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `ScaffoldD` → `Scaffold A`
- `ScaffoldG` → `Scaffold A`
- `ScaffoldH` → `Scaffold A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldA` → `Scaffold A`
- `PhaseA` → `Phase A`
- `FastBreakA` → `Fast Break A`
- `MovementCheckSupport` → `Phase A`
- `SimulationC` → `Simulation A`
- `SimulationE` → `Simulation A`
- `SimulationA` → `Simulation A`
- `SimulationF` → `Simulation A`
- `SimulationD` → `Simulation A`
- `SimulationB` → `Simulation A`
- `InventoryA` → `Inventory A`
- `BaritoneA` → `Baritone A`
- `BaritoneB` → `Baritone A`
- `BaritoneC` → `Baritone A`
- `VelocityD` → `Velocity A`
- `VelocityC` → `Velocity A`
- `VelocityA` → `Velocity A`
- `VelocityB` → `Velocity A`
- `GroundSpoofD` → `Ground Spoof A`
- `GroundSpoofC` → `Ground Spoof A`
- `GroundSpoofB` → `Ground Spoof A`
- `GroundSpoofF` → `Ground Spoof A`
- `GroundSpoofE` → `Ground Spoof A`
- `GroundSpoofG` → `Ground Spoof A`
- `HitboxA` → `Hitboxes A`
- `KillAuraE` → `Kill Aura A`
- `KillAuraF` → `Kill Aura A`
- `KillAuraH` → `Kill Aura A`
- `KillAuraG` → `Kill Aura A`
- `KillAuraD` → `Kill Aura A`
- `KillAuraB` → `Kill Aura A`
- `KillAuraC` → `Kill Aura A`
- `CrystalAuraA` → `Kill Aura A`
- `AnchorAuraA` → `Kill Aura A`
- `AutoClickerE` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerA` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `ReachA` → `Reach A`
- `BFlyA` → `Fly A`
- `BReachA` → `Reach A`
- `BSpeedA` → `Speed A`
- `RaycastA` → `Reach A`

### CloudAC

**No new checks detected.**

**Matched with existing Windfall checks:**

- `CheckAbilties` → `Exploit A`

### Arrow

**Missing from Windfall (8 checks):**

- `movement` **Movement** → `windfall.movement.movement`
  - Source: `src/main/java/me/arrow/checks/impl/simulation/Movement.java`
- `movement` **GravityB** → `windfall.movement.gravity b`
  - Source: `src/main/java/me/arrow/checks/impl/movement/fly/GravityB.java`
- `movement` **GravityD** → `windfall.movement.gravity d`
  - Source: `src/main/java/me/arrow/checks/impl/movement/fly/GravityD.java`
  - `transTicks = getLagCompensationTicks()`
  - `DEFAULT_GRAVITY = 0.08D`
  - `AIR_DRAG = 0.9800000190734863D`
- `movement` **GravityA** → `windfall.movement.gravity a`
  - Source: `src/main/java/me/arrow/checks/impl/movement/fly/GravityA.java`
- `movement` **GravityC** → `windfall.movement.gravity c`
  - Source: `src/main/java/me/arrow/checks/impl/movement/fly/GravityC.java`
  - `JUMP_TOL = 0.046D`
  - `G = 0.08D`
  - `DRAG = 0.9800000190734863D`
- `packet` **InteractE** → `windfall.packet.interact e`
  - Source: `src/main/java/me/arrow/checks/impl/misc/interact/InteractE.java`
- `combat` **AimH2** → `windfall.combat.aim h2`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/AimH2.java`
- `combat` **LinearRegression** → `windfall.combat.linear regression`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/aimassistUtil/LinearRegression.java`
  - `n = x.length`
  - `xbar = sumx / n`
  - `ybar = sumy / n`

**Matched with existing Windfall checks:**

- `SpeedB` → `Speed A`
- `SpeedA` → `Speed A`
- `OmniSprintA` → `Sprint A`
- `IllegalMoveB` → `Phase A`
- `IllegalMoveC` → `Illegal Move`
- `FlyB` → `Fly A`
- `FlyA` → `Fly A`
- `ElytraA` → `Elytra A`
- `MotionD` → `Motion A`
- `MotionA` → `Motion A`
- `MotionB` → `Motion A`
- `MotionE` → `Motion A`
- `GroundB` → `Ground Spoof A`
- `GroundA` → `Ground Spoof A`
- `GroundC` → `Ground Spoof A`
- `TimerB` → `Timer A`
- `TimerA` → `Timer A`
- `TimerC` → `Timer A`
- `InventoryA` → `Inventory A`
- `VehicleA` → `Vehicle A`
- `ScaffoldC` → `Scaffold A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldA` → `Scaffold A`
- `InteractA` → `Multi Interact A`
- `InteractD` → `Reach A`
- `InteractC` → `Multi Interact A`
- `HitboxA` → `Hitboxes A`
- `BackTrackA` → `Backtrack A`
- `BackTrackB` → `Backtrack A`
- `KillauraA` → `Kill Aura A`
- `VelocityA` → `Velocity A`
- `VelocityB` → `Velocity A`
- `AimE` → `Aim A`
- `AimA` → `Aim A`
- `AimC` → `Aim A`
- `AimH` → `Aim A`
- `AimB` → `Aim A`
- `AimD` → `Aim A`
- `AimF` → `Aim A`
- `AimG` → `Aim A`
- `AutoClickerH` → `Autoclicker A`
- `MacroB` → `Macro A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerF` → `Autoclicker A`
- `MacroA` → `Macro A`
- `AutoClickerG` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `ReachA` → `Reach A`

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
