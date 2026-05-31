package com.ewaygames;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FrogPixelSkyblock_Config {

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("FrogPixelSkyblock.toml");

    public static void loadConfig() {
        System.out.println("[FrogPixelSkyblock] Initializing Skyblock Configuration");

        try {
            // 1. Check if the configuration file is missing
            if (Files.notExists(CONFIG_PATH)) {
                System.out.println("[FrogPixelSkyblock] Config file not found! Generating default file...");

                // Ensure the parent directory structure exists (e.g., if the 'config' folder itself was wiped)
                Path parentDir = CONFIG_PATH.getParent();
                if (parentDir != null && Files.notExists(parentDir)) {
                    Files.createDirectories(parentDir);
                }

                // Write the default settings down to disk
                writeDefaultConfig();
            }

            // 2. Guaranteed safe to read now without throwing a NoSuchFileException
            List<String> lines = Files.readAllLines(CONFIG_PATH);

            String currentSection = "";
            boolean insideArray = false;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                // Track which TOML section header we are currently analyzing
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    currentSection = trimmed.toLowerCase();
                    insideArray = false; // Reset array flag on new section
                    continue;
                }

                // Detect when entering an array assignment block
                if (trimmed.contains("=[")) {
                    insideArray = true;
                    continue;
                }

                // Detect when exiting an array block
                if (trimmed.equals("]")) {
                    insideArray = false;
                    continue;
                }

                if (insideArray) {
                    String cleanValue = trimmed.trim();
                    if (cleanValue.startsWith("\"") || cleanValue.startsWith("'")) {
                        cleanValue = cleanValue.substring(1);
                    }
                    if (cleanValue.endsWith(",") || cleanValue.endsWith("]")) {
                        cleanValue = cleanValue.substring(0, cleanValue.length() - 1).trim();
                    }
                    if (cleanValue.endsWith("\"") || cleanValue.endsWith("'")) {
                        cleanValue = cleanValue.substring(0, cleanValue.length() - 1);
                    }

                    cleanValue = cleanValue.trim().toLowerCase();

                    if (cleanValue.isEmpty() || cleanValue.equals("]")) continue;
                }
            }
        } catch (IOException e) {
            System.err.println("[FrogPixelSkyblock] Error loading your TOML config!");
            e.printStackTrace();
        }
    }

    private static void writeDefaultConfig() throws IOException {
        List<String> defaults = List.of(
                "# FrogPixel Skyblock Configuration",
                "",
                "[spawn]",
                "spawn_x = 0.5",
                "spawn_y = 101.0",
                "spawn_z = 0.5",
                "",
                "[features]",
                "enable_void_loop = true"
        );
        Files.write(CONFIG_PATH, defaults);
    }
}