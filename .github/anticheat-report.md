# Windfall Anti-Cheat Monitor Report

**Generated:** 2026-11-08 03:11:28 UTC

---

## Windfall Current Checks

**Total: 60 checks**

### Combat
- `windfall.combat.reach` — Reach A
- `windfall.combat.interactcursor` — Interact Cursor
- `windfall.combat.multiinteract` — Multi Interact A
- `windfall.combat.fastheal` — Fast Heal A
- `windfall.combat.swordblock` — Sword Block A
- `windfall.combat.killaura` — Kill Aura A
- `windfall.combat.macro` — Macro A
- `windfall.combat.backtrack` — Backtrack A
- `windfall.combat.hitboxes` — Hitboxes A
- `windfall.combat.selfinteract` — Self Interact A
- `windfall.combat.aim` — Aim A
- `windfall.combat.autoclicker` — Autoclicker A
- `windfall.combat.criticals` — Criticals A
- `windfall.combat.crystal` — Crystal A

### Movement
- `windfall.movement.positionbreak` — Position Break
- `windfall.movement.baritone` — Baritone A
- `windfall.movement.wrongbreak` — Wrong Break
- `windfall.movement.fastbreak` — Fast Break A
- `windfall.movement.step` — Step A
- `windfall.movement.groundspoof` — Ground Spoof A
- `windfall.movement.invalidplacecursor` — Invalid Place Cursor
- `windfall.movement.rotationplace` — Rotation Place
- `windfall.movement.timer` — Timer A
- `windfall.movement.sprint` — Sprint B
- `windfall.movement.multiplace` — Multi Place
- `windfall.movement.scaffold` — Scaffold A
- `windfall.movement.airliquidplace` — Air Liquid Place
- `windfall.movement.gravity` — Gravity
- `windfall.movement.elytra` — Elytra A
- `windfall.movement.airliquidbreak` — Air Liquid Break
- `windfall.movement.velocity` — Velocity A
- `windfall.movement.speed` — Speed A
- `windfall.movement.farplace` — Far Place A
- `windfall.movement.illegalmove` — Illegal Move
- `windfall.movement.rotationbreak` — Rotation Break A
- `windfall.movement.simulation` — Simulation A
- `windfall.movement.motion` — Motion A
- `windfall.movement.multibreak` — Multi Break
- `windfall.movement.invalidplace` — Invalid Place A
- `windfall.movement.farbreak` — Far Break A
- `windfall.movement.scaffoldsupport` — Scaffold Support
- `windfall.movement.positionplace` — Position Place
- `windfall.movement.noslow` — NoSlow A
- `windfall.movement.fly` — Fly A
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.invalidbreak` — Invalid Break A
- `windfall.movement.noswing` — No Swing A
- `windfall.movement.phase` — Phase A

### Packet
- `windfall.packet.exploit` — Exploit A
- `windfall.packet.crash` — Crash A
- `windfall.packet.chat` — Chat A
- `windfall.packet.transaction` — Transaction A
- `windfall.packet.brand` — Client Brand A
- `windfall.packet.sprint` — Sprint A
- `windfall.packet.creative` — Creative A
- `windfall.packet.bad` — Bad Packets A
- `windfall.packet.order` — Packet Order A
- `windfall.packet.cheststealer` — Chest Stealer A
- `windfall.packet.vehicle` — Vehicle A

---

## Competitor Analysis

### Grim

**Missing from Windfall (1 checks):**

- `movement` **InvalidPlaceFace** → `windfall.movement.invalid place face`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceFace.java`

**Matched with existing Windfall checks:**

- `VehicleD` → `Vehicle A`
- `VehicleE` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `VehicleF` → `Vehicle A`
- `FastBreak` → `Fast Break A`
- `NoSwingBreak` → `No Swing A`
- `WrongBreak` → `Wrong Break`
- `PositionBreakA` → `Position Break`
- `PositionBreakB` → `Position Break`
- `RotationBreak` → `Rotation Break A`
- `FarBreak` → `Far Break A`
- `MultiBreak` → `Multi Break`
- `InvalidBreak` → `Invalid Break A`
- `AirLiquidBreak` → `Air Liquid Break`
- `TimerLimit` → `Timer A`
- `Timer` → `Timer A`
- `VehicleTimer` → `Timer A`
- `TickTimer` → `Timer A`
- `NegativeTimer` → `Timer A`
- `Post` → `No Swing A`
- `ClientBrand` → `Client Brand A`
- `AimModulo360` → `Aim A`
- `AimDuplicateLook` → `Aim A`
- `RotationPlace` → `Rotation Place`
- `DuplicateRotPlace` → `Rotation Place`
- `InvalidPlaceCursor` → `Invalid Place Cursor`
- `FarPlace` → `Far Place A`
- `PositionPlace` → `Position Place`
- `AirLiquidPlace` → `Air Liquid Place`
- `FabricatedPlace` → `Invalid Place Cursor`
- `MultiPlace` → `Multi Place`
- `Baritone` → `Baritone A`
- `ElytraH` → `Elytra A`
- `ElytraI` → `Elytra A`
- `ElytraD` → `Elytra A`
- `ElytraC` → `Elytra A`
- `ElytraA` → `Elytra A`
- `ElytraB` → `Elytra A`
- `ElytraF` → `Elytra A`
- `ElytraE` → `Elytra A`
- `ElytraG` → `Elytra A`
- `MultiActionsC` → `Multi Interact A`
- `MultiActionsF` → `Multi Interact A`
- `MultiActionsA` → `Multi Interact A`
- `MultiActionsD` → `Multi Interact A`
- `MultiActionsB` → `Multi Interact A`
- `MultiActionsE` → `Multi Interact A`
- `MultiActionsG` → `Multi Interact A`
- `FlightA` → `Fly A`
- `CrashI` → `Crash A`
- `CrashD` → `Crash A`
- `CrashH` → `Crash A`
- `CrashA` → `Crash A`
- `CrashB` → `Crash A`
- `CrashC` → `Crash A`
- `CrashE` → `Crash A`
- `CrashG` → `Crash A`
- `CrashF` → `Crash A`
- `MultiInteractA` → `Multi Interact A`
- `MultiInteractB` → `Multi Interact A`
- `InvalidInteractCursor` → `Interact Cursor`
- `Reach` → `Reach A`
- `SelfInteract` → `Self Interact A`
- `NoSlow` → `NoSlow A`
- `Phase` → `Phase A`
- `GroundSpoof` → `Ground Spoof A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderF` → `Packet Order A`
- `PacketOrderJ` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderI` → `Packet Order A`
- `PacketOrderH` → `Packet Order A`
- `PacketOrderM` → `Packet Order A`
- `PacketOrderN` → `Packet Order A`
- `PacketOrderO` → `Packet Order A`
- `PacketOrderK` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderL` → `Packet Order A`
- `PacketOrderP` → `Packet Order A`
- `PacketOrderG` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `ExploitA` → `Exploit A`
- `ExploitB` → `Exploit A`
- `ChatA` → `Chat A`
- `ChatB` → `Chat A`
- `ChatD` → `Chat A`
- `ChatC` → `Chat A`
- `NoFall` → `NoFall A`
- `SprintB` → `Sprint B`
- `SprintF` → `Sprint A`
- `SprintE` → `Sprint A`
- `SprintD` → `Sprint A`
- `SprintG` → `Sprint A`
- `SprintC` → `Sprint A`
- `SprintA` → `Sprint A`

### TruthfulAC

**Missing from Windfall (24 checks):**

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
- `packet` **BadPacketN** → `windfall.packet.bad packet n`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketN.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacketP** → `windfall.packet.bad packet p`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketP.java`
  - `POST_WINDOW_MS = 80L`
- `packet` **BadPacket8** → `windfall.packet.bad packet8`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket8.java`
  - `MAX_DIG_DIST = 6.0D`
- `packet` **BadPacketT** → `windfall.packet.bad packet t`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketT.java`
  - `WINDOW_MS = 10_000L`
  - `SERVER_WINDOW_MS = 15_000L`
  - `GRACE = 3`
- `packet` **BadPacketZ** → `windfall.packet.bad packet z`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketZ.java`
- `packet` **BadPacketM** → `windfall.packet.bad packet m`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketM.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacketR** → `windfall.packet.bad packet r`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketR.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacketB** → `windfall.packet.bad packet b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketB.java`
  - `MAX_FLYING_STREAK = 20`
- `packet` **BadPacketQ** → `windfall.packet.bad packet q`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketQ.java`
- `packet` **BadPacketO** → `windfall.packet.bad packet o`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketO.java`
- `packet` **BadPacketF** → `windfall.packet.bad packet f`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketF.java`
- `packet` **BadPacketY** → `windfall.packet.bad packet y`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketY.java`
- `packet` **BadPacketV** → `windfall.packet.bad packet v`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketV.java`
- `packet` **BadPacket6** → `windfall.packet.bad packet6`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket6.java`
- `packet` **BadPacketL** → `windfall.packet.bad packet l`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketL.java`
- `packet` **BadPacket5** → `windfall.packet.bad packet5`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket5.java`
- `packet` **BadPacketU** → `windfall.packet.bad packet u`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketU.java`

**Matched with existing Windfall checks:**

- `BSpeedA` → `Speed A`
- `BFlyA` → `Fly A`
- `BReachA` → `Reach A`
- `PhaseA` → `Phase A`
- `FastBreakA` → `Fast Break A`
- `AimC` → `Aim A`
- `AimB` → `Aim A`
- `AimA` → `Aim A`
- `HitboxA` → `Hitboxes A`
- `AnchorAuraA` → `Kill Aura A`
- `ReachA` → `Reach A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerA` → `Autoclicker A`
- `AutoClickerE` → `Autoclicker A`
- `KillAuraD` → `Kill Aura A`
- `KillAuraC` → `Kill Aura A`
- `KillAuraE` → `Kill Aura A`
- `KillAuraG` → `Kill Aura A`
- `KillAuraB` → `Kill Aura A`
- `KillAuraH` → `Kill Aura A`
- `KillAuraF` → `Kill Aura A`
- `CrystalAuraA` → `Kill Aura A`
- `MovementCheckSupport` → `Phase A`
- `VelocityD` → `Velocity A`
- `VelocityC` → `Velocity A`
- `VelocityA` → `Velocity A`
- `VelocityB` → `Velocity A`
- `GroundSpoofD` → `Ground Spoof A`
- `GroundSpoofB` → `Ground Spoof A`
- `GroundSpoofG` → `Ground Spoof A`
- `GroundSpoofF` → `Ground Spoof A`
- `GroundSpoofC` → `Ground Spoof A`
- `GroundSpoofE` → `Ground Spoof A`
- `BaritoneA` → `Baritone A`
- `BaritoneC` → `Baritone A`
- `BaritoneB` → `Baritone A`
- `Simulation` → `Simulation A`
- `InventoryA` → `Inventory A`
- `RaycastA` → `Reach A`
- `TimerA` → `Timer A`
- `CrasherA` → `Crash A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `BadPacketH` → `Bad Packets A`
- `BadPacketI` → `Bad Packets A`
- `BadPacketE` → `Bad Packets A`
- `BadPacketD` → `Bad Packets A`
- `BadPacketK` → `Bad Packets A`
- `BadPacketJ` → `Bad Packets A`
- `BadPacketA` → `Bad Packets A`
- `BadPacketG` → `Bad Packets A`
- `InvalidA` → `Invalid Place Cursor`
- `SprintB` → `Sprint B`
- `SprintA` → `Sprint A`

### Arrow

**Missing from Windfall (4 checks):**

- `packet` **Suspicion** → `windfall.packet.suspicion`
  - Source: `src/main/java/me/arrow/checks/impl/suspicion/Suspicion.java`
  - `WINDOW_SIZE = 30`
  - `EVAL_INTERVAL = 10`
  - `NUM_FEATURES = 15`
- `movement` **Movement** → `windfall.movement.movement`
  - Source: `src/main/java/me/arrow/checks/impl/simulation/Movement.java`
- `combat` **AimH2** → `windfall.combat.aim h2`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/AimH2.java`
- `combat` **LinearRegression** → `windfall.combat.linear regression`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/aimassistUtil/LinearRegression.java`
  - `n = x.length`
  - `xbar = sumx / n`
  - `ybar = sumy / n`

**Matched with existing Windfall checks:**

- `VehicleB` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `VehicleC` → `Vehicle A`
- `ExploitA` → `Exploit A`
- `ExploitB` → `Exploit A`
- `TimerB` → `Timer A`
- `TimerC` → `Timer A`
- `TimerA` → `Timer A`
- `ScaffoldD` → `Scaffold A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldA` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `InventoryA` → `Inventory A`
- `FastBreakB` → `Fast Break A`
- `FastBreakA` → `Fast Break A`
- `InteractC` → `Interact Cursor`
- `InteractA` → `Interact Cursor`
- `InteractD` → `Reach A`
- `VelocityA` → `Velocity A`
- `VelocityB` → `Velocity A`
- `HitboxA` → `Hitboxes A`
- `AimG` → `Aim A`
- `AimC` → `Aim A`
- `AimH` → `Aim A`
- `AimB` → `Aim A`
- `AimA` → `Aim A`
- `AimD` → `Aim A`
- `AimF` → `Aim A`
- `AimE` → `Aim A`
- `ReachA` → `Reach A`
- `MacroB` → `Macro A`
- `AutoClickerF` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerG` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `MacroA` → `Macro A`
- `AutoClickerH` → `Autoclicker A`
- `BackTrackA` → `Backtrack A`
- `BackTrackB` → `Backtrack A`
- `KillauraA` → `Kill Aura A`
- `GroundC` → `Ground Spoof A`
- `GroundB` → `Ground Spoof A`
- `GroundA` → `Ground Spoof A`
- `GravityD` → `Gravity`
- `FlyA` → `Fly A`
- `GravityB` → `Gravity`
- `ElytraA` → `Elytra A`
- `FlyB` → `Fly A`
- `ElytraB` → `Elytra A`
- `GravityA` → `Gravity`
- `GravityC` → `Gravity`
- `OmniSprintA` → `Sprint A`
- `SpeedA` → `Speed A`
- `SpeedB` → `Speed A`
- `IllegalMoveC` → `Illegal Move`
- `IllegalMoveB` → `Phase A`
- `MotionE` → `Motion A`
- `MotionD` → `Motion A`
- `MotionA` → `Motion A`
- `MotionB` → `Motion A`

---

## Summary

- Windfall has **60 checks**
- Found **29 new checks** across competitors that Windfall doesn't have

## Recommendations

1. Review generated skeleton files in `src/main/java/io/windfall/anticheat/core/check/impl/`
2. Implement detection logic based on competitor reference
3. Tune thresholds and buffer values for each check
4. Register new checks in `CheckManager.java`
5. Add config entries to `config.yml`
6. Test on live server before enabling punishable mode
