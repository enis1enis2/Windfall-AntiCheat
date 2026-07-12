package io.windfall.anticheat.core.check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares static metadata for an anti-cheat check.
 *
 * <p>Every check class must be annotated with {@code @CheckData}. The {@link Check}
 * base class reads this annotation via {@code getAnnotation()} at construction time
 * to populate its fields. Missing this annotation causes a startup crash.
 *
 * <p>Example:
 * <pre>{@code
 * @CheckData(
 *     name = "Speed A",
 *     stableKey = "windfall.movement.speed",
 *     decay = 0.01,
 *     setbackVl = 15,
 *     compat = {CompatFlag.VIAVERSION_SENSITIVE}
 * )
 * public class SpeedCheck extends Check implements PacketCheck { ... }
 * }</pre>
 *
 * @see Check#Check() constructor reads this annotation
 * @see CompatFlag for compatibility flag values
 */
// RUNTIME retention required — Check constructor reads this via getAnnotation() at startup
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckData {

    /** Human-readable check name shown in alerts (e.g., "Speed A") */
    String name();

    /** Unique key mapping 1:1 to config.yml — never rename without updating all config files */
    String stableKey();

    /** Buffer decay rate per tick — higher = faster confidence decay (default 0.02) */
    double decay() default 0.02;

    /** Violation level threshold that triggers a setback teleport (default 20) */
    int setbackVl() default 20;

    /** Minimum server protocol version to enable this check (default 4 = 1.7.5) */
    int minVersion() default 4;

    /** Maximum server protocol version to enable this check (default 99999 = unlimited) */
    int maxVersion() default 99999;

    /** Compatibility flags controlling version/platform/mismatch behavior */
    CompatFlag[] compat() default {};

    /** Buffer multiplier applied when RELAX_ON_MISMATCH is active (default 1.0 = no relaxation) */
    double relaxMultiplier() default 1.0;

    /** If true, this check is automatically disabled on Folia servers */
    boolean disableOnFolia() default false;

    /** If true, this check is automatically disabled on Purpur servers */
    boolean disableOnPurpur() default false;
}
