# Windfall Anti-Cheat Monitor Report

**Generated:** 2026-07-11 14:10:39 UTC

---

## Windfall Current Checks

**Total: 33 checks**

### Combat
- `windfall.combat.fastheal` — Fast Heal A
- `windfall.combat.killaura` — Kill Aura A
- `windfall.combat.reach` — Reach A
- `windfall.combat.hitboxes` — Hitboxes A
- `windfall.combat.selfinteract` — Self Interact A
- `windfall.combat.autoclicker` — Autoclicker A
- `windfall.combat.multiinteract` — Multi Interact A
- `windfall.combat.aim` — Aim A
- `windfall.combat.backtrack` — Backtrack A
- `windfall.combat.swordblock` — Sword Block A
- `windfall.combat.criticals` — Criticals A

### Movement
- `windfall.movement.simulation` — Simulation A
- `windfall.movement.speed` — Speed A
- `windfall.movement.phase` — Phase A
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.motion` — Motion A
- `windfall.movement.noslow` — NoSlow A
- `windfall.movement.step` — Step A
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
- `windfall.packet.creative` — Creative A
- `windfall.packet.crash` — Crash A

---

## Competitor Analysis

### Grim

**Missing from Windfall (41 checks):**

- `movement` **SetbackBlocker** → `windfall.movement.setbackblocker`
- `movement` **VehiclePredictionRunner** → `windfall.movement.vehiclepredictionrunner`
- `movement` **FlightA** → `windfall.movement.flighta`
- `packet` **VerboseCodecs** → `windfall.packet.verbosecodecs`
  - `PACKET_NONE = Integer.MIN_VALUE`
  - `PACKET_TRANSACTION = Integer.MIN_VALUE + 1`
- `packet` **MultiActionsA** → `windfall.packet.multiactionsa`
- `packet` **MultiActionsG** → `windfall.packet.multiactionsg`
  - `ACTION_INTERACT = 0`
  - `ACTION_ATTACK = 1`
  - `ACTION_SPECTATE_ENTITY = 2`
- `packet` **MultiActionsC** → `windfall.packet.multiactionsc`
- `packet` **MultiActionsB** → `windfall.packet.multiactionsb`
- `packet` **MultiActionsD** → `windfall.packet.multiactionsd`
- `packet` **MultiActionsE** → `windfall.packet.multiactionse`
- `packet` **MultiActionsF** → `windfall.packet.multiactionsf`
  - `ACTION_PLACE = 0`
  - `ACTION_ENTITY = 1`
  - `ACTION_DIG = 2`
- `movement` **VectorPrecisionConverter** → `windfall.movement.vectorprecisionconverter`
  - `PRECISION_LOSS_FIX = 1e-11d`
- `movement` **AirLiquidBreak** → `windfall.movement.airliquidbreak`
- `movement` **InvalidBreak** → `windfall.movement.invalidbreak`
- `movement` **NoSwingBreak** → `windfall.movement.noswingbreak`
- `movement` **WrongBreak** → `windfall.movement.wrongbreak`
  - `exemptedY = player.getClientVersion().isOlderThan(ClientVersion.V_1_8) ? 255 : (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14) ? -1 : 4095)`
- `movement` **PositionBreakA** → `windfall.movement.positionbreaka`
- `movement` **PositionBreakB** → `windfall.movement.positionbreakb`
- `movement` **FastBreak** → `windfall.movement.fastbreak`
- `movement` **MultiBreak** → `windfall.movement.multibreak`
  - `face = VerboseCodecs.enumId(blockBreak.face)`
  - `previousFace = VerboseCodecs.enumId(lastFace)`
- `movement` **FarBreak** → `windfall.movement.farbreak`
- `movement` **RotationBreak** → `windfall.movement.rotationbreak`
  - `flagBuffer = 0`
  - `distance = player.compensatedEntities.self.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)`
- `packet` **VehicleA** → `windfall.packet.vehiclea`
- `packet` **VehicleE** → `windfall.packet.vehiclee`
- `packet` **VehicleB** → `windfall.packet.vehicleb`
- `packet` **VehicleD** → `windfall.packet.vehicled`
- `packet` **VehicleF** → `windfall.packet.vehiclef`
- `combat` **MultiInteractA** → `windfall.combat.multiinteracta`
- `combat` **SelfInteract** → `windfall.combat.selfinteract`
- `combat` **MultiInteractB** → `windfall.combat.multiinteractb`
- `movement` **FabricatedPlace** → `windfall.movement.fabricatedplace`
  - `MAX_DOUBLE_ERROR = Math.ulp(30_000_000.0) * 2.0`
  - `FLOAT_STEP_AT_ONE = Math.ulp(1.0f)`
- `movement` **AirLiquidPlace** → `windfall.movement.airliquidplace`
- `movement` **InvalidPlaceA** → `windfall.movement.invalidplacea`
- `movement` **DuplicateRotPlace** → `windfall.movement.duplicaterotplace`
- `movement` **RotationPlace** → `windfall.movement.rotationplace`
  - `flagBuffer = 0`
  - `distance = player.compensatedEntities.self.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)`
- `movement` **InvalidPlaceB** → `windfall.movement.invalidplaceb`
- `movement` **PositionPlace** → `windfall.movement.positionplace`
- `movement` **FarPlace** → `windfall.movement.farplace`
- `movement` **MultiPlace** → `windfall.movement.multiplace`
  - `faceId = VerboseCodecs.enumId(face)`
  - `lastFaceId = VerboseCodecs.enumId(lastFace)`
- `packet` **Post** → `windfall.packet.post`
  - `isExemptFromSwingingCheck = Integer.MIN_VALUE`
- `packet` **ClientBrand** → `windfall.packet.clientbrand`
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

### TruthfulAC

**Missing from Windfall (14 checks):**

- `movement` **MovementCheckSupport** → `windfall.movement.movementchecksupport`
- `inventory` **InventoryA** → `windfall.inventory.inventorya`
- `movement` **FastBreakA** → `windfall.movement.fastbreaka`
- `packet` **RaycastA** → `windfall.packet.raycasta`
- `packet` **InvalidA** → `windfall.packet.invalida`
  - `MAX_PITCH = 90.0`
  - `MIN_PITCH = -90.0`
  - `pitch = relMovePacketWrapper.getPitch()`
- `packet` **BadPacketA** → `windfall.packet.badpacketa`
- `packet` **BadPacketE** → `windfall.packet.badpackete`
- `packet` **BadPacketG** → `windfall.packet.badpacketg`
- `packet` **BadPacketD** → `windfall.packet.badpacketd`
- `packet` **BadPacketJ** → `windfall.packet.badpacketj`
- `packet` **BadPacketI** → `windfall.packet.badpacketi`
- `packet` **BadPacketC** → `windfall.packet.badpacketc`
- `packet` **BadPacketK** → `windfall.packet.badpacketk`
- `packet` **BadPacketH** → `windfall.packet.badpacketh`

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
- `SimulationC` → `Simulation A`
- `SimulationD` → `Simulation A`
- `SimulationB` → `Simulation A`
- `SimulationA` → `Simulation A`
- `ScaffoldA` → `Scaffold A`
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
  - `_ = = net.Dial("tcp", "localhost:1212")`

### Arrow

**Missing from Windfall (8 checks):**

- `movement` **IllegalMoveB** → `windfall.movement.illegalmoveb`
  - `strafeBuffer = 0`
  - `predictedX = lastDeltaX * 0.9100000262260437`
  - `predictedZ = lastDeltaZ * 0.9100000262260437`
- `movement` **OmniSprintA** → `windfall.movement.omnisprinta`
  - `GROUND_INVALID_ANGLE = 78.0D`
  - `GROUND_HARD_INVALID_ANGLE = 112.5D`
  - `GROUND_MIN_FORWARD_DOT = 0.20D`
- `combat` **MacroA** → `windfall.combat.macroa`
- `packet` **InteractC** → `windfall.packet.interactc`
- `packet` **InteractA** → `windfall.packet.interacta`
  - `time = event.getTimestamp()`
  - `lastTime = this.lastTime`
  - `delta = time - lastTime`
- `packet` **VehicleA** → `windfall.packet.vehiclea`
  - `deltaX = current.getX() - lastVehicleLocation.getX()`
  - `deltaY = current.getY() - lastVehicleLocation.getY()`
  - `deltaZ = current.getZ() - lastVehicleLocation.getZ()`
- `inventory` **InventoryA** → `windfall.inventory.inventorya`
- `inventory` **InventoryB** → `windfall.inventory.inventoryb`
  - `time = event.getTimestamp()`
  - `lastTime = this.lastTime`
  - `delta = time - lastTime`

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
- `TimerA` → `Timer A`
- `TimerB` → `Timer A`
- `PhaseA` → `Phase A`

---

## Summary

- Windfall has **33 checks**
- Found **64 new checks** across competitors that Windfall doesn't have

## Recommendations

1. Review generated skeleton files in `src/main/java/io/windfall/anticheat/core/check/impl/`
2. Implement detection logic based on competitor reference
3. Tune thresholds and buffer values for each check
4. Register new checks in `CheckManager.java`
5. Add config entries to `config.yml`
6. Test on live server before enabling punishable mode
