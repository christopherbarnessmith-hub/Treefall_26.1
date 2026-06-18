package com.kingxion.treefall;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class TreefallConfig {

    private static final Path CONFIG_PATH = Paths.get("config", "treefall.properties");

    public static void load() {
        // Write defaults if the file doesn't exist yet
        if (!Files.exists(CONFIG_PATH)) {
            writeDefaults();
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            props.load(in);
        } catch (IOException e) {
            Treefall.LOGGER.warn("Could not read treefall.properties, using defaults. ({})", e.getMessage());
            return;
        }

        Treefall.MAX_LOGS        = parseInt(props,  "max_logs",        Treefall.MAX_LOGS);
        Treefall.XP_PER_LOG      = parseInt(props,  "xp_per_log",      Treefall.XP_PER_LOG);
        Treefall.DURABILITY_COST = parseBool(props, "durability_cost",  Treefall.DURABILITY_COST);
        Treefall.SNEAK_BYPASS    = parseBool(props, "sneak_bypass",     Treefall.SNEAK_BYPASS);
        Treefall.ALLOW_BUILT_LOGS = parseBool(props, "allow_built_logs", Treefall.ALLOW_BUILT_LOGS);

        Treefall.LOGGER.info("Treefall config loaded: max_logs={}, xp_per_log={}, durability_cost={}, sneak_bypass={}, allow_built_logs={}",
                Treefall.MAX_LOGS, Treefall.XP_PER_LOG, Treefall.DURABILITY_COST, Treefall.SNEAK_BYPASS, Treefall.ALLOW_BUILT_LOGS);
    }

    private static void writeDefaults() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(CONFIG_PATH))) {
                writer.println("# Treefall Configuration");
                writer.println("# Changes take effect on next server/world load.");
                writer.println();
                writer.println("# Maximum number of logs a tree can have before felling is aborted.");
                writer.println("# Increase for large modded trees, decrease for better performance.");
                writer.println("max_logs=512");
                writer.println();
                writer.println("# XP awarded per log when a tree is felled. Set to 0 to disable.");
                writer.println("xp_per_log=1");
                writer.println();
                writer.println("# If true, the axe loses 1 durability per log broken during felling.");
                writer.println("# Unbreaking enchantment is still respected.");
                writer.println("durability_cost=true");
                writer.println();
                writer.println("# If true, sneaking while breaking a log skips felling and breaks");
                writer.println("# only that single block normally. Useful for trimming trees.");
                writer.println("sneak_bypass=true");
                writer.println();
                writer.println("# If true, connected player-built log structures can be felled too.");
                writer.println("# Defaults false so log houses, bridges, and decorations do not chain-break.");
                writer.println("allow_built_logs=false");
            }
        } catch (IOException e) {
            Treefall.LOGGER.warn("Could not write default treefall.properties: {}", e.getMessage());
        }
    }

    private static int parseInt(Properties props, String key, int fallback) {
        String val = props.getProperty(key);
        if (val == null) return fallback;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            Treefall.LOGGER.warn("Invalid value for '{}' in treefall.properties, using default {}", key, fallback);
            return fallback;
        }
    }

    private static boolean parseBool(Properties props, String key, boolean fallback) {
        String val = props.getProperty(key);
        if (val == null) return fallback;
        return Boolean.parseBoolean(val.trim());
    }
}
