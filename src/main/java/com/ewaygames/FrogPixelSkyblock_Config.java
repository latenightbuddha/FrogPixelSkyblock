package com.ewaygames;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FrogPixelSkyblock_Config {

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("FrogPixelSkyblock.toml");

    public static void loadConfig() {

        try {
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
                "# FrogPixel Skyblock Configuration"
        );
        Files.write(CONFIG_PATH, defaults);
    }

}