# Contributing to Windfall Anti-Cheat

Thank you for considering contributing to Windfall! Every contribution helps make anti-cheat detection more accurate and compatible across Minecraft versions.

**First off, thank you for considering contributing. It's people like you that make Windfall such a great tool.**

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [ Ways to Contribute](#ways-to-contribute)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Submit a Contribution](#how-to-submit-a-contribution)
- [Commit Message Format](#commit-message-format)
- [Code Style](#code-style)
- [Testing](#testing)
- [Pull Request Process](#pull-request-process)
- [Reporting Bugs](#reporting-bugs)
- [Suggesting Features](#suggesting-features)
- [Community](#community)

---

## Code of Conduct

This project adheres to the [Contributor Covenant](https://www.contributor-covenant.org/). By participating, you are expected to uphold this code. Be respectful, constructive, and inclusive in all interactions.

---

## Ways to Contribute

You don't have to write code to contribute! Windfall welcomes all types of contributions:

### Code
- Fix a bug you've found
- Implement a new anti-cheat check
- Improve detection accuracy
- Add compatibility for a new server fork or MC version
- Improve test coverage

### Documentation
- Fix typos or unclear explanations in README, Javadoc, or config comments
- Write tutorials or usage guides
- Translate documentation

### Testing
- Test Windfall on different MC versions (1.7 through 26.2+) and server forks (Spigot, Paper, Folia, Purpur)
- Report false positives or missed detections
- Verify physics accuracy against vanilla behavior

### Design
- Improve the website (docs/)
- Create graphics or icons

### Community
- Help answer questions in issues
- Review pull requests
- Share feedback from real server environments

---

## Getting Started

1. **Fork** the repository on GitHub
2. **Clone** your fork locally
3. **Set up** your development environment (see below)
4. **Create a branch** for your change
5. **Make your changes** following the guidelines below
6. **Test** your changes
7. **Commit** with the correct format
8. **Push** to your fork and open a Pull Request

---

## Development Setup

### Prerequisites

- **Java 11+** (JDK 17 recommended for development)
- **Maven 3.9+**
- **Git**
- A Minecraft server for testing (Paper 1.21+ recommended)

### Build

```bash
git clone https://github.com/<your-username>/Windfall-AntiCheat.git
cd Windfall-AntiCheat
mvn clean package
```

The built JAR will be at `target/Windfall.jar`.

### Run Tests

```bash
mvn test
```

All 515 tests must pass before submitting a PR.

### Project Structure

```
src/main/java/io/windfall/anticheat/
  WindfallPlugin.java              — Plugin entry point
  core/
    check/                         — All 52 anti-cheat checks
      impl/
        combat/                    — 12 combat checks
        movement/                  — 29 movement checks
        packet/                    — 10 packet checks
        inventory/                 — 1 inventory check
    physics/                       — Prediction engine, bounding boxes, MC physics
    player/                        — Per-player state tracking
    version/                       — MC version detection and compatibility
    platform/                      — Folia/Purpur compatibility
    compensation/                  — Packet delay compensation
    config/                        — Configuration management
```

### Key Dependencies

| Dependency | Version | Scope | Notes |
|-----------|---------|-------|-------|
| `spigot-api` | 1.8-R0.1-SNAPSHOT | provided | Compile against oldest supported API |
| `packetevents-spigot` | 2.13.0 | provided | External plugin, never shaded |
| `geyser api` | 2.10.0-SNAPSHOT | provided | Bedrock player detection |
| `worldguard-bukkit` | 7.0.14 | provided | Region-based exemptions |
| `junit-jupiter` | 6.1.1 | test | |
| `mockito-core` | 5.23.0 | test | |

---

## How to Submit a Contribution

### Small Fixes (typos, broken links, obvious errors)

1. Fork the repo
2. Create a branch: `git checkout -b fix/typo-in-readme`
3. Make the fix
4. Commit with the correct format (see below)
5. Push and open a PR

### Substantial Contributions (new checks, major features)

**Open an issue first** to discuss your idea before investing time. This ensures your contribution aligns with Windfall's goals and avoids duplicate work.

When proposing a new anti-cheat check, include:
- What cheat behavior does it detect?
- How does it work at the packet level?
- What MC versions should it support?
- Are there known false positive scenarios?

---

## Commit Message Format

Windfall uses a strict commit format. **This is enforced by the CI release workflow.**

### Format

```
V:x.y.x + Short summary
```

### Rules

- **`V:`** prefix — capital V, colon, no space before version
- **`x.y.x`** — version from pom.xml (you MUST bump pom.xml version before committing)
- **`+`** separator
- **Summary** — 3-8 words describing the change

### Examples

```
V:1.9.2 + Fix ReachCheck memory leak
V:1.10.0 + Add new VelocityCheck
V:1.10.1 + Update thread safety
V:1.11.0 + Add PhaseCheck for wall clipping
```

### What NOT to do

```
fix: something              (wrong format)
v1.9.2 fixed bug            (lowercase v, no colon)
V:1.9.2                     (no summary after +)
Update README               (no version prefix)
```

**Note:** Only commits matching `V:x.y.x + Summary` trigger a GitHub Release. The CI extracts the version and creates a tag `v1.9.2`.

---

## Code Style

### General Rules

- **Java 11 only** — no Java 12+ features (no `var`, no `switch` expressions, no text blocks)
- **Target Java 11** — `pom.xml` uses `source=11, target=11`
- **No external dependencies at runtime** — PacketEvents is provided, never shaded
- **Reflection over version checks** — when calling version-specific APIs, use reflection with try/fallback patterns

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Check classes | `PascalCaseCheck` | `ReachCheck`, `FlightCheck` |
| Check names | `@CheckData(name="...")` | `name="Reach A"` |
| Stable keys | `windfall.category.name` | `windfall.combat.reach` |
| Test classes | `PascalCaseTest` | `ReachCheckTest` |
| Variables | `camelCase` | `playerVelocity` |

### Check Implementation Pattern

Every check must:

1. Extend `Check` (or implement `PacketCheck` for packet checks)
2. Annotate with `@CheckData` specifying name, stableKey, decay, setbackVl, version range
3. Handle all 6 version brackets (LEGACY through LATEST) where applicable
4. Use reflection for APIs not available in spigot-api 1.8
5. Clean up player data in `removePlayer(UUID)`

```java
@CheckData(
    name = "Reach A",
    stableKey = "windfall.combat.reach",
    decay = 0.05,
    setbackVl = 10
)
public class ReachCheck extends Check {
    // ...
}
```

### Thread Safety

- Use **immutable snapshot patterns** for shared state
- Publish state via `volatile` references
- Never synchronize on player state objects
- Use `ConcurrentHashMap` for player maps

### Compatibility

- **All code must compile against spigot-api 1.8** — even if it targets newer versions
- Use `ServerFork` and `VersionBracket` for version-specific behavior
- Use `@CheckData` compat flags for fork/version gating
- Use reflection for APIs added after 1.8 (e.g., `BlockData`, `PotionEffectType.getKey()`)

---

## Testing

### Requirements

- All **515 existing tests must pass** before submitting
- New checks should include unit tests
- Tests use JUnit 6.1.1 + Mockito 5.23.0

### Running Tests

```bash
mvn test
```

### Writing Tests

- Test class name: `<CheckName>Test.java`
- Location: `src/test/java/io/windfall/anticheat/`
- Mock `WindfallPlayer` and packet events
- Test both flagging (cheat detected) and reward (legitimate player)
- Test version-specific behavior where applicable

### Test Naming

```java
@Test
void testReachExceedsMaxDistance_flags() { ... }

@Test
void testReachWithinDistance_noFlag() { ... }
```

---

## Pull Request Process

### Before Submitting

- [ ] Code compiles with `mvn clean compile`
- [ ] All tests pass with `mvn test`
- [ ] No new warnings or deprecations
- [ ] Commit follows `V:x.y.x + Summary` format
- [ ] pom.xml version bumped (if applicable)
- [ ] New checks registered in `CheckManager.java`
- [ ] New checks have `removePlayer()` override for cleanup

### PR Description

Include in your PR:

1. **What** — brief description of the change
2. **Why** — which goal it serves (detection accuracy or compatibility)
3. **How** — technical approach (especially for new checks)
4. **Testing** — what MC versions/forks you tested on
5. **Closes** — reference any related issues (`Closes #42`)

### Review Process

1. CI must pass (build + tests)
2. Maintainer reviews for correctness, style, and compatibility
3. May request changes — please be responsive
4. Once approved, maintainer merges

### What to Expect

- **Response time:** Usually within a few days
- **Feedback:** Constructive and specific
- **Changes requested:** Common — don't be discouraged, it's part of the process
- **Not accepted:** Possible if it doesn't align with project goals — you can always fork

---

## Reporting Bugs

When reporting a bug, please include:

1. **Windfall version** (from `/windfall help` or plugin list)
2. **Server software and version** (e.g., Paper 1.21.4, Spigot 1.20.1)
3. **Java version** (`/version` output)
4. **Installed plugins** (especially PacketEvents version, ViaVersion, Geyser)
5. **Steps to reproduce** the issue
6. **Expected behavior** vs **actual behavior**
7. **Console logs** (relevant errors or warnings)
8. **Config** (if modified from defaults)

### False Positives

False positive reports are especially valuable. When reporting:

- Describe the player action that triggered the false flag
- Include the check name from the alert message
- Share the console output showing the VL increase
- Note the player's MC version and connection type (Java/Bedrock)

---

## Suggesting Features

Feature suggestions are welcome! Please open an issue with:

1. **Problem** — what cheat behavior isn't currently detected?
2. **Proposed solution** — how should the check work?
3. **Alternatives** — other approaches you've considered
4. **Compatibility** — which MC versions should it support?
5. **Priority** — how critical is this detection?

### New Check Proposals

For new anti-cheat checks, include:

- **Packet analysis** — which packets does the cheat modify?
- **Detection logic** — what mathematical or behavioral analysis catches it?
- **Thresholds** — what values distinguish cheats from legitimate play?
- **Edge cases** — pistons, elytra, swimming, ice, soul sand, etc.
- **False positive risks** — what legitimate behaviors might trigger it?

---

## Project Goals

Every contribution must serve one or both of these goals:

### Goal 1: Best Possible Detection Logic
- Pixel-accurate physics engine
- Zero false positives on legitimate players
- High confidence before flagging
- Precise setbacks (teleport to last safe position)

### Goal 2: Maximum Compatibility
- MC 1.7 through 26.2+
- Bukkit, Spigot, Paper, Folia, Purpur
- Java 11 through 26
- Java and Bedrock players (via Geyser/Floodgate)

**If a change conflicts with either goal, stop and discuss.**

---

## Community

- **Issues:** [GitHub Issues](https://github.com/enis1enis2/Windfall-AntiCheat/issues)
- **Releases:** [GitHub Releases](https://github.com/enis1enis2/Windfall-AntiCheat/releases)
- **Website:** [enis1enis2.github.io/Windfall-AntiCheat](https://enis1enis2.github.io/Windfall-AntiCheat/)

---

## License

By contributing to Windfall, you agree that your contributions will be licensed under the [MIT License](LICENSE).
