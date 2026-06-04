package com.ewaygames;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class FrogPixelSkyblock_Config {
    private static final int CURRENT_CONFIG_VERSION = 2;

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("FrogPixelSkyblock.toml");

    // ====================================================
    // CONFIGURATION FIELDS (Live values accessed within)
    // ====================================================

    // [spawn]

    // The exact X coordinate where players will spawn or teleport back to the island.
    public static double spawn_x = 0.5;

    // The exact Y coordinate (height) where players will spawn or teleport back to the island.
    public static double spawn_y = 101.0;

    // The exact Z coordinate where players will spawn or teleport back to the island.
    public static double spawn_z = 0.5;

    // [features]

    // Enables the void loop mechanic (e.g., catching players falling into the void and teleporting them back to spawn).
    public static boolean enable_void_loop = true;

    // Enables verbose console logging whenever structure block swapping or data conversion occurs.
    public static boolean verboseStructureConversionLogging = false;

    // The distance (in blocks) around the drop point within which lost items can be recovered.
    public static int default_recovery_radius = 16;

    // [debugging]

    // Determines whether diagnostic visual beacon markers/pillars and layout debug text are generated in the world.
    public static boolean placeVisualMarkerAndLog = false;

    // Enables or disables full, unmodified loot table rewards within generated structures.
    public static boolean fullLootReward = false;

    // Forces lighting adjustments, such as generating torches or structural illumination, inside custom structures.
    public static boolean forceTorch = false;

    // [settings]

    // The trigger command prefix used for skyblock utilities (e.g., typing this string followed by 'spawn' triggers a teleport).
    public static String commandSuffix = "!skyblock";

    // Toggles whether players are permitted to run custom chat-based system utilities.
    public static boolean allow_commands = true;

    // Switches the mode to a higher difficulty where void falls are fatal and nearby dropped items cannot be recovered.
    public static boolean hard_difficulty = false;

    // Forces strict adherence to the server's globally configured difficulty parameters, preventing user overrides.
    public static boolean enforce_server_settings = false;

    // Forces players to be locked into or automatically sent to the sky block dimension environment.
    public static boolean force_sky_dimension = true;

    public static void loadConfig() {
        System.out.println("[FrogPixelSkyblock] Initializing Skyblock Configuration");

        try {
            // Check if the configuration file is missing completely
            if (Files.notExists(CONFIG_PATH)) {
                System.out.println("[FrogPixelSkyblock] Config file not found! Generating default file...");
                ensureParentDirectories();
                writeDefaultConfig();
            } else {
                // Pre-screen file layout version before parsing parameters
                int detectedVersion = readVersionHeaderOnly();
                if (detectedVersion < CURRENT_CONFIG_VERSION) {
                    System.out.println("[FrogPixelSkyblock] Outdated or missing config file format detected (Found: " + detectedVersion + ", Required: " + CURRENT_CONFIG_VERSION + ")");

                    Path backupPath = CONFIG_PATH.getParent().resolve("FrogPixelSkyblock.toml.old");
                    try {
                        Files.move(CONFIG_PATH, backupPath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("[FrogPixelSkyblock] Successfully backed up old settings to 'FrogPixelSkyblock.toml.old'");
                    } catch (IOException e) {
                        System.err.println("[FrogPixelSkyblock] Could not rename old config file: " + e.getMessage());
                    }

                    System.out.println("[FrogPixelSkyblock] Recreating fresh configuration layout...");
                    writeDefaultConfig();
                }
            }

            // Main parsing pass (Guaranteed clean asset space)
            List<String> lines = Files.readAllLines(CONFIG_PATH);
            String currentSection = "";
            boolean insideArray = false;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    currentSection = trimmed.toLowerCase();
                    insideArray = false;
                    continue;
                }

                if (trimmed.contains("=[")) {
                    insideArray = true;
                    continue;
                }

                if (trimmed.equals("]")) {
                    insideArray = false;
                    continue;
                }

                if (!insideArray && trimmed.contains("=")) {
                    String[] parts = trimmed.split("=", 2);
                    String key = parts[0].trim().toLowerCase();
                    String value = parts[1].trim();

                    if (value.contains("#")) {
                        value = value.split("#", 2)[0].trim();
                    }

                    switch (currentSection) {
                        case "[spawn]":
                            try {
                                if (key.equals("spawn_x")) spawn_x = Double.parseDouble(value);
                                if (key.equals("spawn_y")) spawn_y = Double.parseDouble(value);
                                if (key.equals("spawn_z")) spawn_z = Double.parseDouble(value);
                            } catch (NumberFormatException nfe) {
                                System.err.println("[FrogPixelSkyblock] Invalid double value in [spawn] for key: " + key);
                            }
                            break;

                        case "[features]":
                            if (key.equals("enable_void_loop")) {
                                enable_void_loop = Boolean.parseBoolean(value);
                            }
                            if (key.equals("default_recovery_radius") || key.equals("defaultrecoveryradius")) {
                                try {
                                    default_recovery_radius = Integer.parseInt(value);
                                } catch (NumberFormatException nfe) {
                                    System.err.println("[FrogPixelSkyblock] Invalid integer value in [features] for default_recovery_radius!");
                                }
                            }
                            break;

                        case "[debugging]":
                            if (key.equals("place_visual_marker_and_log") || key.equals("placevisualmarkerandlog")) {
                                placeVisualMarkerAndLog = Boolean.parseBoolean(value);
                            }
                            if (key.equals("full_loot_reward") || key.equals("fulllootreward")) {
                                fullLootReward = Boolean.parseBoolean(value);
                            }
                            if (key.equals("verbose_structure_logging") || key.equals("verbosestructureconversionlogging")) {
                                verboseStructureConversionLogging = Boolean.parseBoolean(value);
                            }
                            if (key.equals("force_torch") || key.equals("forcetorch")) {
                                forceTorch = Boolean.parseBoolean(value);
                            }
                            break;

                        case "[settings]":
                            if (key.equals("command_suffix") || key.equals("commandsuffix")) {
                                String cleanStr = value;
                                if (cleanStr.startsWith("\"") || cleanStr.startsWith("'")) cleanStr = cleanStr.substring(1);
                                if (cleanStr.endsWith("\"") || cleanStr.endsWith("'")) cleanStr = cleanStr.substring(0, cleanStr.length() - 1);
                                commandSuffix = cleanStr.trim();
                            }
                            if (key.equals("allow_commands") || key.equals("allowcommands")) {
                                allow_commands = Boolean.parseBoolean(value);
                            }
                            if (key.equals("hard_difficulty") || key.equals("harddifficulty")) {
                                hard_difficulty = Boolean.parseBoolean(value);
                            }
                            if (key.equals("enforce_server_settings") || key.equals("enforceserversettings")) {
                                enforce_server_settings = Boolean.parseBoolean(value);
                            }
                            if (key.equals("force_sky_dimension") || key.equals("forceskydimension")) {
                                force_sky_dimension = Boolean.parseBoolean(value);
                            }
                            break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[FrogPixelSkyblock] Error processing configuration framework!");
            e.printStackTrace();
        }
    }

    private static int readVersionHeaderOnly() {
        try {
            List<String> lines = Files.readAllLines(CONFIG_PATH);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#") || !trimmed.contains("=")) continue;

                String[] parts = trimmed.split("=", 2);
                String key = parts[0].trim().toLowerCase();
                String value = parts[1].trim();

                if (value.contains("#")) {
                    value = value.split("#", 2)[0].trim();
                }

                if (key.equals("config_version")) {
                    return Integer.parseInt(value);
                }
            }
        } catch (Exception e) {
            // Fall through if file is corrupt or missing
        }
        return 0;
    }

    private static void ensureParentDirectories() throws IOException {
        Path parentDir = CONFIG_PATH.getParent();
        if (parentDir != null && Files.notExists(parentDir)) {
            Files.createDirectories(parentDir);
        }
    }

    private static void writeDefaultConfig() throws IOException {
        List<String> defaults = List.of(
                "# ====================================================",
                "# FrogPixel Skyblock Configuration",
                "# ====================================================",
                "# WARNING: DO NOT CHANGE THE CONFIG_VERSION VALUE BELOW.",
                "# If the configuration structure modifications alter across updates,",
                "# the plugin will handle recreating your values automatically.",
                "# Also NO, Changing this number will NOT revert to an older config format",
                "config_version = " + CURRENT_CONFIG_VERSION,
                "# ------------------------------------------------------------------------",
                "",
                "",
                "[spawn]",
                "# The exact X world coordinate for the island spawn location.",
                "spawn_x = " + spawn_x,
                "",
                "# The exact Y world coordinate (height) for the island spawn location.",
                "spawn_y = " + spawn_y,
                "",
                "# The exact Z world coordinate for the island spawn location.",
                "spawn_z = " + spawn_z,
                "",
                "[features]",
                "# Catches players falling into the void and teleports them ",
                "# safely back to the configured spawn island coordinates.",
                "enable_void_loop = " + enable_void_loop,
                "",
                "# The boundary distance radius (in blocks) around a player's drop point",
                "# within which dropped items can be collected or interacted with by recovery utilities.",
                "default_recovery_radius = " + default_recovery_radius,
                "",
                "[debugging]",
                "# Spawns diagnostic directional tracking beacon lines for structures generated based on direction.",
                "# Turning this on will generate blocks in world and will print the position data to the console.",
                "place_visual_marker_and_log = " + placeVisualMarkerAndLog,
                "",
                "# Toggles full crate loot table rewards and custom item drops",
                "# instead of half filled crates within newly generated world structures.",
                "full_loot_reward = " + fullLootReward,
                "",
                "# Outputs detailed troubleshooting messages to the server console",
                "# whenever structure assets trigger block data conversions,",
                "# such as turning vaults into crates or trial spawners into vanilla ones.",
                "verbose_structure_logging = " + verboseStructureConversionLogging,
                "",
                "# Forces the placement of torches on top of the vanilla spawners",
                "# inside the trial dungeon to make it a reward rather than a challenge.",
                "force_torch = " + forceTorch,
                "",
                "[settings]",
                "# The primary chat command text prefix utilized for system commands",
                "# (e.g., typing this string followed by 'spawn' will trigger teleports).",
                "command_suffix = \"" + commandSuffix + "\"",
                "",
                "# Toggles whether players are allowed to use system chat shortcuts completely.",
                "allow_commands = " + allow_commands,
                "",
                "# Enables advanced difficulty behavior. When true, falling into the void",
                "# is fatal (normal damage rules apply) and items dropped inside the void",
                "# recovery radius cannot be restored using recovery command shortcuts.",
                "hard_difficulty = " + hard_difficulty,
                "",
                "# Forces strict server settings. If enabled, players trying to manually",
                "# change local configuration shortcuts or difficulty parameters via commands",
                "# will be blocked and notified that the server is enforcing these properties.",
                "enforce_server_settings = " + enforce_server_settings,
                "",
                "# Determines whether players are forced into the custom sky dimension",
                "# environment upon joining or shifting game modes.",
                "force_sky_dimension = " + force_sky_dimension
        );

        Files.write(CONFIG_PATH, defaults);
    }
}