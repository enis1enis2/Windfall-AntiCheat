# Windfall Anti-Cheat Monitor Report

**Generated:** 2026-11-17 05:37:29 UTC

---

## Windfall Current Checks

**Total: 60 checks**

### Combat
- `windfall.combat.swordblock` — Sword Block A
- `windfall.combat.killaura` — Kill Aura A
- `windfall.combat.selfinteract` — Self Interact A
- `windfall.combat.criticals` — Criticals A
- `windfall.combat.aim` — Aim A
- `windfall.combat.fastheal` — Fast Heal A
- `windfall.combat.macro` — Macro A
- `windfall.combat.interactcursor` — Interact Cursor
- `windfall.combat.multiinteract` — Multi Interact A
- `windfall.combat.backtrack` — Backtrack A
- `windfall.combat.reach` — Reach A
- `windfall.combat.autoclicker` — Autoclicker A
- `windfall.combat.crystal` — Crystal A
- `windfall.combat.hitboxes` — Hitboxes A

### Movement
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.baritone` — Baritone A
- `windfall.movement.scaffold` — Scaffold A
- `windfall.movement.speed` — Speed A
- `windfall.movement.step` — Step A
- `windfall.movement.scaffoldsupport` — Scaffold Support
- `windfall.movement.multiplace` — Multi Place
- `windfall.movement.wrongbreak` — Wrong Break
- `windfall.movement.motion` — Motion A
- `windfall.movement.sprint` — Sprint B
- `windfall.movement.groundspoof` — Ground Spoof A
- `windfall.movement.invalidplace` — Invalid Place A
- `windfall.movement.positionplace` — Position Place
- `windfall.movement.farbreak` — Far Break A
- `windfall.movement.fastbreak` — Fast Break A
- `windfall.movement.elytra` — Elytra A
- `windfall.movement.gravity` — Gravity
- `windfall.movement.airliquidbreak` — Air Liquid Break
- `windfall.movement.airliquidplace` — Air Liquid Place
- `windfall.movement.rotationplace` — Rotation Place
- `windfall.movement.velocity` — Velocity A
- `windfall.movement.fly` — Fly A
- `windfall.movement.illegalmove` — Illegal Move
- `windfall.movement.noswing` — No Swing A
- `windfall.movement.invalidbreak` — Invalid Break A
- `windfall.movement.noslow` — NoSlow A
- `windfall.movement.phase` — Phase A
- `windfall.movement.rotationbreak` — Rotation Break A
- `windfall.movement.positionbreak` — Position Break
- `windfall.movement.multibreak` — Multi Break
- `windfall.movement.timer` — Timer A
- `windfall.movement.invalidplacecursor` — Invalid Place Cursor
- `windfall.movement.farplace` — Far Place A
- `windfall.movement.simulation` — Simulation A

### Packet
- `windfall.packet.crash` — Crash A
- `windfall.packet.cheststealer` — Chest Stealer A
- `windfall.packet.brand` — Client Brand A
- `windfall.packet.exploit` — Exploit A
- `windfall.packet.chat` — Chat A
- `windfall.packet.sprint` — Sprint A
- `windfall.packet.order` — Packet Order A
- `windfall.packet.transaction` — Transaction A
- `windfall.packet.vehicle` — Vehicle A
- `windfall.packet.bad` — Bad Packets A
- `windfall.packet.creative` — Creative A

---

## Competitor Analysis

### Grim

**Missing from Windfall (1 checks):**

- `movement` **InvalidPlaceFace** → `windfall.movement.invalid place face`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceFace.java`

**Matched with existing Windfall checks:**

- `Post` → `No Swing A`
- `ClientBrand` → `Client Brand A`
- `GroundSpoof` → `Ground Spoof A`
- `Phase` → `Phase A`
- `PacketOrderJ` → `Packet Order A`
- `PacketOrderP` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderF` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderL` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderH` → `Packet Order A`
- `PacketOrderM` → `Packet Order A`
- `PacketOrderI` → `Packet Order A`
- `PacketOrderG` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderK` → `Packet Order A`
- `PacketOrderO` → `Packet Order A`
- `PacketOrderN` → `Packet Order A`
- `VehicleB` → `Vehicle A`
- `VehicleF` → `Vehicle A`
- `VehicleD` → `Vehicle A`
- `VehicleE` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `Baritone` → `Baritone A`
- `CrashI` → `Crash A`
- `CrashF` → `Crash A`
- `CrashA` → `Crash A`
- `CrashB` → `Crash A`
- `CrashD` → `Crash A`
- `CrashC` → `Crash A`
- `CrashG` → `Crash A`
- `CrashH` → `Crash A`
- `CrashE` → `Crash A`
- `DuplicateRotPlace` → `Rotation Place`
- `PositionPlace` → `Position Place`
- `RotationPlace` → `Rotation Place`
- `FarPlace` → `Far Place A`
- `FabricatedPlace` → `Invalid Place A`
- `AirLiquidPlace` → `Air Liquid Place`
- `MultiPlace` → `Multi Place`
- `InvalidPlaceCursor` → `Invalid Place Cursor`
- `TickTimer` → `Timer A`
- `VehicleTimer` → `Timer A`
- `Timer` → `Timer A`
- `TimerLimit` → `Timer A`
- `NegativeTimer` → `Timer A`
- `NoSlow` → `NoSlow A`
- `FlightA` → `Fly A`
- `SelfInteract` → `Self Interact A`
- `MultiInteractA` → `Multi Interact A`
- `MultiInteractB` → `Multi Interact A`
- `Reach` → `Reach A`
- `InvalidInteractCursor` → `Interact Cursor`
- `FarBreak` → `Far Break A`
- `PositionBreakA` → `Position Break`
- `PositionBreakB` → `Position Break`
- `FastBreak` → `Fast Break A`
- `NoSwingBreak` → `No Swing A`
- `RotationBreak` → `Rotation Break A`
- `WrongBreak` → `Wrong Break`
- `MultiBreak` → `Multi Break`
- `AirLiquidBreak` → `Air Liquid Break`
- `InvalidBreak` → `Invalid Break A`
- `ChatB` → `Chat A`
- `ChatA` → `Chat A`
- `ChatD` → `Chat A`
- `ChatC` → `Chat A`
- `SprintG` → `Sprint B`
- `SprintF` → `Sprint B`
- `SprintD` → `Sprint B`
- `SprintB` → `Sprint B`
- `SprintC` → `Sprint B`
- `SprintA` → `Sprint A`
- `SprintE` → `Sprint B`
- `NoFall` → `NoFall A`
- `MultiActionsF` → `Multi Interact A`
- `MultiActionsB` → `Multi Interact A`
- `MultiActionsA` → `Multi Interact A`
- `MultiActionsD` → `Multi Interact A`
- `MultiActionsC` → `Multi Interact A`
- `MultiActionsG` → `Multi Interact A`
- `MultiActionsE` → `Multi Interact A`
- `ElytraH` → `Elytra A`
- `ElytraA` → `Elytra A`
- `ElytraD` → `Elytra A`
- `ElytraC` → `Elytra A`
- `ElytraE` → `Elytra A`
- `ElytraG` → `Elytra A`
- `ElytraI` → `Elytra A`
- `ElytraB` → `Elytra A`
- `ElytraF` → `Elytra A`
- `ExploitA` → `Exploit A`
- `ExploitB` → `Exploit A`
- `AimDuplicateLook` → `Aim A`
- `AimModulo360` → `Aim A`

### TruthfulAC

**Missing from Windfall (24 checks):**

- `packet` **BadPacketU** → `windfall.packet.bad packet u`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketU.java`
- `packet` **BadPacketP** → `windfall.packet.bad packet p`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketP.java`
  - `POST_WINDOW_MS = 80L`
- `packet` **BadPacketT** → `windfall.packet.bad packet t`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketT.java`
  - `WINDOW_MS = 10_000L`
  - `SERVER_WINDOW_MS = 15_000L`
  - `GRACE = 3`
- `packet` **BadPacketV** → `windfall.packet.bad packet v`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketV.java`
- `packet` **BadPacketQ** → `windfall.packet.bad packet q`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketQ.java`
- `packet` **BadPacketY** → `windfall.packet.bad packet y`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketY.java`
- `packet` **BadPacket8** → `windfall.packet.bad packet8`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket8.java`
  - `MAX_DIG_DIST = 6.0D`
- `packet` **BadPacketN** → `windfall.packet.bad packet n`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketN.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacketM** → `windfall.packet.bad packet m`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketM.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacket5** → `windfall.packet.bad packet5`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket5.java`
- `packet` **BadPacketL** → `windfall.packet.bad packet l`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketL.java`
- `packet` **BadPacketZ** → `windfall.packet.bad packet z`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketZ.java`
- `packet` **BadPacketF** → `windfall.packet.bad packet f`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketF.java`
- `packet` **BadPacketR** → `windfall.packet.bad packet r`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketR.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacketB** → `windfall.packet.bad packet b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketB.java`
  - `MAX_FLYING_STREAK = 20`
- `packet` **BadPacketO** → `windfall.packet.bad packet o`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketO.java`
- `packet` **BadPacket6** → `windfall.packet.bad packet6`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket6.java`
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
- `movement` **ScaffoldRotation** → `windfall.movement.scaffold rotation`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldRotation.java`
  - `EXPANSION = 0.10D`
  - `FLAG_BUFFER = 6.0D`
  - `targetX = clicked.getX() + face.getModX()`
- `movement` **ScaffoldMotion** → `windfall.movement.scaffold motion`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldMotion.java`
  - `HISTORY_TICKS = 24`
  - `WINDOW_TICKS = 16`
  - `FLAG_BUFFER = 4.0D`
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

**Matched with existing Windfall checks:**

- `BadPacketK` → `Bad Packets A`
- `BadPacketG` → `Bad Packets A`
- `BadPacketD` → `Bad Packets A`
- `BadPacketE` → `Bad Packets A`
- `BadPacketH` → `Bad Packets A`
- `BadPacketA` → `Bad Packets A`
- `BadPacketJ` → `Bad Packets A`
- `BadPacketI` → `Bad Packets A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `TimerA` → `Timer A`
- `CrasherA` → `Crash A`
- `InvalidA` → `Invalid Place A`
- `SprintB` → `Sprint B`
- `SprintA` → `Sprint A`
- `BFlyA` → `Fly A`
- `BSpeedA` → `Speed A`
- `BReachA` → `Reach A`
- `MovementCheckSupport` → `Phase A`
- `BaritoneA` → `Baritone A`
- `BaritoneC` → `Baritone A`
- `BaritoneB` → `Baritone A`
- `GroundSpoofD` → `Ground Spoof A`
- `GroundSpoofG` → `Ground Spoof A`
- `GroundSpoofF` → `Ground Spoof A`
- `GroundSpoofE` → `Ground Spoof A`
- `GroundSpoofC` → `Ground Spoof A`
- `GroundSpoofB` → `Ground Spoof A`
- `VelocityD` → `Velocity A`
- `VelocityA` → `Velocity A`
- `VelocityC` → `Velocity A`
- `VelocityB` → `Velocity A`
- `InventoryA` → `Inventory A`
- `Simulation` → `Simulation A`
- `KillAuraF` → `Kill Aura A`
- `KillAuraE` → `Kill Aura A`
- `KillAuraB` → `Kill Aura A`
- `KillAuraH` → `Kill Aura A`
- `KillAuraC` → `Kill Aura A`
- `KillAuraG` → `Kill Aura A`
- `KillAuraD` → `Kill Aura A`
- `CrystalAuraA` → `Kill Aura A`
- `HitboxA` → `Hitboxes A`
- `AnchorAuraA` → `Kill Aura A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerE` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerA` → `Autoclicker A`
- `AimC` → `Aim A`
- `AimA` → `Aim A`
- `AimB` → `Aim A`
- `ReachA` → `Reach A`
- `RaycastA` → `Reach A`
- `PhaseA` → `Phase A`
- `FastBreakA` → `Fast Break A`

### Arrow

**Missing from Windfall (5 checks):**

- `movement` **FastBreakTracker** → `windfall.movement.fast break tracker`
  - Source: `src/main/java/me/arrow/checks/impl/misc/interact/FastBreakTracker.java`
  - `packetTime = System.nanoTime()`
  - `packetWorldName = worldName`
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

- `VehicleB` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `VehicleC` → `Vehicle A`
- `TimerC` → `Timer A`
- `TimerB` → `Timer A`
- `TimerA` → `Timer A`
- `ExploitA` → `Exploit A`
- `ExploitB` → `Exploit A`
- `InventoryA` → `Inventory A`
- `FastBreakB` → `Fast Break A`
- `InteractC` → `Self Interact A`
- `FastBreakA` → `Fast Break A`
- `InteractA` → `Self Interact A`
- `InteractD` → `Reach A`
- `ScaffoldD` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldA` → `Scaffold A`
- `SpeedB` → `Speed A`
- `OmniSprintA` → `Sprint A`
- `SpeedA` → `Speed A`
- `ElytraA` → `Elytra A`
- `FlyB` → `Fly A`
- `GravityD` → `Gravity`
- `GravityA` → `Gravity`
- `GravityC` → `Gravity`
- `GravityB` → `Gravity`
- `FlyA` → `Fly A`
- `ElytraB` → `Elytra A`
- `GroundB` → `Ground Spoof A`
- `GroundC` → `Ground Spoof A`
- `GroundA` → `Ground Spoof A`
- `IllegalMoveC` → `Illegal Move`
- `IllegalMoveB` → `Phase A`
- `MotionE` → `Motion A`
- `MotionD` → `Motion A`
- `MotionA` → `Motion A`
- `MotionB` → `Motion A`
- `AimC` → `Aim A`
- `AimA` → `Aim A`
- `AimE` → `Aim A`
- `AimG` → `Aim A`
- `AimD` → `Aim A`
- `AimH` → `Aim A`
- `AimB` → `Aim A`
- `AimF` → `Aim A`
- `KillauraA` → `Kill Aura A`
- `HitboxA` → `Hitboxes A`
- `VelocityA` → `Velocity A`
- `VelocityB` → `Velocity A`
- `BackTrackA` → `Backtrack A`
- `BackTrackB` → `Backtrack A`
- `AutoClickerC` → `Autoclicker A`
- `MacroA` → `Macro A`
- `MacroB` → `Macro A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerF` → `Autoclicker A`
- `AutoClickerH` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerG` → `Autoclicker A`
- `ReachA` → `Reach A`

---

## Summary

- Windfall has **60 checks**
- Found **30 new checks** across competitors that Windfall doesn't have

## Recommendations

1. Review generated skeleton files in `src/main/java/io/windfall/anticheat/core/check/impl/`
2. Implement detection logic based on competitor reference
3. Tune thresholds and buffer values for each check
4. Register new checks in `CheckManager.java`
5. Add config entries to `config.yml`
6. Test on live server before enabling punishable mode
