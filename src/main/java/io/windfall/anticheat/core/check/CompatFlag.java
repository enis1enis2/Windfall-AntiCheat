package io.windfall.anticheat.core.check;

/**
 * Compatibility flags that control check behavior based on server environment.
 *
 * <p>Version flags ({@code VERSION_*}) restrict a check to specific protocol ranges.
 * Platform flags ({@code FOLIA_UNSAFE}, {@code PURPUR_KB_DEPENDENT}) disable checks
 * on incompatible server forks. Mismatch flags ({@code RELAX_ON_MISMATCH}) reduce
 * sensitivity when ViaVersion translation introduces physics drift.
 *
 * @see Check#hasCompatFlag(CompatFlag)
 * @see CheckManager#getSkipReason(Check)
 */
public enum CompatFlag {

    /** Check requires pre-1.9 combat mechanics (protocol &lt; 107) */
    VERSION_LEGACY,
    /** Check requires 1.9–1.12 combat mechanics (protocol 107–340) */
    VERSION_COMBAT,
    /** Check requires 1.13–1.14 flat world format (protocol 393–498) */
    VERSION_FLAT,
    /** Check requires 1.15–1.18.2 world format (protocol 573–758) */
    VERSION_WORLD,
    /** Check requires 1.19–1.20.4 modern mechanics (protocol 759–766) */
    VERSION_MODERN,
    /** Check requires 1.21+ latest mechanics (protocol 767+) */
    VERSION_LATEST,

    /** Check uses APIs unsafe on Folia region threads (e.g., sync entity access) */
    FOLIA_UNSAFE,
    /** Check depends on Purpur-specific knockback values */
    PURPUR_KB_DEPENDENT,
    /** Check requires synchronous chunk access (Paper async chunks may lag) */
    PAPER_CHUNK_DEPENDENT,
    /** Check is sensitive to ViaVersion packet translation errors — relax on mismatch */
    VIAVERSION_SENSITIVE,
    /** Check is sensitive to Geyser Bedrock-to-Java translation errors */
    GEYSEIR_SENSITIVE,

    /** Disable the check entirely when a ViaVersion mismatch is detected */
    DISABLE_ON_MISMATCH,
    /** Reduce check sensitivity (multiply buffer by relaxMultiplier) on ViaVersion mismatch */
    RELAX_ON_MISMATCH
}
