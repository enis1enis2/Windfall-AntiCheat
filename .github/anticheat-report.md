# Windfall Anti-Cheat Monitor Report

**Generated:** 2026-07-11 14:37:50 UTC

---

## Windfall Current Checks

**Total: 44 checks**

### Combat
- `windfall.combat.fastheal` — Fast Heal A
- `windfall.combat.killaura` — Kill Aura A
- `windfall.combat.reach` — Reach A
- `windfall.combat.hitboxes` — Hitboxes A
- `windfall.combat.macro` — Macro A
- `windfall.combat.selfinteract` — Self Interact A
- `windfall.combat.autoclicker` — Autoclicker A
- `windfall.combat.multiinteract` — Multi Interact A
- `windfall.combat.aim` — Aim A
- `windfall.combat.backtrack` — Backtrack A
- `windfall.combat.swordblock` — Sword Block A
- `windfall.combat.criticals` — Criticals A

### Movement
- `windfall.movement.rotationbreak` — Rotation Break A
- `windfall.movement.simulation` — Simulation A
- `windfall.movement.speed` — Speed A
- `windfall.movement.farplace` — Far Place A
- `windfall.movement.invalidplace` — Invalid Place A
- `windfall.movement.phase` — Phase A
- `windfall.movement.fastbreak` — Fast Break A
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.motion` — Motion A
- `windfall.movement.noslow` — NoSlow A
- `windfall.movement.step` — Step A
- `windfall.movement.invalidbreak` — Invalid Break A
- `windfall.movement.noswing` — No Swing A
- `windfall.movement.farbreak` — Far Break A
- `windfall.movement.timer` — Timer A
- `windfall.movement.baritone` — Baritone A
- `windfall.movement.fly` — Fly A
- `windfall.movement.elytra` — Elytra A
- `windfall.movement.groundspoof` — Ground Spoof A
- `windfall.movement.scaffold` — Scaffold A
- `windfall.movement.velocity` — Velocity A

### Packet
- `windfall.packet.chat` — Chat A
- `windfall.packet.sprint` — Sprint A
- `windfall.packet.bad` — Bad Packets A
- `windfall.packet.cheststealer` — Chest Stealer A
- `windfall.packet.exploit` — Exploit A
- `windfall.packet.order` — Packet Order A
- `windfall.packet.vehicle` — Vehicle A
- `windfall.packet.creative` — Creative A
- `windfall.packet.brand` — Client Brand A
- `windfall.packet.crash` — Crash A

---

## Competitor Analysis

### Grim

**Missing from Windfall (22 checks):**

- `movement` **SetbackBlocker** → `windfall.movement.setbackblocker`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/movement/SetbackBlocker.java`
- `movement` **VehiclePredictionRunner** → `windfall.movement.vehiclepredictionrunner`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/movement/VehiclePredictionRunner.java`
- `movement` **FlightA** → `windfall.movement.flighta`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/flight/FlightA.java`
- `packet` **VerboseCodecs** → `windfall.packet.verbosecodecs`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/verbose/VerboseCodecs.java`
  - `PACKET_NONE = Integer.MIN_VALUE`
  - `PACKET_TRANSACTION = Integer.MIN_VALUE + 1`
- `packet` **MultiActionsA** → `windfall.packet.multiactionsa`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/multiactions/MultiActionsA.java`
- `packet` **MultiActionsG** → `windfall.packet.multiactionsg`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/multiactions/MultiActionsG.java`
  - `ACTION_INTERACT = 0`
  - `ACTION_ATTACK = 1`
  - `ACTION_SPECTATE_ENTITY = 2`
- `packet` **MultiActionsC** → `windfall.packet.multiactionsc`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/multiactions/MultiActionsC.java`
- `packet` **MultiActionsB** → `windfall.packet.multiactionsb`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/multiactions/MultiActionsB.java`
- `packet` **MultiActionsD** → `windfall.packet.multiactionsd`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/multiactions/MultiActionsD.java`
- `packet` **MultiActionsE** → `windfall.packet.multiactionse`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/multiactions/MultiActionsE.java`
- `packet` **MultiActionsF** → `windfall.packet.multiactionsf`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/multiactions/MultiActionsF.java`
  - `ACTION_PLACE = 0`
  - `ACTION_ENTITY = 1`
  - `ACTION_DIG = 2`
- `movement` **VectorPrecisionConverter** → `windfall.movement.vectorprecisionconverter`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/velocity/VectorPrecisionConverter.java`
  - `PRECISION_LOSS_FIX = 1e-11d`
- `packet` **VehicleA** → `windfall.packet.vehiclea`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/vehicle/VehicleA.java`
- `packet` **VehicleE** → `windfall.packet.vehiclee`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/vehicle/VehicleE.java`
- `packet` **VehicleB** → `windfall.packet.vehicleb`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/vehicle/VehicleB.java`
- `packet` **VehicleD** → `windfall.packet.vehicled`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/vehicle/VehicleD.java`
- `packet` **VehicleF** → `windfall.packet.vehiclef`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/vehicle/VehicleF.java`
- `combat` **MultiInteractA** → `windfall.combat.multiinteracta`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/combat/MultiInteractA.java`
- `combat` **SelfInteract** → `windfall.combat.selfinteract`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/combat/SelfInteract.java`
- `combat` **MultiInteractB** → `windfall.combat.multiinteractb`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/combat/MultiInteractB.java`
- `packet` **Post** → `windfall.packet.post`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/misc/Post.java`
  - `isExemptFromSwingingCheck = Integer.MIN_VALUE`
- `packet` **ClientBrand** → `windfall.packet.clientbrand`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/misc/ClientBrand.java`
  - `CHANNEL = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13) ? "minecraft:brand" : "MC|Brand"`
  - `brand = "vanilla"`

**Matched with existing Windfall checks:**

- `NoSlow` → `NoSlow A`
- `PacketOrderK` → `Packet Order A`
- `PacketOrderM` → `Packet Order A`
- `PacketOrderF` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderJ` → `Packet Order A`
- `PacketOrderL` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderN` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderO` → `Packet Order A`
- `PacketOrderG` → `Packet Order A`
- `PacketOrderH` → `Packet Order A`
- `PacketOrderP` → `Packet Order A`
- `PacketOrderI` → `Packet Order A`
- `ChatD` → `Chat A`
- `ChatA` → `Chat A`
- `ChatC` → `Chat A`
- `ChatB` → `Chat A`
- `SprintC` → `Sprint A`
- `SprintG` → `Sprint A`
- `SprintA` → `Sprint A`
- `SprintF` → `Sprint A`
- `SprintE` → `Sprint A`
- `SprintB` → `Sprint A`
- `SprintD` → `Sprint A`
- `ElytraE` → `Elytra A`
- `ElytraI` → `Elytra A`
- `ElytraH` → `Elytra A`
- `ElytraB` → `Elytra A`
- `ElytraG` → `Elytra A`
- `ElytraA` → `Elytra A`
- `ElytraF` → `Elytra A`
- `ElytraD` → `Elytra A`
- `ElytraC` → `Elytra A`
- `Phase` → `Phase A`
- `GroundSpoof` → `Ground Spoof A`
- `AimModulo360` → `Aim A`
- `AimDuplicateLook` → `Aim A`
- `AirLiquidBreak` → `Rotation Break A`
- `InvalidBreak` → `Rotation Break A`
- `NoSwingBreak` → `Rotation Break A`
- `WrongBreak` → `Rotation Break A`
- `PositionBreakA` → `Rotation Break A`
- `PositionBreakB` → `Rotation Break A`
- `FastBreak` → `Rotation Break A`
- `MultiBreak` → `Rotation Break A`
- `FarBreak` → `Rotation Break A`
- `RotationBreak` → `Rotation Break A`
- `Baritone` → `Baritone A`
- `ExploitB` → `Exploit A`
- `ExploitA` → `Exploit A`
- `Hitboxes` → `Hitboxes A`
- `Reach` → `Reach A`
- `NoFall` → `NoFall A`
- `TimerLimit` → `Timer A`
- `TickTimer` → `Timer A`
- `VehicleTimer` → `Timer A`
- `Timer` → `Timer A`
- `NegativeTimer` → `Timer A`
- `CrashH` → `Crash A`
- `CrashD` → `Crash A`
- `CrashI` → `Crash A`
- `CrashE` → `Crash A`
- `CrashG` → `Crash A`
- `CrashB` → `Crash A`
- `CrashF` → `Crash A`
- `CrashA` → `Crash A`
- `CrashC` → `Crash A`
- `FabricatedPlace` → `Far Place A`
- `AirLiquidPlace` → `Far Place A`
- `InvalidPlaceA` → `Far Place A`
- `DuplicateRotPlace` → `Far Place A`
- `RotationPlace` → `Far Place A`
- `InvalidPlaceB` → `Far Place A`
- `PositionPlace` → `Far Place A`
- `FarPlace` → `Far Place A`
- `MultiPlace` → `Far Place A`

### TruthfulAC

**Missing from Windfall (12 checks):**

- `movement` **MovementCheckSupport** → `windfall.movement.movementchecksupport`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/MovementCheckSupport.java`
- `packet` **RaycastA** → `windfall.packet.raycasta`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/raycast/RaycastA.java`
- `packet` **InvalidA** → `windfall.packet.invalida`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/invalid/InvalidA.java`
  - `MAX_PITCH = 90.0`
  - `MIN_PITCH = -90.0`
  - `pitch = relMovePacketWrapper.getPitch()`
- `packet` **BadPacketA** → `windfall.packet.badpacketa`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketA.java`
- `packet` **BadPacketE** → `windfall.packet.badpackete`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketE.java`
- `packet` **BadPacketG** → `windfall.packet.badpacketg`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketG.java`
- `packet` **BadPacketD** → `windfall.packet.badpacketd`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketD.java`
- `packet` **BadPacketJ** → `windfall.packet.badpacketj`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketJ.java`
- `packet` **BadPacketI** → `windfall.packet.badpacketi`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketI.java`
- `packet` **BadPacketC** → `windfall.packet.badpacketc`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketC.java`
- `packet` **BadPacketK** → `windfall.packet.badpacketk`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketK.java`
- `packet` **BadPacketH** → `windfall.packet.badpacketh`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketH.java`

**Matched with existing Windfall checks:**

- `GroundSpoofB` → `Ground Spoof A`
- `GroundSpoofG` → `Ground Spoof A`
- `GroundSpoofD` → `Ground Spoof A`
- `GroundSpoofC` → `Ground Spoof A`
- `GroundSpoofE` → `Ground Spoof A`
- `GroundSpoofF` → `Ground Spoof A`
- `VelocityD` → `Velocity A`
- `VelocityC` → `Velocity A`
- `VelocityA` → `Velocity A`
- `VelocityB` → `Velocity A`
- `BaritoneB` → `Baritone A`
- `BaritoneA` → `Baritone A`
- `BaritoneC` → `Baritone A`
- `InventoryA` → `Inventory A`
- `SimulationC` → `Simulation A`
- `SimulationD` → `Simulation A`
- `SimulationB` → `Simulation A`
- `SimulationA` → `Simulation A`
- `ScaffoldA` → `Scaffold A`
- `FastBreakA` → `Rotation Break A`
- `PhaseA` → `Phase A`
- `BReachA` → `Reach A`
- `BSpeedA` → `Speed A`
- `BFlyA` → `Fly A`
- `AnchorAuraA` → `Kill Aura A`
- `CrystalAuraA` → `Kill Aura A`
- `AimH` → `Aim A`
- `AimL` → `Aim A`
- `AimF` → `Aim A`
- `AimD` → `Aim A`
- `AimG` → `Aim A`
- `AimK` → `Aim A`
- `AimE` → `Aim A`
- `AimJ` → `Aim A`
- `AimI` → `Aim A`
- `AimB` → `Aim A`
- `AimA` → `Aim A`
- `ReachA` → `Reach A`
- `HitboxA` → `Hitboxes A`
- `KillAuraG` → `Kill Aura A`
- `KillAuraC` → `Kill Aura A`
- `KillAuraH` → `Kill Aura A`
- `KillAuraD` → `Kill Aura A`
- `KillAuraF` → `Kill Aura A`
- `KillAuraE` → `Kill Aura A`
- `KillAuraB` → `Kill Aura A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerE` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerA` → `Autoclicker A`
- `CrasherA` → `Crash A`
- `SprintA` → `Sprint A`
- `SprintB` → `Sprint A`
- `TimerA` → `Timer A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`

### CloudAC

**Missing from Windfall (1 checks):**

- `packet` **CheckAbilties** → `windfall.packet.abilties`
  - Source: `ComputationServer/sys/checks.go`
  - `_ = = net.Dial("tcp", "localhost:1212")`

### Arrow

**Missing from Windfall (5 checks):**

- `movement` **IllegalMoveB** → `windfall.movement.illegalmoveb`
  - Source: `src/main/java/me/arrow/checks/impl/movement/illegalmove/IllegalMoveB.java`
  - `strafeBuffer = 0`
  - `predictedX = lastDeltaX * 0.9100000262260437`
  - `predictedZ = lastDeltaZ * 0.9100000262260437`
- `movement` **OmniSprintA** → `windfall.movement.omnisprinta`
  - Source: `src/main/java/me/arrow/checks/impl/movement/speed/OmniSprintA.java`
  - `GROUND_INVALID_ANGLE = 78.0D`
  - `GROUND_HARD_INVALID_ANGLE = 112.5D`
  - `GROUND_MIN_FORWARD_DOT = 0.20D`
- `packet` **InteractC** → `windfall.packet.interactc`
  - Source: `src/main/java/me/arrow/checks/impl/misc/interact/InteractC.java`
- `packet` **InteractA** → `windfall.packet.interacta`
  - Source: `src/main/java/me/arrow/checks/impl/misc/interact/InteractA.java`
  - `time = event.getTimestamp()`
  - `lastTime = this.lastTime`
  - `delta = time - lastTime`
- `packet` **VehicleA** → `windfall.packet.vehiclea`
  - Source: `src/main/java/me/arrow/checks/impl/misc/vehicle/VehicleA.java`
  - `deltaX = current.getX() - lastVehicleLocation.getX()`
  - `deltaY = current.getY() - lastVehicleLocation.getY()`
  - `deltaZ = current.getZ() - lastVehicleLocation.getZ()`

**Matched with existing Windfall checks:**

- `GroundA` → `Ground Spoof A`
- `GroundB` → `Ground Spoof A`
- `GroundC` → `Ground Spoof A`
- `MotionB` → `Motion A`
- `MotionF` → `Motion A`
- `MotionD` → `Motion A`
- `MotionA` → `Motion A`
- `MotionE` → `Motion A`
- `SpeedC` → `Speed A`
- `SpeedB` → `Speed A`
- `SpeedA` → `Speed A`
- `FlyB` → `Fly A`
- `ElytraA` → `Elytra A`
- `FlyC` → `Fly A`
- `FlyA` → `Fly A`
- `BackTrackA` → `Backtrack A`
- `BackTrackB` → `Backtrack A`
- `VelocityA` → `Velocity A`
- `VelocityB` → `Velocity A`
- `ReachA` → `Reach A`
- `KillauraA` → `Kill Aura A`
- `AutoClickerG` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `MacroA` → `Macro A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerF` → `Autoclicker A`
- `AimF` → `Aim A`
- `AimD` → `Aim A`
- `AimG` → `Aim A`
- `AimE` → `Aim A`
- `AimC` → `Aim A`
- `AimB` → `Aim A`
- `AimA` → `Aim A`
- `ScaffoldA` → `Scaffold A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `InventoryA` → `Inventory A`
- `InventoryB` → `Inventory A`
- `TimerA` → `Timer A`
- `TimerB` → `Timer A`
- `PhaseA` → `Phase A`

---

## Summary

- Windfall has **44 checks**
- Found **40 new checks** across competitors that Windfall doesn't have

## Recommendations

1. Review generated skeleton files in `src/main/java/io/windfall/anticheat/core/check/impl/`
2. Implement detection logic based on competitor reference
3. Tune thresholds and buffer values for each check
4. Register new checks in `CheckManager.java`
5. Add config entries to `config.yml`
6. Test on live server before enabling punishable mode
