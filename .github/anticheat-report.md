# Windfall Anti-Cheat Monitor Report

**Generated:** 2026-11-28 05:25:07 UTC

---

## Windfall Current Checks

**Total: 60 checks**

### Combat
- `windfall.combat.selfinteract` — Self Interact A
- `windfall.combat.criticals` — Criticals A
- `windfall.combat.backtrack` — Backtrack A
- `windfall.combat.macro` — Macro A
- `windfall.combat.hitboxes` — Hitboxes A
- `windfall.combat.multiinteract` — Multi Interact A
- `windfall.combat.crystal` — Crystal A
- `windfall.combat.fastheal` — Fast Heal A
- `windfall.combat.killaura` — Kill Aura A
- `windfall.combat.reach` — Reach A
- `windfall.combat.interactcursor` — Interact Cursor
- `windfall.combat.swordblock` — Sword Block A
- `windfall.combat.autoclicker` — Autoclicker A
- `windfall.combat.aim` — Aim A

### Movement
- `windfall.movement.invalidplace` — Invalid Place A
- `windfall.movement.timer` — Timer A
- `windfall.movement.motion` — Motion A
- `windfall.movement.positionbreak` — Position Break
- `windfall.movement.scaffold` — Scaffold A
- `windfall.movement.invalidplacecursor` — Invalid Place Cursor
- `windfall.movement.speed` — Speed A
- `windfall.movement.phase` — Phase A
- `windfall.movement.farplace` — Far Place A
- `windfall.movement.step` — Step A
- `windfall.movement.illegalmove` — Illegal Move
- `windfall.movement.elytra` — Elytra A
- `windfall.movement.velocity` — Velocity A
- `windfall.movement.airliquidplace` — Air Liquid Place
- `windfall.movement.farbreak` — Far Break A
- `windfall.movement.sprint` — Sprint B
- `windfall.movement.multiplace` — Multi Place
- `windfall.movement.multibreak` — Multi Break
- `windfall.movement.fastbreak` — Fast Break A
- `windfall.movement.simulation` — Simulation A
- `windfall.movement.baritone` — Baritone A
- `windfall.movement.positionplace` — Position Place
- `windfall.movement.wrongbreak` — Wrong Break
- `windfall.movement.rotationplace` — Rotation Place
- `windfall.movement.groundspoof` — Ground Spoof A
- `windfall.movement.rotationbreak` — Rotation Break A
- `windfall.movement.scaffoldsupport` — Scaffold Support
- `windfall.movement.noswing` — No Swing A
- `windfall.movement.fly` — Fly A
- `windfall.movement.noslow` — NoSlow A
- `windfall.movement.gravity` — Gravity
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.airliquidbreak` — Air Liquid Break
- `windfall.movement.invalidbreak` — Invalid Break A

### Packet
- `windfall.packet.crash` — Crash A
- `windfall.packet.chat` — Chat A
- `windfall.packet.creative` — Creative A
- `windfall.packet.exploit` — Exploit A
- `windfall.packet.sprint` — Sprint A
- `windfall.packet.vehicle` — Vehicle A
- `windfall.packet.brand` — Client Brand A
- `windfall.packet.cheststealer` — Chest Stealer A
- `windfall.packet.transaction` — Transaction A
- `windfall.packet.bad` — Bad Packets A
- `windfall.packet.order` — Packet Order A

---

## Competitor Analysis

### Grim

**Missing from Windfall (1 checks):**

- `movement` **InvalidPlaceFace** → `windfall.movement.invalid place face`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceFace.java`

**Matched with existing Windfall checks:**

- `VehicleE` → `Vehicle A`
- `VehicleF` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `VehicleD` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `Phase` → `Phase A`
- `GroundSpoof` → `Ground Spoof A`
- `FlightA` → `Fly A`
- `ElytraI` → `Elytra A`
- `ElytraB` → `Elytra A`
- `ElytraG` → `Elytra A`
- `ElytraC` → `Elytra A`
- `ElytraE` → `Elytra A`
- `ElytraD` → `Elytra A`
- `ElytraF` → `Elytra A`
- `ElytraH` → `Elytra A`
- `ElytraA` → `Elytra A`
- `MultiActionsC` → `Multi Interact A`
- `MultiActionsA` → `Multi Interact A`
- `MultiActionsG` → `Multi Interact A`
- `MultiActionsE` → `Multi Interact A`
- `MultiActionsD` → `Multi Interact A`
- `MultiActionsF` → `Multi Interact A`
- `MultiActionsB` → `Multi Interact A`
- `NoFall` → `NoFall A`
- `CrashI` → `Crash A`
- `CrashA` → `Crash A`
- `CrashD` → `Crash A`
- `CrashF` → `Crash A`
- `CrashH` → `Crash A`
- `CrashC` → `Crash A`
- `CrashE` → `Crash A`
- `CrashG` → `Crash A`
- `CrashB` → `Crash A`
- `ClientBrand` → `Client Brand A`
- `Post` → `No Swing A`
- `FastBreak` → `Fast Break A`
- `MultiBreak` → `Multi Break`
- `AirLiquidBreak` → `Air Liquid Break`
- `WrongBreak` → `Wrong Break`
- `FarBreak` → `Far Break A`
- `PositionBreakA` → `Position Break`
- `NoSwingBreak` → `No Swing A`
- `InvalidBreak` → `Invalid Break A`
- `RotationBreak` → `Rotation Break A`
- `PositionBreakB` → `Position Break`
- `RotationPlace` → `Rotation Place`
- `PositionPlace` → `Position Place`
- `AirLiquidPlace` → `Air Liquid Place`
- `FabricatedPlace` → `Invalid Place A`
- `InvalidPlaceCursor` → `Invalid Place Cursor`
- `MultiPlace` → `Multi Place`
- `FarPlace` → `Far Place A`
- `DuplicateRotPlace` → `Rotation Place`
- `Baritone` → `Baritone A`
- `TickTimer` → `Timer A`
- `NegativeTimer` → `Timer A`
- `TimerLimit` → `Timer A`
- `VehicleTimer` → `Timer A`
- `Timer` → `Timer A`
- `ChatB` → `Chat A`
- `ChatA` → `Chat A`
- `ChatD` → `Chat A`
- `ChatC` → `Chat A`
- `ExploitB` → `Exploit A`
- `ExploitA` → `Exploit A`
- `AimDuplicateLook` → `Aim A`
- `AimModulo360` → `Aim A`
- `PacketOrderN` → `Packet Order A`
- `PacketOrderJ` → `Packet Order A`
- `PacketOrderK` → `Packet Order A`
- `PacketOrderH` → `Packet Order A`
- `PacketOrderO` → `Packet Order A`
- `PacketOrderF` → `Packet Order A`
- `PacketOrderL` → `Packet Order A`
- `PacketOrderG` → `Packet Order A`
- `PacketOrderM` → `Packet Order A`
- `PacketOrderI` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderP` → `Packet Order A`
- `Reach` → `Reach A`
- `MultiInteractA` → `Multi Interact A`
- `MultiInteractB` → `Multi Interact A`
- `InvalidInteractCursor` → `Interact Cursor`
- `SelfInteract` → `Self Interact A`
- `SprintE` → `Sprint B`
- `SprintF` → `Sprint B`
- `SprintD` → `Sprint B`
- `SprintC` → `Sprint B`
- `SprintB` → `Sprint B`
- `SprintG` → `Sprint B`
- `SprintA` → `Sprint A`
- `NoSlow` → `NoSlow A`

### TruthfulAC

**Missing from Windfall (24 checks):**

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
- `movement` **ScaffoldFace** → `windfall.movement.scaffold face`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldFace.java`
  - `FLAG_BUFFER = 5.0D`
  - `REPEAT_TICKS = 20`
  - `clickedId = data.getWorldCache().getBlockId(clicked.getX(), clicked.getY(), clicked.getZ())`
- `movement` **ScaffoldReach** → `windfall.movement.scaffold reach`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldReach.java`
  - `JITTER = 0.25D`
  - `FLAG_BUFFER = 4.0D`
  - `faceX = clicked.getX() + 0.5D + face.getModX() * 0.5D`
- `packet` **BadPacketF** → `windfall.packet.bad packet f`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketF.java`
- `packet` **BadPacket8** → `windfall.packet.bad packet8`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket8.java`
  - `MAX_DIG_DIST = 6.0D`
- `packet` **BadPacket5** → `windfall.packet.bad packet5`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket5.java`
- `packet` **BadPacket6** → `windfall.packet.bad packet6`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket6.java`
- `packet` **BadPacketB** → `windfall.packet.bad packet b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketB.java`
  - `MAX_FLYING_STREAK = 20`
- `packet` **BadPacketN** → `windfall.packet.bad packet n`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketN.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacketT** → `windfall.packet.bad packet t`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketT.java`
  - `WINDOW_MS = 10_000L`
  - `SERVER_WINDOW_MS = 15_000L`
  - `GRACE = 3`
- `packet` **BadPacketY** → `windfall.packet.bad packet y`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketY.java`
- `packet` **BadPacketQ** → `windfall.packet.bad packet q`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketQ.java`
- `packet` **BadPacketU** → `windfall.packet.bad packet u`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketU.java`
- `packet` **BadPacketV** → `windfall.packet.bad packet v`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketV.java`
- `packet` **BadPacketM** → `windfall.packet.bad packet m`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketM.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacketZ** → `windfall.packet.bad packet z`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketZ.java`
- `packet` **BadPacketO** → `windfall.packet.bad packet o`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketO.java`
- `packet` **BadPacketP** → `windfall.packet.bad packet p`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketP.java`
  - `POST_WINDOW_MS = 80L`
- `packet` **BadPacketR** → `windfall.packet.bad packet r`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketR.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacketL** → `windfall.packet.bad packet l`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketL.java`
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

- `BSpeedA` → `Speed A`
- `BFlyA` → `Fly A`
- `BReachA` → `Reach A`
- `PhaseA` → `Phase A`
- `FastBreakA` → `Fast Break A`
- `BadPacketG` → `Bad Packets A`
- `BadPacketE` → `Bad Packets A`
- `BadPacketK` → `Bad Packets A`
- `BadPacketH` → `Bad Packets A`
- `BadPacketA` → `Bad Packets A`
- `BadPacketJ` → `Bad Packets A`
- `BadPacketI` → `Bad Packets A`
- `BadPacketD` → `Bad Packets A`
- `InvalidA` → `Invalid Place A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `TimerA` → `Timer A`
- `CrasherA` → `Crash A`
- `SprintB` → `Sprint B`
- `SprintA` → `Sprint A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerA` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerE` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `KillAuraD` → `Kill Aura A`
- `KillAuraF` → `Kill Aura A`
- `KillAuraG` → `Kill Aura A`
- `KillAuraE` → `Kill Aura A`
- `KillAuraB` → `Kill Aura A`
- `KillAuraH` → `Kill Aura A`
- `KillAuraC` → `Kill Aura A`
- `AnchorAuraA` → `Kill Aura A`
- `ReachA` → `Reach A`
- `HitboxA` → `Hitboxes A`
- `CrystalAuraA` → `Kill Aura A`
- `AimB` → `Aim A`
- `AimA` → `Aim A`
- `AimC` → `Aim A`
- `RaycastA` → `Reach A`
- `MovementCheckSupport` → `Phase A`
- `BaritoneA` → `Baritone A`
- `BaritoneB` → `Baritone A`
- `BaritoneC` → `Baritone A`
- `VelocityB` → `Velocity A`
- `VelocityD` → `Velocity A`
- `VelocityC` → `Velocity A`
- `VelocityA` → `Velocity A`
- `InventoryA` → `Inventory A`
- `Simulation` → `Simulation A`
- `GroundSpoofF` → `Ground Spoof A`
- `GroundSpoofG` → `Ground Spoof A`
- `GroundSpoofD` → `Ground Spoof A`
- `GroundSpoofE` → `Ground Spoof A`
- `GroundSpoofC` → `Ground Spoof A`
- `GroundSpoofB` → `Ground Spoof A`

### Arrow

**Missing from Windfall (6 checks):**

- `packet` **VehicleSupport** → `windfall.packet.vehicle support`
  - Source: `src/main/java/me/arrow/checks/impl/misc/vehicle/VehicleSupport.java`
  - `sinceIceTicks = 1000`
- `movement` **FastBreakTracker** → `windfall.movement.fast break tracker`
  - Source: `src/main/java/me/arrow/checks/impl/misc/interact/FastBreakTracker.java`
  - `packetTime = System.nanoTime()`
  - `packetWorldName = worldName`
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

- `VehicleA` → `Vehicle A`
- `VehicleC` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `TimerB` → `Timer A`
- `TimerA` → `Timer A`
- `TimerC` → `Timer A`
- `FastBreakB` → `Fast Break A`
- `InteractC` → `Self Interact A`
- `FastBreakA` → `Fast Break A`
- `InteractD` → `Reach A`
- `InteractA` → `Self Interact A`
- `InventoryA` → `Inventory A`
- `ScaffoldD` → `Scaffold A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `ScaffoldA` → `Scaffold A`
- `ExploitB` → `Exploit A`
- `ExploitA` → `Exploit A`
- `AutoClickerH` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `MacroB` → `Macro A`
- `AutoClickerG` → `Autoclicker A`
- `MacroA` → `Macro A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerF` → `Autoclicker A`
- `KillauraA` → `Kill Aura A`
- `ReachA` → `Reach A`
- `HitboxA` → `Hitboxes A`
- `BackTrackA` → `Backtrack A`
- `BackTrackB` → `Backtrack A`
- `AimB` → `Aim A`
- `AimG` → `Aim A`
- `AimD` → `Aim A`
- `AimE` → `Aim A`
- `AimA` → `Aim A`
- `AimC` → `Aim A`
- `AimH` → `Aim A`
- `AimF` → `Aim A`
- `VelocityB` → `Velocity A`
- `VelocityA` → `Velocity A`
- `GravityC` → `Gravity`
- `ElytraB` → `Elytra A`
- `FlyA` → `Fly A`
- `GravityB` → `Gravity`
- `FlyB` → `Fly A`
- `GravityD` → `Gravity`
- `GravityA` → `Gravity`
- `ElytraA` → `Elytra A`
- `OmniSprintA` → `Sprint A`
- `SpeedB` → `Speed A`
- `SpeedA` → `Speed A`
- `GroundB` → `Ground Spoof A`
- `GroundC` → `Ground Spoof A`
- `GroundA` → `Ground Spoof A`
- `MotionE` → `Motion A`
- `MotionA` → `Motion A`
- `MotionB` → `Motion A`
- `MotionD` → `Motion A`
- `IllegalMoveC` → `Illegal Move`
- `IllegalMoveB` → `Phase A`

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
