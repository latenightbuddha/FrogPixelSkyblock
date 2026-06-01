package com.ewaygames;

import net.minecraft.core.BlockPos;
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

    public static void spawnStructureNearSpawn(String file, ServerLevel level, BlockPos spawnCenter, Rotation rotation, int distanceOut) {
        File worldDir = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        File targetDest = new File(worldDir, "generated/minecraft/structures/" + file + ".nbt");

        // 1. Extract the file if it doesn't exist
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

        // 2. LOAD THE STRUCTURE DIRECTLY VIA RAW NBT
        Optional<StructureTemplate> templateOpt = Optional.empty();
        try {
            if (targetDest.exists()) {
                // Read the uncompressed NBT data directly from disk using NbtIo
                net.minecraft.nbt.CompoundTag nbtCompound = net.minecraft.nbt.NbtIo.readCompressed(
                        targetDest.toPath(),
                        net.minecraft.nbt.NbtAccounter.unlimitedHeap()
                );

                // Create a blank structure template and populate it with the parsed NBT data
                StructureTemplate template = new StructureTemplate();
                template.load(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK), nbtCompound);

                templateOpt = Optional.of(template);
            }
        } catch (Exception e) {
            System.err.println("[FrogPixelSkyblock] Failed to parse raw structure NBT data: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback to standard check if direct read failed
        if (templateOpt.isEmpty()) {
            StructureTemplateManager templateManager = level.getStructureManager();
            Identifier structureLocation = Identifier.fromNamespaceAndPath("minecraft", file);
            templateOpt = templateManager.get(structureLocation);
        }

        // inside public static void spawnStructureNearSpawn(...)
        if (templateOpt.isPresent()) {
            StructureTemplate template = templateOpt.get();

            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .setMirror(Mirror.NONE)
                    .setRotation(rotation)
                    .setIgnoreEntities(false);

            settings.addProcessor(getMansionProcessor());

            // Original template boundaries
            int sizeX = template.getSize().getX();
            int sizeZ = template.getSize().getZ();

            int finalX = spawnCenter.getX();
            int finalY = spawnCenter.getY() - 5; // Retains your custom height offset
            int finalZ = spawnCenter.getZ();

            // Perfect Cardinal Grid Alignment Math
            if (rotation == Rotation.NONE) { // NORTH side (Faces South, expands +X, +Z)
                finalX -= (sizeX / 2);
                finalZ -= (distanceOut + sizeZ);
            }
            else if (rotation == Rotation.CLOCKWISE_180) { // SOUTH side (Faces North, expands -X, -Z in world space due to 180 flip)
                finalX += (sizeX / 2);
                finalZ += distanceOut;
            }
            else if (rotation == Rotation.COUNTERCLOCKWISE_90) { // EAST side (Faces West, expands -Z, +X after rotation)
                finalX += distanceOut;
                finalZ += (sizeX / 2);
            }
            else if (rotation == Rotation.CLOCKWISE_90) { // WEST side (Faces East, expands +Z, -X after rotation)
                finalX -= (distanceOut + sizeZ);
                finalZ -= (sizeX / 2);
            }

            BlockPos targetPlacementPos = new BlockPos(finalX, finalY, finalZ);
            template.placeInWorld(level, targetPlacementPos, targetPlacementPos, settings, level.getRandom(), 3);

            System.out.println("[FrogPixelSkyblock] Materialized a " + file + " facing " + rotation.name() + " at: " + targetPlacementPos);
        }
    }

    private static StructureProcessor getMansionProcessor() {
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