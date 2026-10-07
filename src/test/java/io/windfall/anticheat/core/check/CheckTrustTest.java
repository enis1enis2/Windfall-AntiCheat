package io.windfall.anticheat.core.check;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import io.windfall.anticheat.compat.trust.TrustFactorModel;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the trust-model wiring between {@link Check} and {@link WindfallPlayer}.
 *
 * <p>The trust factor lives on the player (so it survives across checks) and the base check
 * adjusts it: a flag lowers trust, the per-tick {@link Check#reward} raises it back, and
 * {@link Check#trustAdjustedThreshold} translates the rank into threshold scaling used by the
 * ported detection models.
 */
class CheckTrustTest extends CheckTestBase {

    @CheckData(name = "Trust A", stableKey = "windfall.test.trust")
    private static final class TrustCheck extends Check implements PacketCheck {
        @Override
        public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        }

        @Override
        public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
        }
    }

    private TrustCheck createCheck() {
        return new TrustCheck();
    }

    /** Builds a real player (not a mock) so its trust model actually mutates. */
    private WindfallPlayer realPlayer() {
        org.bukkit.entity.Player bukkit = mock(org.bukkit.entity.Player.class);
        when(bukkit.getUniqueId()).thenReturn(UUID.randomUUID());
        when(bukkit.getName()).thenReturn("Trusted");
        when(bukkit.isOnline()).thenReturn(true);
        when(bukkit.getWorld()).thenReturn(mock(org.bukkit.World.class));

        ClientVersion clientVersion = mock(ClientVersion.class);
        when(clientVersion.getProtocolVersion()).thenReturn(767);
        User user = mock(User.class);
        when(user.getClientVersion()).thenReturn(clientVersion);
        return new WindfallPlayer(bukkit, user);
    }

    @Test
    void windfallPlayer_exposesTrustModel() {
        WindfallPlayer player = realPlayer();
        TrustFactorModel trust = player.getTrustFactor();
        assertNotNull(trust);
        assertEquals(0.0, trust.getTrust(), 1e-9);
        assertEquals(TrustFactorModel.TrustRank.SUSPICIOUS, trust.getRank());
    }

    @Test
    void flag_decreasesTrust() {
        TrustCheck check = createCheck();
        WindfallPlayer player = realPlayer();

        check.flag(player);
        check.flag(player);

        assertEquals(-2.0, player.getTrustFactor().getTrust(), 1e-9);
    }

    @Test
    void repeatedFlags_dropPlayerIntoUntrustworthyRank() {
        TrustCheck check = createCheck();
        WindfallPlayer player = realPlayer();

        for (int i = 0; i < 21; i++) {
            check.flag(player);
        }

        assertEquals(-21.0, player.getTrustFactor().getTrust(), 1e-9);
        assertEquals(TrustFactorModel.TrustRank.UNTRUSTWORTHY, player.getTrustFactor().getRank());
    }

    @Test
    void reward_restoresTrust() {
        TrustCheck check = createCheck();
        WindfallPlayer player = realPlayer();

        check.flag(player);
        check.reward(player);

        assertEquals(0.0, player.getTrustFactor().getTrust(), 1e-9,
            "one clean tick should cancel one flag's trust penalty");
    }

    @Test
    void trustAdjustedThreshold_noTrustModel_returnsBase() {
        TrustCheck check = createCheck();
        // Base mock player: getTrustFactor() is unstubbed (null) — treated as neutral.
        WindfallPlayer player = createMockPlayer("Mock");
        assertEquals(2.0, check.trustAdjustedThreshold(player, 2.0), 1e-9);
    }

    @Test
    void trustAdjustedThreshold_superUntrustworthy_halvesThreshold() {
        TrustCheck check = createCheck();
        WindfallPlayer player = realPlayer();
        player.getTrustFactor().setTrust(-60.0);

        assertEquals(1.0, check.trustAdjustedThreshold(player, 2.0), 1e-9);
    }

    @Test
    void trustAdjustedThreshold_untrustworthy_tightensThreshold() {
        TrustCheck check = createCheck();
        WindfallPlayer player = realPlayer();
        player.getTrustFactor().setTrust(-30.0);

        assertEquals(1.5, check.trustAdjustedThreshold(player, 2.0), 1e-9);
    }

    @Test
    void trustAdjustedThreshold_normalAndSuspicious_keepBase() {
        TrustCheck check = createCheck();
        WindfallPlayer suspicious = realPlayer();
        suspicious.getTrustFactor().setTrust(0.0);

        WindfallPlayer normal = realPlayer();
        normal.getTrustFactor().setTrust(40.0);

        assertEquals(2.0, check.trustAdjustedThreshold(suspicious, 2.0), 1e-9);
        assertEquals(2.0, check.trustAdjustedThreshold(normal, 2.0), 1e-9);
    }

    @Test
    void trustAdjustedThreshold_legit_raisesThreshold() {
        TrustCheck check = createCheck();
        WindfallPlayer player = realPlayer();
        player.getTrustFactor().setTrust(90.0);

        assertEquals(2.6, check.trustAdjustedThreshold(player, 2.0), 1e-9);
    }

    @Test
    void trustAdjustedThreshold_trusted_raisesThreshold() {
        TrustCheck check = createCheck();
        WindfallPlayer player = realPlayer();
        player.getTrustFactor().setTrust(70.0);

        assertEquals(2.2, check.trustAdjustedThreshold(player, 2.0), 1e-9);
    }
}