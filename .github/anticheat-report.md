# Windfall Anti-Cheat Monitor Report

**Generated:** 2026-11-23 04:36:43 UTC

---

## Windfall Current Checks

**Total: 60 checks**

### Combat
- `windfall.combat.multiinteract` — Multi Interact A
- `windfall.combat.reach` — Reach A
- `windfall.combat.swordblock` — Sword Block A
- `windfall.combat.hitboxes` — Hitboxes A
- `windfall.combat.fastheal` — Fast Heal A
- `windfall.combat.autoclicker` — Autoclicker A
- `windfall.combat.crystal` — Crystal A
- `windfall.combat.backtrack` — Backtrack A
- `windfall.combat.aim` — Aim A
- `windfall.combat.selfinteract` — Self Interact A
- `windfall.combat.criticals` — Criticals A
- `windfall.combat.interactcursor` — Interact Cursor
- `windfall.combat.macro` — Macro A
- `windfall.combat.killaura` — Kill Aura A

### Movement
- `windfall.movement.illegalmove` — Illegal Move
- `windfall.movement.phase` — Phase A
- `windfall.movement.noswing` — No Swing A
- `windfall.movement.positionbreak` — Position Break
- `windfall.movement.invalidplace` — Invalid Place A
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.velocity` — Velocity A
- `windfall.movement.fly` — Fly A
- `windfall.movement.invalidplacecursor` — Invalid Place Cursor
- `windfall.movement.speed` — Speed A
- `windfall.movement.invalidbreak` — Invalid Break A
- `windfall.movement.step` — Step A
- `windfall.movement.scaffoldsupport` — Scaffold Support
- `windfall.movement.farbreak` — Far Break A
- `windfall.movement.gravity` — Gravity
- `windfall.movement.elytra` — Elytra A
- `windfall.movement.wrongbreak` — Wrong Break
- `windfall.movement.rotationplace` — Rotation Place
- `windfall.movement.scaffold` — Scaffold A
- `windfall.movement.multiplace` — Multi Place
- `windfall.movement.fastbreak` — Fast Break A
- `windfall.movement.groundspoof` — Ground Spoof A
- `windfall.movement.airliquidbreak` — Air Liquid Break
- `windfall.movement.farplace` — Far Place A
- `windfall.movement.rotationbreak` — Rotation Break A
- `windfall.movement.multibreak` — Multi Break
- `windfall.movement.timer` — Timer A
- `windfall.movement.simulation` — Simulation A
- `windfall.movement.airliquidplace` — Air Liquid Place
- `windfall.movement.sprint` — Sprint B
- `windfall.movement.motion` — Motion A
- `windfall.movement.noslow` — NoSlow A
- `windfall.movement.positionplace` — Position Place
- `windfall.movement.baritone` — Baritone A

### Packet
- `windfall.packet.sprint` — Sprint A
- `windfall.packet.vehicle` — Vehicle A
- `windfall.packet.brand` — Client Brand A
- `windfall.packet.transaction` — Transaction A
- `windfall.packet.bad` — Bad Packets A
- `windfall.packet.crash` — Crash A
- `windfall.packet.creative` — Creative A
- `windfall.packet.cheststealer` — Chest Stealer A
- `windfall.packet.exploit` — Exploit A
- `windfall.packet.order` — Packet Order A
- `windfall.packet.chat` — Chat A

---

## Competitor Analysis

### Grim

**Missing from Windfall (1 checks):**

- `movement` **InvalidPlaceFace** → `windfall.movement.invalid place face`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceFace.java`

**Matched with existing Windfall checks:**

- `CrashC` → `Crash A`
- `CrashF` → `Crash A`
- `CrashB` → `Crash A`
- `CrashG` → `Crash A`
- `CrashH` → `Crash A`
- `CrashI` → `Crash A`
- `CrashD` → `Crash A`
- `CrashA` → `Crash A`
- `CrashE` → `Crash A`
- `GroundSpoof` → `Ground Spoof A`
- `Phase` → `Phase A`
- `ClientBrand` → `Client Brand A`
- `Post` → `No Swing A`
- `PacketOrderP` → `Packet Order A`
- `PacketOrderM` → `Packet Order A`
- `PacketOrderN` → `Packet Order A`
- `PacketOrderL` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderH` → `Packet Order A`
- `PacketOrderG` → `Packet Order A`
- `PacketOrderI` → `Packet Order A`
- `PacketOrderO` → `Packet Order A`
- `PacketOrderK` → `Packet Order A`
- `PacketOrderJ` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderF` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `NegativeTimer` → `Timer A`
- `Timer` → `Timer A`
- `TickTimer` → `Timer A`
- `VehicleTimer` → `Timer A`
- `TimerLimit` → `Timer A`
- `NoSlow` → `NoSlow A`
- `Baritone` → `Baritone A`
- `NoFall` → `NoFall A`
- `MultiActionsC` → `Multi Interact A`
- `MultiActionsE` → `Multi Interact A`
- `MultiActionsF` → `Multi Interact A`
- `MultiActionsA` → `Multi Interact A`
- `MultiActionsD` → `Multi Interact A`
- `MultiActionsG` → `Multi Interact A`
- `MultiActionsB` → `Multi Interact A`
- `FlightA` → `Fly A`
- `VehicleF` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `VehicleE` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `VehicleD` → `Vehicle A`
- `SprintF` → `Sprint A`
- `SprintA` → `Sprint A`
- `SprintE` → `Sprint A`
- `SprintG` → `Sprint A`
- `SprintD` → `Sprint A`
- `SprintB` → `Sprint B`
- `SprintC` → `Sprint A`
- `FabricatedPlace` → `Invalid Place A`
- `PositionPlace` → `Position Place`
- `DuplicateRotPlace` → `Rotation Place`
- `MultiPlace` → `Multi Place`
- `RotationPlace` → `Rotation Place`
- `FarPlace` → `Far Place A`
- `InvalidPlaceCursor` → `Invalid Place Cursor`
- `AirLiquidPlace` → `Air Liquid Place`
- `SelfInteract` → `Self Interact A`
- `MultiInteractA` → `Multi Interact A`
- `Reach` → `Reach A`
- `MultiInteractB` → `Multi Interact A`
- `InvalidInteractCursor` → `Interact Cursor`
- `ExploitB` → `Exploit A`
- `ExploitA` → `Exploit A`
- `ElytraH` → `Elytra A`
- `ElytraD` → `Elytra A`
- `ElytraG` → `Elytra A`
- `ElytraE` → `Elytra A`
- `ElytraF` → `Elytra A`
- `ElytraI` → `Elytra A`
- `ElytraC` → `Elytra A`
- `ElytraA` → `Elytra A`
- `ElytraB` → `Elytra A`
- `WrongBreak` → `Wrong Break`
- `MultiBreak` → `Multi Break`
- `FarBreak` → `Far Break A`
- `RotationBreak` → `Rotation Break A`
- `AirLiquidBreak` → `Air Liquid Break`
- `InvalidBreak` → `Invalid Break A`
- `FastBreak` → `Fast Break A`
- `NoSwingBreak` → `No Swing A`
- `PositionBreakA` → `Position Break`
- `PositionBreakB` → `Position Break`
- `AimDuplicateLook` → `Aim A`
- `AimModulo360` → `Aim A`
- `ChatC` → `Chat A`
- `ChatA` → `Chat A`
- `ChatD` → `Chat A`
- `ChatB` → `Chat A`

### TruthfulAC

**Missing from Windfall (24 checks):**

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
- `packet` **BadPacket6** → `windfall.packet.bad packet6`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket6.java`
- `packet` **BadPacketY** → `windfall.packet.bad packet y`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketY.java`
- `packet` **BadPacket8** → `windfall.packet.bad packet8`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket8.java`
  - `MAX_DIG_DIST = 6.0D`
- `packet` **BadPacketB** → `windfall.packet.bad packet b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketB.java`
  - `MAX_FLYING_STREAK = 20`
- `packet` **BadPacketL** → `windfall.packet.bad packet l`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketL.java`
- `packet` **BadPacketO** → `windfall.packet.bad packet o`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketO.java`
- `packet` **BadPacketM** → `windfall.packet.bad packet m`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketM.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacketQ** → `windfall.packet.bad packet q`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketQ.java`
- `packet` **BadPacketR** → `windfall.packet.bad packet r`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketR.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacket5** → `windfall.packet.bad packet5`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket5.java`
- `packet` **BadPacketV** → `windfall.packet.bad packet v`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketV.java`
- `packet` **BadPacketU** → `windfall.packet.bad packet u`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketU.java`
- `packet` **BadPacketP** → `windfall.packet.bad packet p`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketP.java`
  - `POST_WINDOW_MS = 80L`
- `packet` **BadPacketZ** → `windfall.packet.bad packet z`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketZ.java`
- `packet` **BadPacketT** → `windfall.packet.bad packet t`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketT.java`
  - `WINDOW_MS = 10_000L`
  - `SERVER_WINDOW_MS = 15_000L`
  - `GRACE = 3`
- `packet` **BadPacketF** → `windfall.packet.bad packet f`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketF.java`
- `packet` **BadPacketN** → `windfall.packet.bad packet n`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketN.java`
  - `POST_WINDOW_MS = 55L`
- `movement` **ScaffoldReach** → `windfall.movement.scaffold reach`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldReach.java`
  - `JITTER = 0.25D`
  - `FLAG_BUFFER = 4.0D`
  - `faceX = clicked.getX() + 0.5D + face.getModX() * 0.5D`
- `movement` **ScaffoldFace** → `windfall.movement.scaffold face`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldFace.java`
  - `FLAG_BUFFER = 5.0D`
  - `REPEAT_TICKS = 20`
  - `clickedId = data.getWorldCache().getBlockId(clicked.getX(), clicked.getY(), clicked.getZ())`
- `movement` **ScaffoldMotion** → `windfall.movement.scaffold motion`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldMotion.java`
  - `HISTORY_TICKS = 24`
  - `WINDOW_TICKS = 16`
  - `FLAG_BUFFER = 4.0D`
- `movement` **ScaffoldRotation** → `windfall.movement.scaffold rotation`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldRotation.java`
  - `EXPANSION = 0.10D`
  - `FLAG_BUFFER = 6.0D`
  - `targetX = clicked.getX() + face.getModX()`

**Matched with existing Windfall checks:**

- `MovementCheckSupport` → `Phase A`
- `BaritoneB` → `Baritone A`
- `BaritoneC` → `Baritone A`
- `BaritoneA` → `Baritone A`
- `Simulation` → `Simulation A`
- `VelocityB` → `Velocity A`
- `VelocityD` → `Velocity A`
- `VelocityC` → `Velocity A`
- `VelocityA` → `Velocity A`
- `GroundSpoofG` → `Ground Spoof A`
- `GroundSpoofC` → `Ground Spoof A`
- `GroundSpoofE` → `Ground Spoof A`
- `GroundSpoofD` → `Ground Spoof A`
- `GroundSpoofB` → `Ground Spoof A`
- `GroundSpoofF` → `Ground Spoof A`
- `InventoryA` → `Inventory A`
- `BSpeedA` → `Speed A`
- `BFlyA` → `Fly A`
- `BReachA` → `Reach A`
- `ReachA` → `Reach A`
- `CrystalAuraA` → `Kill Aura A`
- `HitboxA` → `Hitboxes A`
- `AnchorAuraA` → `Kill Aura A`
- `KillAuraC` → `Kill Aura A`
- `KillAuraD` → `Kill Aura A`
- `KillAuraG` → `Kill Aura A`
- `KillAuraF` → `Kill Aura A`
- `KillAuraH` → `Kill Aura A`
- `KillAuraB` → `Kill Aura A`
- `KillAuraE` → `Kill Aura A`
- `AutoClickerE` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerA` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AimC` → `Aim A`
- `AimA` → `Aim A`
- `AimB` → `Aim A`
- `BadPacketD` → `Bad Packets A`
- `BadPacketA` → `Bad Packets A`
- `BadPacketH` → `Bad Packets A`
- `BadPacketG` → `Bad Packets A`
- `BadPacketK` → `Bad Packets A`
- `BadPacketE` → `Bad Packets A`
- `BadPacketJ` → `Bad Packets A`
- `BadPacketI` → `Bad Packets A`
- `TimerA` → `Timer A`
- `InvalidA` → `Invalid Place A`
- `SprintA` → `Sprint A`
- `SprintB` → `Sprint B`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `CrasherA` → `Crash A`
- `RaycastA` → `Reach A`
- `FastBreakA` → `Fast Break A`
- `PhaseA` → `Phase A`

### Arrow

**Missing from Windfall (6 checks):**

- `packet` **VehicleSupport** → `windfall.packet.vehicle support`
  - Source: `src/main/java/me/arrow/checks/impl/misc/vehicle/VehicleSupport.java`
  - `sinceIceTicks = 1000`
- `movement` **FastBreakTracker** → `windfall.movement.fast break tracker`
  - Source: `src/main/java/me/arrow/checks/impl/misc/interact/FastBreakTracker.java`
  - `packetTime = System.nanoTime()`
  - `packetWorldName = worldName`
- `movement` **Movement** → `windfall.movement.movement`
  - Source: `src/main/java/me/arrow/checks/impl/simulation/Movement.java`
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

**Matched with existing Windfall checks:**

- `TimerB` → `Timer A`
- `TimerA` → `Timer A`
- `TimerC` → `Timer A`
- `ExploitB` → `Exploit A`
- `ExploitA` → `Exploit A`
- `VehicleC` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `FastBreakB` → `Fast Break A`
- `FastBreakA` → `Fast Break A`
- `InteractD` → `Reach A`
- `InteractA` → `Multi Interact A`
- `InteractC` → `Multi Interact A`
- `ScaffoldC` → `Scaffold A`
- `ScaffoldD` → `Scaffold A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldA` → `Scaffold A`
- `InventoryA` → `Inventory A`
- `MotionB` → `Motion A`
- `MotionE` → `Motion A`
- `MotionA` → `Motion A`
- `MotionD` → `Motion A`
- `OmniSprintA` → `Sprint A`
- `SpeedB` → `Speed A`
- `SpeedA` → `Speed A`
- `GroundC` → `Ground Spoof A`
- `GroundA` → `Ground Spoof A`
- `GroundB` → `Ground Spoof A`
- `GravityB` → `Gravity`
- `FlyA` → `Fly A`
- `GravityD` → `Gravity`
- `FlyB` → `Fly A`
- `GravityA` → `Gravity`
- `ElytraA` → `Elytra A`
- `ElytraB` → `Elytra A`
- `GravityC` → `Gravity`
- `IllegalMoveB` → `Phase A`
- `IllegalMoveC` → `Illegal Move`
- `BackTrackB` → `Backtrack A`
- `BackTrackA` → `Backtrack A`
- `AimF` → `Aim A`
- `AimC` → `Aim A`
- `AimA` → `Aim A`
- `AimB` → `Aim A`
- `AimE` → `Aim A`
- `AimH` → `Aim A`
- `AimG` → `Aim A`
- `AimD` → `Aim A`
- `VelocityB` → `Velocity A`
- `VelocityA` → `Velocity A`
- `ReachA` → `Reach A`
- `HitboxA` → `Hitboxes A`
- `KillauraA` → `Kill Aura A`
- `AutoClickerH` → `Autoclicker A`
- `AutoClickerG` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `MacroA` → `Macro A`
- `MacroB` → `Macro A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerF` → `Autoclicker A`

---

## Summary

- Windfall has **60 checks**
- Found **31 new checks** across competitors that Windfall doesn't have

## Recommendations

1. Review generated skeleton files in `src/main/java/io/windfall/anticheat/core/check/impl/`
2. Implement detection logic based on competitor reference
3. Tune thresholds and buffer values for each check
4. Register new checks in `CheckManager.java`
5. Add config entries to `config.yml`
6. Test on live server before enabling punishable mode
