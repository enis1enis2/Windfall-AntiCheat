# Windfall Anti-Cheat Monitor Report

**Generated:** 2026-07-11 14:50:37 UTC

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

**Missing from Windfall (17 checks):**

- `packet` **MultiActionsG** → `windfall.packet.multi actions g`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/multiactions/MultiActionsG.java`
  - `ACTION_INTERACT = 0`
  - `ACTION_ATTACK = 1`
  - `ACTION_SPECTATE_ENTITY = 2`
- `movement` **AirLiquidBreak** → `windfall.movement.air liquid break`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/breaking/AirLiquidBreak.java`
- `movement` **WrongBreak** → `windfall.movement.wrong break`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/breaking/WrongBreak.java`
  - `exemptedY = player.getClientVersion().isOlderThan(ClientVersion.V_1_8) ? 255 : (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14) ? -1 : 4095)`
- `movement` **PositionBreakA** → `windfall.movement.position break a`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/breaking/PositionBreakA.java`
- `movement` **PositionBreakB** → `windfall.movement.position break b`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/breaking/PositionBreakB.java`
- `movement` **MultiBreak** → `windfall.movement.multi break`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/breaking/MultiBreak.java`
  - `face = VerboseCodecs.enumId(blockBreak.face)`
  - `previousFace = VerboseCodecs.enumId(lastFace)`
- `packet` **CrashE** → `windfall.packet.crash e`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/crash/CrashE.java`
- `packet` **CrashG** → `windfall.packet.crash g`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/crash/CrashG.java`
- `packet` **CrashB** → `windfall.packet.crash b`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/crash/CrashB.java`
- `packet` **CrashF** → `windfall.packet.crash f`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/crash/CrashF.java`
- `packet` **CrashC** → `windfall.packet.crash c`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/crash/CrashC.java`
- `movement` **AirLiquidPlace** → `windfall.movement.air liquid place`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/AirLiquidPlace.java`
- `movement` **DuplicateRotPlace** → `windfall.movement.duplicate rot place`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/DuplicateRotPlace.java`
- `movement` **RotationPlace** → `windfall.movement.rotation place`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/RotationPlace.java`
  - `flagBuffer = 0`
  - `distance = player.compensatedEntities.self.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)`
- `movement` **PositionPlace** → `windfall.movement.position place`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/PositionPlace.java`
- `movement` **MultiPlace** → `windfall.movement.multi place`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/MultiPlace.java`
  - `faceId = VerboseCodecs.enumId(face)`
  - `lastFaceId = VerboseCodecs.enumId(lastFace)`
- `packet` **Post** → `windfall.packet.post`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/misc/Post.java`
  - `isExemptFromSwingingCheck = Integer.MIN_VALUE`

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
- `FlightA` → `Fly A`
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
- `MultiActionsA` → `Multi Interact A`
- `MultiActionsC` → `Multi Interact A`
- `MultiActionsB` → `Multi Interact A`
- `MultiActionsD` → `Multi Interact A`
- `MultiActionsE` → `Multi Interact A`
- `MultiActionsF` → `Multi Interact A`
- `Phase` → `Phase A`
- `GroundSpoof` → `Ground Spoof A`
- `AimModulo360` → `Aim A`
- `AimDuplicateLook` → `Aim A`
- `InvalidBreak` → `Invalid Break A`
- `NoSwingBreak` → `No Swing A`
- `FastBreak` → `Fast Break A`
- `FarBreak` → `Far Break A`
- `RotationBreak` → `Rotation Break A`
- `VehicleA` → `Vehicle A`
- `VehicleE` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `VehicleD` → `Vehicle A`
- `VehicleF` → `Vehicle A`
- `Baritone` → `Baritone A`
- `ExploitB` → `Exploit A`
- `ExploitA` → `Exploit A`
- `MultiInteractA` → `Multi Interact A`
- `SelfInteract` → `Self Interact A`
- `Hitboxes` → `Hitboxes A`
- `MultiInteractB` → `Multi Interact A`
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
- `CrashA` → `Crash A`
- `FabricatedPlace` → `Invalid Place A`
- `InvalidPlaceA` → `Invalid Place A`
- `InvalidPlaceB` → `Invalid Place A`
- `FarPlace` → `Far Place A`
- `ClientBrand` → `Client Brand A`

### TruthfulAC

**Missing from Windfall (41 checks):**

- `movement` **MovementCheckSupport** → `windfall.movement.movement check support`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/MovementCheckSupport.java`
- `movement` **GroundSpoofB** → `windfall.movement.ground spoof b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/spoof/GroundSpoofB.java`
- `movement` **GroundSpoofG** → `windfall.movement.ground spoof g`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/spoof/GroundSpoofG.java`
- `movement` **GroundSpoofD** → `windfall.movement.ground spoof d`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/spoof/GroundSpoofD.java`
- `movement` **GroundSpoofC** → `windfall.movement.ground spoof c`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/spoof/GroundSpoofC.java`
- `movement` **GroundSpoofE** → `windfall.movement.ground spoof e`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/spoof/GroundSpoofE.java`
  - `serverFallDistance = 0`
- `movement` **GroundSpoofF** → `windfall.movement.ground spoof f`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/spoof/GroundSpoofF.java`
- `movement` **VelocityD** → `windfall.movement.velocity d`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/velocity/VelocityD.java`
- `movement` **VelocityC** → `windfall.movement.velocity c`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/velocity/VelocityC.java`
- `movement` **VelocityB** → `windfall.movement.velocity b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/velocity/VelocityB.java`
- `movement` **BaritoneB** → `windfall.movement.baritone b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/baritone/BaritoneB.java`
  - `dyaw = Math.abs(data.getDeltaYaw())`
  - `dpitch = Math.abs(data.getDeltaPitch())`
- `movement` **BaritoneC** → `windfall.movement.baritone c`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/baritone/BaritoneC.java`
  - `now = System.currentTimeMillis()`
- `movement` **SimulationC** → `windfall.movement.simulation c`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/simulation/SimulationC.java`
  - `LIQUID_GRAVITY = 0.04D`
  - `LIQUID_Y_DRAG = 0.8D`
  - `LIQUID_XZ_DRAG = 0.8D`
- `movement` **SimulationD** → `windfall.movement.simulation d`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/simulation/SimulationD.java`
  - `BOAT_MAX_XZ = 1.25D`
  - `BOAT_ICE_MAX_XZ = 5.0D`
  - `BOAT_BLUE_ICE_MAX_XZ = 9.5D`
- `movement` **SimulationB** → `windfall.movement.simulation b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/movement/simulation/SimulationB.java`
  - `ELYTRA_DRAG_XZ = 0.99D`
  - `ELYTRA_DRAG_Y = 0.98D`
  - `GRAVITY = 0.08D`
- `packet` **RaycastA** → `windfall.packet.raycast a`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/raycast/RaycastA.java`
- `combat` **AnchorAuraA** → `windfall.combat.anchor aura a`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/anchor/AnchorAuraA.java`
- `combat` **CrystalAuraA** → `windfall.combat.crystal aura a`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/crystal/CrystalAuraA.java`
  - `lastCleanup = System.currentTimeMillis()`
- `combat` **AimH** → `windfall.combat.aim h`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/aim/AimH.java`
  - `SAMPLE_SIZE = 20`
  - `OSCILLATION_THRESHOLD = 0.7`
- `combat` **AimL** → `windfall.combat.aim l`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/aim/AimL.java`
  - `MIN_AXIS_LOCK_YAW = 4.0f`
  - `MAX_AXIS_LOCK_PITCH = 0.01f`
  - `MIN_LINEAR_DELTA = 1.0f`
- `combat` **AimF** → `windfall.combat.aim f`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/aim/AimF.java`
  - `SAMPLE_SIZE = 25`
- `combat` **AimD** → `windfall.combat.aim d`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/aim/AimD.java`
  - `deltaYaw = Math.abs(playerData.getDeltaYaw())`
  - `deltaPitch = Math.abs(playerData.getDeltaPitch())`
  - `yawAccel = Math.abs(deltaYaw - Math.abs(playerData.getLastDeltaYaw()))`
- `combat` **AimG** → `windfall.combat.aim g`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/aim/AimG.java`
  - `SAMPLE_SIZE = 12`
- `combat` **AimK** → `windfall.combat.aim k`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/aim/AimK.java`
  - `MIN_DELTA = 0.35`
  - `EXPANDER = 16777216.0`
  - `MIN_VALID_STEP = 0.001`
- `combat` **AimE** → `windfall.combat.aim e`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/aim/AimE.java`
  - `WINDOW_SIZE = 30`
- `combat` **AimJ** → `windfall.combat.aim j`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/aim/AimJ.java`
  - `SAMPLE_SIZE = 20`
  - `MIN_ROTATION = 0.5`
- `combat` **AimI** → `windfall.combat.aim i`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/aim/AimI.java`
  - `SAMPLE_SIZE = 25`
  - `SUSPICION_THRESHOLD = 0.75`
- `combat` **AimB** → `windfall.combat.aim b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/aim/AimB.java`
  - `HEAD_SNAP_THRESHOLD = 50.0F`
  - `SMOOTH_ACCEL_THRESHOLD = 0.001F`
  - `SNAP_PATTERN_THRESHOLD = 5`
- `combat` **HitboxA** → `windfall.combat.hitbox a`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/hitbox/HitboxA.java`
  - `MAX_BACKTRACK = 8`
  - `PLAYER_EXPANSION = 0.08`
  - `NON_PLAYER_EXPANSION = 0.25`
- `combat` **KillAuraG** → `windfall.combat.kill aura g`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/killaura/KillAuraG.java`
  - `MAX_TARGETS_WINDOW = 10`
  - `TIME_WINDOW_MS = 500L`
  - `SUSPICIOUS_TARGET_COUNT = 3`
- `combat` **KillAuraC** → `windfall.combat.kill aura c`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/killaura/KillAuraC.java`
- `combat` **KillAuraH** → `windfall.combat.kill aura h`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/killaura/KillAuraH.java`
  - `SAMPLE_SIZE = 15`
  - `VARIANCE_THRESHOLD = 25.0`
  - `QUANTIZATION_THRESHOLD = 0.85`
- `combat` **KillAuraD** → `windfall.combat.kill aura d`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/killaura/KillAuraD.java`
- `combat` **KillAuraF** → `windfall.combat.kill aura f`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/killaura/KillAuraF.java`
- `combat` **KillAuraE** → `windfall.combat.kill aura e`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/killaura/KillAuraE.java`
  - `STEP = 0.2`
  - `MAX_RAY_LENGTH = 6.0`
  - `currentTick = Bukkit.getCurrentTick()`
- `combat` **KillAuraB** → `windfall.combat.kill aura b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/killaura/KillAuraB.java`
- `combat` **AutoClickerB** → `windfall.combat.auto clicker b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/autoclicker/AutoClickerB.java`
- `combat` **AutoClickerC** → `windfall.combat.auto clicker c`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/autoclicker/AutoClickerC.java`
- `combat` **AutoClickerE** → `windfall.combat.auto clicker e`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/autoclicker/AutoClickerE.java`
- `combat` **AutoClickerD** → `windfall.combat.auto clicker d`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/autoclicker/AutoClickerD.java`
- `packet` **CrasherA** → `windfall.packet.crasher a`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/crasher/CrasherA.java`
  - `MAX_CHANNEL_LENGTH = 32`
  - `MAX_PAYLOAD_SIZE = 32767`
  - `SUSPICIOUS_PAYLOAD_SIZE = 30000`

**Matched with existing Windfall checks:**

- `VelocityA` → `Velocity A`
- `BaritoneA` → `Baritone A`
- `InventoryA` → `Inventory A`
- `SimulationA` → `Simulation A`
- `ScaffoldA` → `Scaffold A`
- `FastBreakA` → `Fast Break A`
- `PhaseA` → `Phase A`
- `BReachA` → `Reach A`
- `BSpeedA` → `Speed A`
- `BFlyA` → `Fly A`
- `AimA` → `Aim A`
- `ReachA` → `Reach A`
- `AutoClickerA` → `Autoclicker A`
- `SprintA` → `Sprint A`
- `SprintB` → `Sprint A`
- `InvalidA` → `Invalid Place A`
- `TimerA` → `Timer A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `BadPacketA` → `Bad Packets A`
- `BadPacketE` → `Bad Packets A`
- `BadPacketG` → `Bad Packets A`
- `BadPacketD` → `Bad Packets A`
- `BadPacketJ` → `Bad Packets A`
- `BadPacketI` → `Bad Packets A`
- `BadPacketC` → `Bad Packets A`
- `BadPacketK` → `Bad Packets A`
- `BadPacketH` → `Bad Packets A`

### CloudAC

**Missing from Windfall (1 checks):**

- `packet` **CheckAbilties** → `windfall.packet.abilties`
  - Source: `ComputationServer/sys/checks.go`
  - `_ = = net.Dial("tcp", "localhost:1212")`

### Arrow

**Missing from Windfall (28 checks):**

- `movement` **GroundA** → `windfall.movement.ground a`
  - Source: `src/main/java/me/arrow/checks/impl/movement/ground/GroundA.java`
- `movement` **GroundB** → `windfall.movement.ground b`
  - Source: `src/main/java/me/arrow/checks/impl/movement/ground/GroundB.java`
- `movement` **GroundC** → `windfall.movement.ground c`
  - Source: `src/main/java/me/arrow/checks/impl/movement/ground/GroundC.java`
- `movement` **MotionB** → `windfall.movement.motion b`
  - Source: `src/main/java/me/arrow/checks/impl/movement/motion/MotionB.java`
- `movement` **MotionF** → `windfall.movement.motion f`
  - Source: `src/main/java/me/arrow/checks/impl/movement/motion/MotionF.java`
  - `deltaY = md.getDeltaY()`
  - `lastDeltaY = md.getLastDeltaY()`
- `movement` **MotionD** → `windfall.movement.motion d`
  - Source: `src/main/java/me/arrow/checks/impl/movement/motion/MotionD.java`
- `movement` **MotionE** → `windfall.movement.motion e`
  - Source: `src/main/java/me/arrow/checks/impl/movement/motion/MotionE.java`
- `movement` **IllegalMoveB** → `windfall.movement.illegal move b`
  - Source: `src/main/java/me/arrow/checks/impl/movement/illegalmove/IllegalMoveB.java`
  - `strafeBuffer = 0`
  - `predictedX = lastDeltaX * 0.9100000262260437`
  - `predictedZ = lastDeltaZ * 0.9100000262260437`
- `movement` **SpeedC** → `windfall.movement.speed c`
  - Source: `src/main/java/me/arrow/checks/impl/movement/speed/SpeedC.java`
  - `maxBuffer1 = 13`
  - `resetRate1 = 0.4`
- `movement` **SpeedB** → `windfall.movement.speed b`
  - Source: `src/main/java/me/arrow/checks/impl/movement/speed/SpeedB.java`
  - `AIR_ICE_INCREMENT_PER_TICK = 0.1525`
  - `AIR_ICE_INCREMENT_PER_TICK_SMALLER = 0.06`
  - `AIR_MAX_ICE_SPEED_BOOST = 3.75`
- `movement` **FlyB** → `windfall.movement.fly b`
  - Source: `src/main/java/me/arrow/checks/impl/movement/fly/FlyB.java`
  - `deviation = getDevation(this.samples)`
  - `average = getAverage(this.underBlockSamples)`
- `movement` **FlyC** → `windfall.movement.fly c`
  - Source: `src/main/java/me/arrow/checks/impl/movement/fly/FlyC.java`
  - `JUMP_TOL = 0.06`
  - `serverAirTicks = movementData.getServerAirTicks()`
  - `clientAirTicks = movementData.getClientAirTicks()`
- `combat` **BackTrackB** → `windfall.combat.back track b`
  - Source: `src/main/java/me/arrow/checks/impl/combat/backtrack/BackTrackB.java`
- `movement` **VelocityB** → `windfall.movement.velocity b`
  - Source: `src/main/java/me/arrow/checks/impl/combat/velocity/VelocityB.java`
- `combat` **AutoClickerG** → `windfall.combat.auto clicker g`
  - Source: `src/main/java/me/arrow/checks/impl/combat/autoclicker/AutoClickerG.java`
- `combat` **AutoClickerB** → `windfall.combat.auto clicker b`
  - Source: `src/main/java/me/arrow/checks/impl/combat/autoclicker/AutoClickerB.java`
- `combat` **AutoClickerC** → `windfall.combat.auto clicker c`
  - Source: `src/main/java/me/arrow/checks/impl/combat/autoclicker/AutoClickerC.java`
- `combat` **AutoClickerD** → `windfall.combat.auto clicker d`
  - Source: `src/main/java/me/arrow/checks/impl/combat/autoclicker/AutoClickerD.java`
  - `LN_2 = Math.log(2.0)`
- `combat` **AutoClickerF** → `windfall.combat.auto clicker f`
  - Source: `src/main/java/me/arrow/checks/impl/combat/autoclicker/AutoClickerF.java`
- `combat` **AimF** → `windfall.combat.aim f`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/AimF.java`
  - `eps = 1.0E-9D`
- `combat` **AimD** → `windfall.combat.aim d`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/AimD.java`
- `combat` **AimG** → `windfall.combat.aim g`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/AimG.java`
- `combat` **AimE** → `windfall.combat.aim e`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/AimE.java`
- `combat` **AimC** → `windfall.combat.aim c`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/AimC.java`
- `combat` **AimB** → `windfall.combat.aim b`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/AimB.java`
- `movement` **ScaffoldB** → `windfall.movement.scaffold b`
  - Source: `src/main/java/me/arrow/checks/impl/misc/scaffold/ScaffoldB.java`
  - `MAX_SAMPLES = 80`
  - `MIN_SAMPLES = 48`
  - `PLACE_CONTEXT_TICKS = 8`
- `movement` **ScaffoldC** → `windfall.movement.scaffold c`
  - Source: `src/main/java/me/arrow/checks/impl/misc/scaffold/ScaffoldC.java`
  - `CONFIRMED_PLACE_CONTEXT_TICKS = 8`
  - `REQUIRED_CONFIRMED_BRIDGE_BLOCKS = 7`
  - `BRIDGE_STREAK_TIMEOUT_TICKS = 28`
- `movement` **TimerB** → `windfall.movement.timer b`
  - Source: `src/main/java/me/arrow/checks/impl/misc/timer/TimerB.java`
  - `lastPacket = -1337L`

**Matched with existing Windfall checks:**

- `MotionA` → `Motion A`
- `SpeedA` → `Speed A`
- `OmniSprintA` → `Sprint A`
- `ElytraA` → `Elytra A`
- `FlyA` → `Fly A`
- `BackTrackA` → `Backtrack A`
- `VelocityA` → `Velocity A`
- `ReachA` → `Reach A`
- `KillauraA` → `Kill Aura A`
- `MacroA` → `Macro A`
- `AimA` → `Aim A`
- `InteractC` → `Self Interact A`
- `InteractA` → `Self Interact A`
- `ScaffoldA` → `Scaffold A`
- `VehicleA` → `Vehicle A`
- `InventoryA` → `Inventory A`
- `InventoryB` → `Inventory A`
- `TimerA` → `Timer A`
- `PhaseA` → `Phase A`

---

## Summary

- Windfall has **44 checks**
- Found **87 new checks** across competitors that Windfall doesn't have

## Recommendations

1. Review generated skeleton files in `src/main/java/io/windfall/anticheat/core/check/impl/`
2. Implement detection logic based on competitor reference
3. Tune thresholds and buffer values for each check
4. Register new checks in `CheckManager.java`
5. Add config entries to `config.yml`
6. Test on live server before enabling punishable mode
