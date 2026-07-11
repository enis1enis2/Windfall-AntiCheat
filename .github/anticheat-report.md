# Windfall Anti-Cheat Monitor Report

**Generated:** 2026-07-11 09:59:06 UTC

---

## Windfall Current Checks

**Total: 17 checks**

### Combat
- `windfall.combat.aim` — Aim A
- `windfall.combat.criticals` — Criticals A
- `windfall.combat.fastheal` — Fast Heal A
- `windfall.combat.killaura` — Kill Aura A
- `windfall.combat.reach` — Reach A
- `windfall.combat.swordblock` — Sword Block A

### Movement
- `windfall.movement.elytra` — Elytra A
- `windfall.movement.fly` — Fly A
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.scaffold` — Scaffold A
- `windfall.movement.speed` — Speed A
- `windfall.movement.step` — Step A
- `windfall.movement.timer` — Timer A
- `windfall.movement.velocity` — Velocity A

### Packet
- `windfall.packet.bad` — Bad Packets A
- `windfall.packet.cheststealer` — Chest Stealer A
- `windfall.packet.creative` — Creative A

---

## Competitor Analysis

### Grim

**Missing from Windfall (84 checks):**

- `movement` **Baritone** → `windfall.movement.baritone`
  - `deltaPitch = Math.abs(to.pitch() - from.pitch())`
- `movement` **AirLiquidBreak** → `windfall.movement.airliquidbreak`
- `movement` **FarBreak** → `windfall.movement.farbreak`
- `movement` **FastBreak** → `windfall.movement.fastbreak`
- `movement` **InvalidBreak** → `windfall.movement.invalidbreak`
- `movement` **MultiBreak** → `windfall.movement.multibreak`
  - `face = VerboseCodecs.enumId(blockBreak.face)`
  - `previousFace = VerboseCodecs.enumId(lastFace)`
- `movement` **NoSwingBreak** → `windfall.movement.noswingbreak`
- `movement` **PositionBreakA** → `windfall.movement.positionbreaka`
- `movement` **PositionBreakB** → `windfall.movement.positionbreakb`
- `movement` **RotationBreak** → `windfall.movement.rotationbreak`
  - `flagBuffer = 0`
  - `distance = player.compensatedEntities.self.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)`
- `movement` **WrongBreak** → `windfall.movement.wrongbreak`
  - `exemptedY = player.getClientVersion().isOlderThan(ClientVersion.V_1_8) ? 255 : (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14) ? -1 : 4095)`
- `packet` **ChatA** → `windfall.packet.chata`
- `packet` **ChatB** → `windfall.packet.chatb`
- `packet` **ChatC** → `windfall.packet.chatc`
- `packet` **ChatD** → `windfall.packet.chatd`
- `combat` **Hitboxes** → `windfall.combat.hitboxes`
- `combat` **MultiInteractA** → `windfall.combat.multiinteracta`
- `combat` **MultiInteractB** → `windfall.combat.multiinteractb`
- `combat` **SelfInteract** → `windfall.combat.selfinteract`
- `packet` **CrashA** → `windfall.packet.crasha`
  - `HARD_CODED_BORDER = 2.9999999E7D`
- `packet` **CrashB** → `windfall.packet.crashb`
- `packet` **CrashC** → `windfall.packet.crashc`
- `packet` **CrashD** → `windfall.packet.crashd`
  - `lecternId = -1`
- `packet` **CrashE** → `windfall.packet.crashe`
- `packet` **CrashF** → `windfall.packet.crashf`
- `packet` **CrashG** → `windfall.packet.crashg`
- `packet` **CrashH** → `windfall.packet.crashh`
  - `length = text.length()`
- `packet` **CrashI** → `windfall.packet.crashi`
- `packet` **ExploitA** → `windfall.packet.exploita`
- `packet` **ExploitB** → `windfall.packet.exploitb`
  - `BOOK_EXPECTED_SLOT = 0`
  - `BOOK_EXPECTED_TYPE = 1`
  - `BOOK_EXPECTED_AMOUNT = 2`
- `movement` **FlightA** → `windfall.movement.flighta`
- `packet` **ClientBrand** → `windfall.packet.clientbrand`
  - `CHANNEL = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13) ? "minecraft:brand" : "MC|Brand"`
  - `brand = "vanilla"`
- `packet` **Post** → `windfall.packet.post`
  - `isExemptFromSwingingCheck = Integer.MIN_VALUE`
- `movement` **NoSlow** → `windfall.movement.noslow`
  - `bestOffset = 1`
- `packet` **SetbackBlocker** → `windfall.packet.setbackblocker`
- `packet` **VehiclePredictionRunner** → `windfall.packet.vehiclepredictionrunner`
- `packet` **MultiActionsA** → `windfall.packet.multiactionsa`
- `packet` **MultiActionsB** → `windfall.packet.multiactionsb`
- `packet` **MultiActionsC** → `windfall.packet.multiactionsc`
- `packet` **MultiActionsD** → `windfall.packet.multiactionsd`
- `packet` **MultiActionsE** → `windfall.packet.multiactionse`
- `packet` **MultiActionsF** → `windfall.packet.multiactionsf`
  - `ACTION_PLACE = 0`
  - `ACTION_ENTITY = 1`
  - `ACTION_DIG = 2`
- `packet` **MultiActionsG** → `windfall.packet.multiactionsg`
  - `ACTION_INTERACT = 0`
  - `ACTION_ATTACK = 1`
  - `ACTION_SPECTATE_ENTITY = 2`
- `packet` **PacketOrderA** → `windfall.packet.packetordera`
- `packet` **PacketOrderB** → `windfall.packet.packetorderb`
- `packet` **PacketOrderC** → `windfall.packet.packetorderc`
  - `KIND_SKIPPED_INTERACT_AT = 0`
  - `KIND_SKIPPED_INTERACT = 1`
  - `KIND_SKIPPED_INTERACT_TICK = 2`
- `packet` **PacketOrderD** → `windfall.packet.packetorderd`
  - `entity = packet.getEntityId()`
- `packet` **PacketOrderE** → `windfall.packet.packetordere`
  - `ATTACKING = 1 << 0`
  - `RIGHT_CLICKING = 1 << 1`
  - `OPENING_INVENTORY = 1 << 2`
- `packet` **PacketOrderF** → `windfall.packet.packetorderf`
  - `ACTION_INTERACT = 0`
  - `ACTION_ATTACK = 1`
  - `ACTION_SPECTATE_ENTITY = 2`
- `packet` **PacketOrderG** → `windfall.packet.packetorderg`
  - `ACTION_OPEN_INVENTORY = 0`
  - `ACTION_SWAP = 1`
  - `ACTION_DROP = 2`
- `packet` **PacketOrderH** → `windfall.packet.packetorderh`
- `packet` **PacketOrderI** → `windfall.packet.packetorderi`
  - `TYPE_INTERACT = 0`
  - `TYPE_PLACE_USE = 1`
  - `TYPE_RELEASE = 2`
- `packet` **PacketOrderJ** → `windfall.packet.packetorderj`
- `packet` **PacketOrderK** → `windfall.packet.packetorderk`
  - `KIND_OPEN = 0`
  - `KIND_CLICK = 1`
  - `KIND_CLOSE = 2`
- `packet` **PacketOrderL** → `windfall.packet.packetorderl`
  - `ACTION_INVENTORY = 0`
  - `ACTION_SWAP = 1`
- `packet` **PacketOrderM** → `windfall.packet.packetorderm`
- `packet` **PacketOrderN** → `windfall.packet.packetordern`
- `packet` **PacketOrderO** → `windfall.packet.packetordero`
- `packet` **PacketOrderP** → `windfall.packet.packetorderp`
- `movement` **GroundSpoof** → `windfall.movement.groundspoof`
- `movement` **Phase** → `windfall.movement.phase`
- `movement` **AirLiquidPlace** → `windfall.movement.airliquidplace`
- `movement` **DuplicateRotPlace** → `windfall.movement.duplicaterotplace`
- `movement` **FabricatedPlace** → `windfall.movement.fabricatedplace`
  - `MAX_DOUBLE_ERROR = Math.ulp(30_000_000.0) * 2.0`
  - `FLOAT_STEP_AT_ONE = Math.ulp(1.0f)`
- `movement` **FarPlace** → `windfall.movement.farplace`
- `movement` **InvalidPlaceA** → `windfall.movement.invalidplacea`
- `movement` **InvalidPlaceB** → `windfall.movement.invalidplaceb`
- `movement` **MultiPlace** → `windfall.movement.multiplace`
  - `faceId = VerboseCodecs.enumId(face)`
  - `lastFaceId = VerboseCodecs.enumId(lastFace)`
- `movement` **PositionPlace** → `windfall.movement.positionplace`
- `movement` **RotationPlace** → `windfall.movement.rotationplace`
  - `flagBuffer = 0`
  - `distance = player.compensatedEntities.self.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)`
- `packet` **SprintA** → `windfall.packet.sprinta`
- `packet` **SprintB** → `windfall.packet.sprintb`
- `packet` **SprintC** → `windfall.packet.sprintc`
- `packet` **SprintD** → `windfall.packet.sprintd`
- `packet` **SprintE** → `windfall.packet.sprinte`
- `packet` **SprintF** → `windfall.packet.sprintf`
- `packet` **SprintG** → `windfall.packet.sprintg`
- `packet` **VehicleA** → `windfall.packet.vehiclea`
- `packet` **VehicleB** → `windfall.packet.vehicleb`
- `packet` **VehicleD** → `windfall.packet.vehicled`
- `packet` **VehicleE** → `windfall.packet.vehiclee`
- `packet` **VehicleF** → `windfall.packet.vehiclef`
- `movement` **VectorPrecisionConverter** → `windfall.movement.vectorprecisionconverter`
  - `PRECISION_LOSS_FIX = 1e-11d`
- `packet` **VerboseCodecs** → `windfall.packet.verbosecodecs`
  - `PACKET_NONE = Integer.MIN_VALUE`
  - `PACKET_TRANSACTION = Integer.MIN_VALUE + 1`

**Matched with existing Windfall checks:**

- `AimDuplicateLook` → `Aim A`
- `AimModulo360` → `Aim A`
- `Reach` → `Reach A`
- `ElytraA` → `Elytra A`
- `ElytraB` → `Elytra A`
- `ElytraC` → `Elytra A`
- `ElytraD` → `Elytra A`
- `ElytraE` → `Elytra A`
- `ElytraF` → `Elytra A`
- `ElytraG` → `Elytra A`
- `ElytraH` → `Elytra A`
- `ElytraI` → `Elytra A`
- `NoFall` → `NoFall A`
- `NegativeTimer` → `Timer A`
- `TickTimer` → `Timer A`
- `Timer` → `Timer A`
- `TimerLimit` → `Timer A`
- `VehicleTimer` → `Timer A`

### TruthfulAC

**Missing from Windfall (42 checks):**

- `packet` **MovementCheckSupport** → `windfall.packet.movementchecksupport`
- `packet` **RaycastA** → `windfall.packet.raycasta`
- `movement` **FastBreakA** → `windfall.movement.fastbreaka`
- `movement` **PhaseA** → `windfall.movement.phasea`
- `packet` **BadPacketA** → `windfall.packet.badpacketa`
- `packet` **BadPacketC** → `windfall.packet.badpacketc`
- `packet` **BadPacketD** → `windfall.packet.badpacketd`
- `packet` **BadPacketE** → `windfall.packet.badpackete`
- `packet` **BadPacketG** → `windfall.packet.badpacketg`
- `packet` **BadPacketH** → `windfall.packet.badpacketh`
- `packet` **BadPacketI** → `windfall.packet.badpacketi`
- `packet` **BadPacketJ** → `windfall.packet.badpacketj`
- `packet` **BadPacketK** → `windfall.packet.badpacketk`
- `packet` **CrasherA** → `windfall.packet.crashera`
  - `MAX_CHANNEL_LENGTH = 32`
  - `MAX_PAYLOAD_SIZE = 32767`
  - `SUSPICIOUS_PAYLOAD_SIZE = 30000`
- `packet` **InvalidA** → `windfall.packet.invalida`
  - `MAX_PITCH = 90.0`
  - `MIN_PITCH = -90.0`
  - `pitch = relMovePacketWrapper.getPitch()`
- `packet` **PacketOrderA** → `windfall.packet.packetordera`
- `packet` **PacketOrderB** → `windfall.packet.packetorderb`
- `packet` **PacketOrderC** → `windfall.packet.packetorderc`
- `packet` **PacketOrderD** → `windfall.packet.packetorderd`
- `packet` **PacketOrderE** → `windfall.packet.packetordere`
- `packet` **SprintA** → `windfall.packet.sprinta`
- `packet` **SprintB** → `windfall.packet.sprintb`
- `movement` **BaritoneA** → `windfall.movement.baritonea`
  - `dyaw = Math.abs(data.getDeltaYaw())`
  - `normalized = ((data.getYaw() % 360.0F) + 360.0F) % 360.0F`
  - `rem45 = normalized % 45.0F`
- `movement` **BaritoneB** → `windfall.movement.baritoneb`
  - `dyaw = Math.abs(data.getDeltaYaw())`
  - `dpitch = Math.abs(data.getDeltaPitch())`
- `movement` **BaritoneC** → `windfall.movement.baritonec`
  - `now = System.currentTimeMillis()`
- `inventory` **InventoryA** → `windfall.inventory.inventorya`
- `movement` **SimulationA** → `windfall.movement.simulationa`
  - `JUMP_BOOST_PER_LEVEL = 0.1D`
  - `FREEFALL_FROM_ZERO = -PhysicsConstants.GRAVITY * PhysicsConstants.AIR_DRAG_Y`
  - `SLOW_FALL_MIN_Y = -PhysicsConstants.SLOW_FALLING_GRAVITY * PhysicsConstants.AIR_DRAG_Y`
- `movement` **SimulationB** → `windfall.movement.simulationb`
  - `ELYTRA_DRAG_XZ = 0.99D`
  - `ELYTRA_DRAG_Y = 0.98D`
  - `GRAVITY = 0.08D`
- `movement` **SimulationC** → `windfall.movement.simulationc`
  - `LIQUID_GRAVITY = 0.04D`
  - `LIQUID_Y_DRAG = 0.8D`
  - `LIQUID_XZ_DRAG = 0.8D`
- `movement` **SimulationD** → `windfall.movement.simulationd`
  - `BOAT_MAX_XZ = 1.25D`
  - `BOAT_ICE_MAX_XZ = 5.0D`
  - `BOAT_BLUE_ICE_MAX_XZ = 9.5D`
- `movement` **GroundSpoofB** → `windfall.movement.groundspoofb`
- `movement` **GroundSpoofC** → `windfall.movement.groundspoofc`
- `movement` **GroundSpoofD** → `windfall.movement.groundspoofd`
- `movement` **GroundSpoofE** → `windfall.movement.groundspoofe`
  - `serverFallDistance = 0`
- `movement` **GroundSpoofF** → `windfall.movement.groundspooff`
- `movement` **GroundSpoofG** → `windfall.movement.groundspoofg`
- `combat` **AutoClickerA** → `windfall.combat.autoclickera`
  - `SAMPLE_SIZE = 40`
- `combat` **AutoClickerB** → `windfall.combat.autoclickerb`
- `combat` **AutoClickerC** → `windfall.combat.autoclickerc`
- `combat` **AutoClickerD** → `windfall.combat.autoclickerd`
- `combat` **AutoClickerE** → `windfall.combat.autoclickere`
- `combat` **HitboxA** → `windfall.combat.hitboxa`
  - `MAX_BACKTRACK = 8`
  - `PLAYER_EXPANSION = 0.08`
  - `NON_PLAYER_EXPANSION = 0.25`

**Matched with existing Windfall checks:**

- `BFlyA` → `Fly A`
- `BReachA` → `Reach A`
- `BSpeedA` → `Speed A`
- `ScaffoldA` → `Scaffold A`
- `TimerA` → `Timer A`
- `VelocityA` → `Velocity A`
- `VelocityB` → `Velocity A`
- `VelocityC` → `Velocity A`
- `VelocityD` → `Velocity A`
- `AimA` → `Aim A`
- `AimB` → `Aim A`
- `AimD` → `Aim A`
- `AimE` → `Aim A`
- `AimF` → `Aim A`
- `AimG` → `Aim A`
- `AimH` → `Aim A`
- `AimI` → `Aim A`
- `AimJ` → `Aim A`
- `AimK` → `Aim A`
- `AimL` → `Aim A`
- `AnchorAuraA` → `Kill Aura A`
- `CrystalAuraA` → `Kill Aura A`
- `KillAuraB` → `Kill Aura A`
- `KillAuraC` → `Kill Aura A`
- `KillAuraD` → `Kill Aura A`
- `KillAuraE` → `Kill Aura A`
- `KillAuraF` → `Kill Aura A`
- `KillAuraG` → `Kill Aura A`
- `KillAuraH` → `Kill Aura A`
- `ReachA` → `Reach A`

### CloudAC

**Missing from Windfall (1 checks):**

- `packet` **CheckAbilties** → `windfall.packet.abilties`
  - `_ = = net.Dial("tcp", "localhost:1212")`

### Arrow

**Missing from Windfall (24 checks):**

- `movement` **GroundA** → `windfall.movement.grounda`
- `movement` **GroundB** → `windfall.movement.groundb`
- `movement` **GroundC** → `windfall.movement.groundc`
- `packet` **IllegalMoveB** → `windfall.packet.illegalmoveb`
  - `strafeBuffer = 0`
  - `predictedX = lastDeltaX * 0.9100000262260437`
  - `predictedZ = lastDeltaZ * 0.9100000262260437`
- `movement` **MotionA** → `windfall.movement.motiona`
- `movement` **MotionB** → `windfall.movement.motionb`
- `movement` **MotionD** → `windfall.movement.motiond`
- `movement` **MotionE** → `windfall.movement.motione`
- `movement` **MotionF** → `windfall.movement.motionf`
  - `deltaY = md.getDeltaY()`
  - `lastDeltaY = md.getLastDeltaY()`
- `movement` **OmniSprintA** → `windfall.movement.omnisprinta`
  - `GROUND_INVALID_ANGLE = 78.0D`
  - `GROUND_HARD_INVALID_ANGLE = 112.5D`
  - `GROUND_MIN_FORWARD_DOT = 0.20D`
- `packet` **InteractA** → `windfall.packet.interacta`
  - `time = event.getTimestamp()`
  - `lastTime = this.lastTime`
  - `delta = time - lastTime`
- `packet` **InteractC** → `windfall.packet.interactc`
- `inventory` **InventoryA** → `windfall.inventory.inventorya`
- `inventory` **InventoryB** → `windfall.inventory.inventoryb`
  - `time = event.getTimestamp()`
  - `lastTime = this.lastTime`
  - `delta = time - lastTime`
- `movement` **PhaseA** → `windfall.movement.phasea`
  - `PLAYER_HALF_WIDTH = 0.2985D`
  - `PLAYER_HEIGHT = 1.798D`
  - `COLLISION_EPSILON = 0.0015D`
- `packet` **VehicleA** → `windfall.packet.vehiclea`
  - `deltaX = current.getX() - lastVehicleLocation.getX()`
  - `deltaY = current.getY() - lastVehicleLocation.getY()`
  - `deltaZ = current.getZ() - lastVehicleLocation.getZ()`
- `combat` **AutoClickerB** → `windfall.combat.autoclickerb`
- `combat` **AutoClickerC** → `windfall.combat.autoclickerc`
- `combat` **AutoClickerD** → `windfall.combat.autoclickerd`
  - `LN_2 = Math.log(2.0)`
- `combat` **AutoClickerF** → `windfall.combat.autoclickerf`
- `combat` **AutoClickerG** → `windfall.combat.autoclickerg`
- `combat` **MacroA** → `windfall.combat.macroa`
- `combat` **BackTrackA** → `windfall.combat.backtracka`
- `combat` **BackTrackB** → `windfall.combat.backtrackb`

**Matched with existing Windfall checks:**

- `ElytraA` → `Elytra A`
- `FlyA` → `Fly A`
- `FlyB` → `Fly A`
- `FlyC` → `Fly A`
- `SpeedA` → `Speed A`
- `SpeedB` → `Speed A`
- `SpeedC` → `Speed A`
- `ScaffoldA` → `Scaffold A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `TimerA` → `Timer A`
- `TimerB` → `Timer A`
- `AimA` → `Aim A`
- `AimB` → `Aim A`
- `AimC` → `Aim A`
- `AimD` → `Aim A`
- `AimE` → `Aim A`
- `AimF` → `Aim A`
- `AimG` → `Aim A`
- `KillauraA` → `Kill Aura A`
- `ReachA` → `Reach A`
- `VelocityA` → `Velocity A`
- `VelocityB` → `Velocity A`

---

## Summary

- Windfall has **17 checks**
- Found **151 new checks** across competitors that Windfall doesn't have

## Recommendations

1. Review generated skeleton files in `src/main/java/io/windfall/anticheat/core/check/impl/`
2. Implement detection logic based on competitor reference
3. Tune thresholds and buffer values for each check
4. Register new checks in `CheckManager.java`
5. Add config entries to `config.yml`
6. Test on live server before enabling punishable mode
