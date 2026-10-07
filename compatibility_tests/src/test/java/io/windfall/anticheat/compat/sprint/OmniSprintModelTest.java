package io.windfall.anticheat.compat.sprint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OmniSprintModelTest {

    private static final double SPRINT_SPEED = 0.286D;

    private OmniSprintModel.Context baseContext() {
        OmniSprintModel.Context ctx = new OmniSprintModel.Context();
        ctx.baseSpeed = SPRINT_SPEED;
        ctx.sprinting = true;
        ctx.lastSprinting = true;
        ctx.packetMoving = true;
        ctx.onGround = true;
        ctx.serverGround = true;
        ctx.clientGroundTicks = 10;
        ctx.serverGroundTicksPlus = 10;
        return ctx;
    }

    @Test
    void cleanForwardSprintNeverFlags() {
        OmniSprintModel model = new OmniSprintModel();
        for (int i = 0; i < 40; i++) {
            OmniSprintModel.Context ctx = baseContext();
            ctx.deltaX = 0.0D;
            ctx.deltaZ = 0.286D;
            ctx.yaw = 0.0D;
            ctx.lastYaw = 0.0D;

            OmniSprintModel.Result result = model.handle(ctx);

            assertFalse(result.groundFlag, "forward sprint flagged at tick " + i);
            assertFalse(result.airFlag, "forward sprint flagged air at tick " + i);
        }
        assertEquals(0.0D, model.getGroundBuffer(), 1.0E-9D);
    }

    @Test
    void repeatedSidewaysSprintFlagsGround() {
        OmniSprintModel model = new OmniSprintModel();
        boolean flagged = false;
        int flaggedAt = -1;

        for (int i = 0; i < 20; i++) {
            OmniSprintModel.Context ctx = baseContext();
            ctx.deltaX = 0.3D;
            ctx.deltaZ = 0.0D;
            ctx.yaw = 0.0D;
            ctx.lastYaw = 0.0D;

            OmniSprintModel.Result result = model.handle(ctx);

            if (result.groundFlag) {
                flagged = true;
                flaggedAt = i;
                break;
            }
        }

        assertTrue(flagged, "sideways sprint should have flagged");
        assertTrue(flaggedAt <= 12, "expected flag within 12 ticks, flagged at " + flaggedAt);
        assertTrue(model.getSprintTicks() > 0, "sprint ticks keep accumulating while sprinting");
        assertEquals(4.0D, model.getGroundBuffer(), 1.0E-9D, "buffer resets to 4 after ground flag");
        assertEquals(0, model.getGroundInvalidTicks());
    }

    @Test
    void repeatedSidewaysSprintFlagsAir() {
        OmniSprintModel model = new OmniSprintModel();
        boolean flagged = false;

        for (int i = 0; i < 20; i++) {
            OmniSprintModel.Context ctx = baseContext();
            ctx.onGround = false;
            ctx.serverGround = false;
            ctx.clientGroundTicks = 0;
            ctx.serverGroundTicksPlus = 0;
            ctx.clientAirTicks = 5;
            ctx.serverAirTicks = 5;
            ctx.deltaX = 0.3D;
            ctx.deltaZ = 0.0D;
            ctx.yaw = 0.0D;
            ctx.lastYaw = 0.0D;

            OmniSprintModel.Result result = model.handle(ctx);

            if (result.airFlag) {
                flagged = true;
                break;
            }
        }

        assertTrue(flagged, "sideways air sprint should have flagged");
        assertEquals(3.0D, model.getAirBuffer(), 1.0E-9D, "buffer resets to 3 after air flag");
    }

    @Test
    void backdashSprintFlagsGround() {
        OmniSprintModel model = new OmniSprintModel();
        boolean flagged = false;

        for (int i = 0; i < 20; i++) {
            OmniSprintModel.Context ctx = baseContext();
            ctx.deltaX = 0.0D;
            ctx.deltaZ = -0.3D;
            ctx.yaw = 0.0D;
            ctx.lastYaw = 0.0D;

            OmniSprintModel.Result result = model.handle(ctx);

            if (result.groundFlag) {
                flagged = true;
                break;
            }
        }

        assertTrue(flagged, "sprinting straight backwards should have flagged ground");
    }

    @Test
    void notSprintingResetsStateAndRewards() {
        OmniSprintModel model = new OmniSprintModel();

        for (int i = 0; i < 6; i++) {
            OmniSprintModel.Context ctx = baseContext();
            ctx.deltaX = 0.3D;
            ctx.deltaZ = 0.0D;
            ctx.yaw = 0.0D;
            model.handle(ctx);
        }

        double raisedBuffer = model.getGroundBuffer();
        assertTrue(raisedBuffer > 0.0D);

        for (int i = 0; i < 10; i++) {
            OmniSprintModel.Context ctx = baseContext();
            ctx.sprinting = false;
            ctx.lastSprinting = false;
            ctx.deltaX = 0.3D;
            ctx.deltaZ = 0.0D;
            ctx.yaw = 0.0D;
            model.handle(ctx);
        }

        assertEquals(0, model.getSprintTicks());
        assertEquals(0.0D, model.getGroundBuffer(), 1.0E-9D, "not sprinting drains the ground buffer by 0.75 per tick");
    }

    @Test
    void environmentExemptionsNeverFlag() {
        OmniSprintModel model = new OmniSprintModel();

        for (int i = 0; i < 25; i++) {
            OmniSprintModel.Context ctx = baseContext();
            ctx.deltaX = 0.3D;
            ctx.deltaZ = 0.0D;
            ctx.yaw = 0.0D;
            ctx.insideWater = true;

            OmniSprintModel.Result result = model.handle(ctx);

            assertFalse(result.groundFlag, "water exemption flagged at tick " + i);
            assertFalse(result.airFlag, "water exemption flagged air at tick " + i);
        }
        assertEquals(0.0D, model.getGroundBuffer(), 1.0E-9D);
    }

    @Test
    void turningMomentGivesGracePeriod() {
        OmniSprintModel model = new OmniSprintModel();

        boolean flaggedWhileTurning = false;

        for (int i = 0; i < 6; i++) {
            OmniSprintModel.Context ctx = baseContext();
            ctx.deltaX = 0.3D;
            ctx.deltaZ = 0.0D;
            ctx.yaw = i * 10.0D;
            ctx.lastYaw = 0.0D;
            ctx.deltaYaw = 10.0D;

            OmniSprintModel.Result result = model.handle(ctx);

            if (result.groundFlag) {
                flaggedWhileTurning = true;
                break;
            }
        }

        assertFalse(flaggedWhileTurning, "grace period must absorb small early turns");
    }
}