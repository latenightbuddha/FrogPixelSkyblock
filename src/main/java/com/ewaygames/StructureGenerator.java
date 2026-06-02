package com.ewaygames;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class StructureGenerator {

    public static void spawnStructureNearSpawn(String file, ServerLevel level, BlockPos spawnCenter, Rotation rotation, Mirror mirror, int distanceOut, boolean setIgnoreEntities, boolean placeVisualMarkerAndLog) {
        File worldDir = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        File targetDest = new File(worldDir, "generated/minecraft/structures/" + file + ".nbt");

        // 1. Extract the asset from the jar files if missing on the world disk space
        try {
            if (!targetDest.exists()) {
                targetDest.getParentFile().mkdirs();
                try (InputStream in = StructureGenerator.class.getResourceAsStream("/data/minecraft/structures/" + file + ".nbt")) {
                    if (in != null) {
                        Files.copy(in, targetDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("[FrogPixelSkyblock] Successfully extracted " + file + ".nbt to world disk space.");
                    } else {
                        System.err.println("[FrogPixelSkyblock] Failed to read resource stream from jar path!");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[FrogPixelSkyblock] Failed to write fallback asset mapping to world files: " + e.getMessage());
        }

        // 2. Load the file template data from disk memory
        Optional<StructureTemplate> templateOpt = Optional.empty();
        try {
            if (targetDest.exists()) {
                net.minecraft.nbt.CompoundTag nbtCompound = net.minecraft.nbt.NbtIo.readCompressed(
                        targetDest.toPath(),
                        net.minecraft.nbt.NbtAccounter.unlimitedHeap()
                );

                StructureTemplate template = new StructureTemplate();
                template.load(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK), nbtCompound);
                templateOpt = Optional.of(template);
            }
        } catch (Exception e) {
            System.err.println("[FrogPixelSkyblock] Failed to parse raw structure NBT data: " + e.getMessage());
            e.printStackTrace();
        }

        if (templateOpt.isEmpty()) {
            StructureTemplateManager templateManager = level.getStructureManager();
            Identifier structureLocation = Identifier.fromNamespaceAndPath("minecraft", file);
            templateOpt = templateManager.get(structureLocation);
        }

        if (templateOpt.isPresent()) {
            StructureTemplate template = templateOpt.get();
            Vec3i templateSize = template.getSize();

            // Dynamically evaluate coordinates relative to the mixin's provided spawnCenter argument
            int centralPlatformX = spawnCenter.getX(); // 0
            int centralPlatformZ = spawnCenter.getZ(); // 0
            int finalY = spawnCenter.getY() - 5;

            int sizeX = templateSize.getX(); // 63
            int sizeZ = templateSize.getZ(); // 61

            int finalX = 0;
            int finalZ = 0;

            // 3. Pure Mathematical Grid Matrix Layout Calibration
            // Bypasses intermediate engine translation bugs by offsetting the bounding hinges manually.
            switch (rotation) {
                case NONE: // NORTH (Red Beacon)
                    // Expands East (+X) and South (+Z). Anchor goes North (-Z).
                    finalX = centralPlatformX - (sizeX / 2);
                    finalZ = centralPlatformZ - distanceOut - sizeZ;
                    break;

                case CLOCKWISE_90: // WEST (Yellow Beacon)
                    // Expands West (-X) and South (+Z). Anchor goes West (-X).
                    finalX = centralPlatformX - distanceOut - sizeX;
                    finalZ = centralPlatformZ - (sizeZ / 2);
                    break;

                case CLOCKWISE_180: // SOUTH (Blue Beacon)
                    // Expands West (-X) and North (-Z).
                    // Add back size dimensions to shift its backward structural expansion safely away from spawn!
                    finalX = centralPlatformX - (sizeX / 2) + sizeX;
                    finalZ = centralPlatformZ + distanceOut + sizeZ;
                    break;

                case COUNTERCLOCKWISE_90: // EAST (Green Beacon)
                    // Expands East (+X) and North (-Z). Dimensions swap values on 90 degree flips.
                    // Add back size dimensions to compensate for backward bounding box growth!
                    finalX = centralPlatformX + distanceOut + sizeZ;
                    finalZ = centralPlatformZ - (sizeX / 2) + sizeX;
                    break;
            }

            BlockPos targetPlacementPos = new BlockPos(finalX, finalY, finalZ);

            // Establish the placement settings while explicitly resetting pivot hinge drift logic
            StructurePlaceSettings placementData = new StructurePlaceSettings()
                    .setRotation(rotation)
                    .setMirror(mirror)
                    .setIgnoreEntities(setIgnoreEntities)
                    .setRotationPivot(BlockPos.ZERO);

            if(placeVisualMarkerAndLog) {

                // --- DEBUG LOGGING ---
                System.out.println("========================================");
                System.out.println("[STRUCTURE DEBUG] Direction: " + rotation.name());
                System.out.println("[STRUCTURE DEBUG] Mansion Size: X=" + sizeX + ", Z=" + sizeZ);
                System.out.println("[STRUCTURE DEBUG] CALCULATED PLACEMENT CORNER: " + targetPlacementPos);
                System.out.println("========================================");

                // Spawns diagnostic directional tracking beacon lines
                placeVisualMarker(level, targetPlacementPos, rotation);
            }

            // 4. Run direct engine block placement
            template.placeInWorld(level, targetPlacementPos, targetPlacementPos, placementData, level.getRandom(), 3);

            System.out.println("[FrogPixelSkyblock] Materialized a " + file + " facing " + rotation.name() + " at: " + targetPlacementPos);
        }
    }

    /**
     * Spawns a diagnostic beacon platform and a floating colored beam at the structure's origin corner.
     */
    private static void placeVisualMarker(ServerLevel level, BlockPos pos, Rotation rotation) {
        BlockState glassColor;
        BlockState solidMarker;

        switch (rotation) {
            case NONE:
                glassColor = Blocks.RED_STAINED_GLASS.defaultBlockState();
                solidMarker = Blocks.RED_WOOL.defaultBlockState();
                break;
            case CLOCKWISE_180:
                glassColor = Blocks.BLUE_STAINED_GLASS.defaultBlockState();
                solidMarker = Blocks.BLUE_WOOL.defaultBlockState();
                break;
            case COUNTERCLOCKWISE_90:
                glassColor = Blocks.GREEN_STAINED_GLASS.defaultBlockState();
                solidMarker = Blocks.GREEN_WOOL.defaultBlockState();
                break;
            case CLOCKWISE_90:
                glassColor = Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
                solidMarker = Blocks.YELLOW_WOOL.defaultBlockState();
                break;
            default:
                glassColor = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
                solidMarker = Blocks.IRON_BLOCK.defaultBlockState();
        }

        // Build beacon pad structure at y=50 through the void space
        BlockPos basePos = new BlockPos(pos.getX(), 50, pos.getZ());
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                level.setBlockAndUpdate(basePos.offset(xOffset, 0, zOffset), Blocks.IRON_BLOCK.defaultBlockState());
            }
        }
        level.setBlockAndUpdate(basePos.above(), Blocks.BEACON.defaultBlockState());
        level.setBlockAndUpdate(basePos.above(2), glassColor);

        // Place a structural verification pillar spanning y=60 to y=90
        for (int y = 60; y <= 90; y++) {
            BlockPos linePos = new BlockPos(pos.getX(), y, pos.getZ());
            level.setBlockAndUpdate(linePos, (y % 2 == 0) ? solidMarker : Blocks.SEA_LANTERN.defaultBlockState());
        }
    }

    private static StructureProcessor getStructureProcessor() {
        return new StructureProcessor() {
            @Override
            public StructureTemplate.StructureBlockInfo processBlock(
                    LevelReader levelReader,
                    BlockPos templatePos,
                    BlockPos worldPos,
                    StructureTemplate.StructureBlockInfo clientInfo,
                    StructureTemplate.StructureBlockInfo serverInfo,
                    StructurePlaceSettings settings
            ) {
                BlockState state = serverInfo.state();
                if (state.is(Blocks.LAVA) || state.is(Blocks.SAND) || state.is(Blocks.WATER) || state.is(Blocks.GRAVEL)) {
                    return new StructureTemplate.StructureBlockInfo(serverInfo.pos(), Blocks.AIR.defaultBlockState(), serverInfo.nbt());
                }
                return serverInfo;
            }

            @Override
            protected StructureProcessorType<?> getType() {
                return StructureProcessorType.BLOCK_IGNORE;
            }
        };
    }
}