package io.windfall.anticheat.core.version;

import java.util.logging.Logger;

public enum ServerFork {

    FOLIA("Folia", "Regionized multithreading, EntityScheduler required"),
    PURPUR("Purpur", "Custom knockback, rideable entities"),
    PAPER("Paper", "Async chunk loading, EAR 2.0, Anti-Xray"),
    SPIGOT("Spigot", "Basic optimizations, standard scheduler"),
    BUKKIT("Bukkit", "Vanilla API, single-threaded");

    private final String displayName;
    private final String description;

    ServerFork(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public static ServerFork detect(Logger logger) {
        // Order matters: Folia extends Paper, Purpur extends Pufferfish extends Paper
        if (classExists("io.papermc.paper.threadedregions.RegionizedServer")) {
            if (logger != null) logger.info("[Windfall] Detected server fork: Folia");
            return FOLIA;
        }
        if (classExists("org.purpurmc.purpur.PurpurConfig")) {
            if (logger != null) logger.info("[Windfall] Detected server fork: Purpur");
            return PURPUR;
        }
        if (classExists("com.destroystokyo.paper.PaperConfig")) {
            if (logger != null) logger.info("[Windfall] Detected server fork: Paper");
            return PAPER;
        }
        if (classExists("org.spigotmc.SpigotConfig")) {
            if (logger != null) logger.info("[Windfall] Detected server fork: Spigot");
            return SPIGOT;
        }
        if (logger != null) logger.info("[Windfall] Detected server fork: Bukkit (vanilla)");
        return BUKKIT;
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isFolia() { return this == FOLIA; }
    public boolean isPurpur() { return this == PURPUR; }
    public boolean isPaperOrAbove() { return this == PAPER || this == FOLIA || this == PURPUR; }
    public boolean isSpigotOrAbove() { return this != BUKKIT; }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
