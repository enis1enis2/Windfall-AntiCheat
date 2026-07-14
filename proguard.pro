# ============================================================
# Windfall Anti-Cheat — ProGuard Configuration
# ============================================================
# Strategy:
#   - API/entry point/reflection classes: fully preserved (-keep)
#   - Check class names: preserved for logging (-keep)
#   - All other classes: names + members fully obfuscated
#   - Package structure flattened via -repackageclasses ''
#   - mapping.txt provides deobfuscation for stack traces
# ============================================================

# --- Core settings ---
-dontshrink
-dontoptimize
-dontwarn
-dontusemixedcaseclassnames

# --- Obfuscation settings ---
-overloadaggressively
-repackageclasses ''
-allowaccessmodification

# --- Keep attributes (critical for Bukkit/PacketEvents) ---
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,Synthetic,EnclosingMethod,RuntimeVisibleAnnotations

# ============================================================
# KEEP CLASS NAMES — only where absolutely required
# ============================================================

# --- API entry point (plugin.yml) ---
-keep public class io.windfall.anticheat.WindfallPlugin {
    public *;
    protected *;
}

# --- Bukkit Listener inner class ---
-keep class io.windfall.anticheat.WindfallPlugin$PlayerQuitListener {
    *;
}

# --- PacketEvents listener ---
-keep class io.windfall.anticheat.core.network.PacketListener {
    *;
}

# --- ChecklistGUI (Bukkit Listener + Adventure reflection) ---
-keep class io.windfall.anticheat.core.command.ChecklistGUI {
    *;
}

# --- @CheckData annotation (RUNTIME retention) ---
-keep @io.windfall.anticheat.core.check.CheckData class * {
    *;
}
-keep class io.windfall.anticheat.core.check.CheckData {
    *;
}

# --- Check framework (called via reflection) ---
-keep class io.windfall.anticheat.core.check.Check {
    *;
}
-keep class io.windfall.anticheat.core.check.CompatFlag {
    *;
}
-keep class io.windfall.anticheat.core.check.type.PacketCheck {
    *;
}
-keep class io.windfall.anticheat.core.check.CheckManager {
    *;
}

# --- Check implementations: inner classes preserved for method params ---
-keep class io.windfall.anticheat.core.check.impl.**$* {
    *;
}

# --- PredictionContext (used as method param in kept checks) ---
-keep class io.windfall.anticheat.core.physics.PredictionContext { *; }

# --- Public API (external plugins depend on these) ---
-keep class io.windfall.anticheat.api.** {
    *;
}

# --- Heavy reflection classes (Class.forName, getMethod, invoke) ---
-keep class io.windfall.anticheat.core.compat.WorldGuardCompat { *; }
-keep class io.windfall.anticheat.core.platform.FoliaCompat { *; }
-keep class io.windfall.anticheat.core.platform.PurpurCompat { *; }
-keep class io.windfall.anticheat.core.plugin.PluginDetector { *; }
-keep class io.windfall.anticheat.core.version.ServerFork { *; }
-keep class io.windfall.anticheat.core.version.VersionManager { *; }
-keep class io.windfall.anticheat.core.scheduler.PlatformScheduler { *; }
-keep class io.windfall.anticheat.core.bedrock.GeyserManager { *; }
-keep class io.windfall.anticheat.core.bedrock.GeysersTracker { *; }
-keep class io.windfall.anticheat.core.bedrock.BedrockInfo { *; }
-keep class io.windfall.anticheat.core.command.CommandManager { *; }
-keep class io.windfall.anticheat.core.command.CommandManager$WindfallCommand { *; }
-keep class io.windfall.anticheat.core.player.WindfallPlayer { *; }
-keep class io.windfall.anticheat.core.check.impl.movement.ElytraCheck { *; }

# Inner classes of reflection-heavy classes
-keep class io.windfall.anticheat.core.bedrock.**$* { *; }
-keep class io.windfall.anticheat.core.command.**$* { *; }
-keep class io.windfall.anticheat.core.player.**$* { *; }
-keep class io.windfall.anticheat.core.scheduler.**$* { *; }

# ============================================================
# ENUMS AND SERIALIZABLE
# ============================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
}
