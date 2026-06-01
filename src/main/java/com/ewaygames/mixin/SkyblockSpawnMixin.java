package com.ewaygames.mixin;

import com.ewaygames.FrogPixelSkyblock;
import com.ewaygames.StructureGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.level.ServerLevel")
public class SkyblockSpawnMixin {

    @Dynamic("Suppresses the un-mapped Mojang ServerPlayer method configuration.")
    @Inject(method = "addPlayer(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("TAIL"))
    private void onPlayerEnteredServerWorld(ServerPlayer serverPlayerEntity, CallbackInfo ci) {
        try {
            ServerLevel worldInstance = (ServerLevel) (Object) this;
            String worldId = worldInstance.dimension().identifier().toString();

            if (worldId.equals(FrogPixelSkyblock.SKY_DIM)) {

                worldInstance.getServer().execute(() -> {
                    try {
                        int targetX = 0;
                        int targetY = 64;
                        int targetZ = 0;

                        // Anchor tracking at vanilla minimum floor (Y = -64)
                        BlockPos bedrockAnchorPos = new BlockPos(targetX, -64, targetZ);

                        // Checks if the tracking anchor is registered yet
                        if (!worldInstance.getBlockState(bedrockAnchorPos).is(Blocks.BEDROCK)) {
                            System.out.println("[FrogPixelSkyblock] Fresh skyblock world detected. Generating 32x32 solid stone monolith foundation...");

                            BlockState grassState = Blocks.GRASS_BLOCK.defaultBlockState();
                            BlockState dirtState = Blocks.DIRT.defaultBlockState();
                            BlockState stoneState = Blocks.STONE.defaultBlockState();
                            BlockState bedrockState = Blocks.BEDROCK.defaultBlockState();
                            BlockState torchState = Blocks.TORCH.defaultBlockState();
                            BlockState chestState = Blocks.CHEST.defaultBlockState();

                            BlockState[] saplingVarieties = new BlockState[] {
                                    Blocks.OAK_SAPLING.defaultBlockState(),
                                    Blocks.SPRUCE_SAPLING.defaultBlockState(),
                                    Blocks.BIRCH_SAPLING.defaultBlockState(),
                                    Blocks.JUNGLE_SAPLING.defaultBlockState(),
                                    Blocks.ACACIA_SAPLING.defaultBlockState(),
                                    Blocks.DARK_OAK_SAPLING.defaultBlockState(),
                                    Blocks.CHERRY_SAPLING.defaultBlockState(),
                                    Blocks.MANGROVE_PROPAGULE.defaultBlockState()
                            };

                            worldInstance.setChunkForced(0, 0, true);

                            // 1. Place the baseline bedrock tracking marker at Y = -64
                            worldInstance.setBlockAndUpdate(bedrockAnchorPos, bedrockState);

                            // 2. Place the anchor torch right on top of it at Y = -63
                            worldInstance.setBlockAndUpdate(bedrockAnchorPos.above(), torchState);

                            // 3. Generate the massive 32x32 platform blocks
                            int saplingCounter = 0;

                            for (int x = -16; x < 16; x++) {
                                for (int z = -16; z < 16; z++) {
                                    int currentX = targetX + x;
                                    int currentZ = targetZ + z;

                                    // FIXED: Every single coordinate across the 32x32 footprint now fills vertically with Stone from Y=-62 to Y=60
                                    for (int stoneY = -62; stoneY <= 60; stoneY++) {
                                        worldInstance.setBlockAndUpdate(new BlockPos(currentX, stoneY, currentZ), stoneState);
                                    }

                                    // Platform structural dirt layers directly below the surface (Y=61, Y=62, Y=63)
                                    worldInstance.setBlockAndUpdate(new BlockPos(currentX, targetY - 3, currentZ), dirtState);
                                    worldInstance.setBlockAndUpdate(new BlockPos(currentX, targetY - 2, currentZ), dirtState);
                                    worldInstance.setBlockAndUpdate(new BlockPos(currentX, targetY - 1, currentZ), dirtState);

                                    // Perfect clean grass surface layer across the platform
                                    BlockPos grassPos = new BlockPos(currentX, targetY, currentZ);
                                    worldInstance.setBlockAndUpdate(grassPos, grassState);

                                    // Orchard sapling layout matrix
                                    if (x % 6 == 0 && z % 6 == 0) {
                                        if (x > -14 && x < 14 && z > -14 && z < 14) {

                                            // Skip placing a tree node directly on the exact spawn point (0, 0)
                                            if (x == 0 && z == 0) {
                                                continue;
                                            }

                                            BlockState selectedSapling = saplingVarieties[saplingCounter % saplingVarieties.length];
                                            worldInstance.setBlockAndUpdate(new BlockPos(currentX, targetY + 1, currentZ), selectedSapling);
                                            saplingCounter++;
                                        }
                                    }

                                    if ((x + 3) % 6 == 0 && (z + 3) % 6 == 0) {
                                        if (x > -15 && x < 15 && z > -15 && z < 15) {
                                            worldInstance.setBlockAndUpdate(new BlockPos(currentX, targetY + 1, currentZ), torchState);
                                        }
                                    }
                                }
                            }

                            // ====================================================
                            // 4-PLAYER STARTER CHEST GENERATION
                            // ====================================================
                            BlockPos chestPos = new BlockPos(targetX + 1, targetY + 1, targetZ);
                            worldInstance.setBlockAndUpdate(chestPos, chestState);

                            BlockEntity blockEntity = worldInstance.getBlockEntity(chestPos);
                            if (blockEntity instanceof ChestBlockEntity chestEntity) {
                                chestEntity.setItem(0, new ItemStack(Items.LAVA_BUCKET, 1));
                                chestEntity.setItem(1, new ItemStack(Items.ICE, 4));
                                chestEntity.setItem(2, new ItemStack(Items.WATER_BUCKET, 1));

                                chestEntity.setItem(9, new ItemStack(Items.BREAD, 32));

                                chestEntity.setItem(11, new ItemStack(Items.MELON_SEEDS, 4));
                                chestEntity.setItem(12, new ItemStack(Items.PUMPKIN_SEEDS, 4));
                                chestEntity.setItem(13, new ItemStack(Items.SUGAR_CANE, 4));
                                chestEntity.setItem(14, new ItemStack(Items.BONE, 12));

                                chestEntity.setItem(18, new ItemStack(Items.IRON_INGOT, 12));
                                chestEntity.setItem(19, new ItemStack(Items.STRING, 16));
                            }

                            try {
                                net.minecraft.core.BlockPos spawnCenter = new net.minecraft.core.BlockPos(0, 70, 0);

                                // Estimate or dynamically get the width/length of your structure bounding box
                                int sizeX = 60;
                                int sizeZ = 60;
                                int distanceOut = 80; // How many blocks of empty void you want between spawn and the mansion wall

                                // NORTH: Faces South naturally. Pivot point stays top-left.
                                net.minecraft.core.BlockPos northTarget = new net.minecraft.core.BlockPos(
                                        spawnCenter.getX() - (sizeX / 2),
                                        spawnCenter.getY() - 5,
                                        spawnCenter.getZ() - distanceOut - sizeZ
                                );

                                // SOUTH: Rotated 180°. Flips the box completely.
                                net.minecraft.core.BlockPos southTarget = new net.minecraft.core.BlockPos(
                                        spawnCenter.getX() + (sizeX / 2),
                                        spawnCenter.getY() - 5,
                                        spawnCenter.getZ() + distanceOut + sizeZ
                                );

                                // EAST: Rotated 270° (Counter-clockwise 90). Sweeps the width into the Z axis.
                                net.minecraft.core.BlockPos eastTarget  = new net.minecraft.core.BlockPos(
                                        spawnCenter.getX() + distanceOut + sizeZ,
                                        spawnCenter.getY() - 5,
                                        spawnCenter.getZ() - (sizeX / 2)
                                );

                                // WEST: Rotated 90° (Clockwise 90).
                                net.minecraft.core.BlockPos westTarget  = new net.minecraft.core.BlockPos(
                                        spawnCenter.getX() - distanceOut - sizeZ,
                                        spawnCenter.getY() - 5,
                                        spawnCenter.getZ() + (sizeX / 2)
                                );

                                //TODO Fix the generation for these prefabs
                                StructureGenerator.spawnStructureNearSpawn("woodland_mansion", worldInstance, northTarget, Rotation.CLOCKWISE_90);
                                // StructureGenerator.spawnStructureNearSpawn("todo marine_temple", worldInstance, southTarget, Rotation.CLOCKWISE_180);
                                // StructureGenerator.spawnStructureNearSpawn("todo sky_aircraft", worldInstance, eastTarget,  Rotation.COUNTERCLOCKWISE_90);
                                // StructureGenerator.spawnStructureNearSpawn("todo unknown_prefab", worldInstance, westTarget,  Rotation.NONE);

                            } catch (Exception e) {
                                System.err.println("[FrogPixelSkyblock] Failed to generate the structure: " + e.getMessage());
                            }
                        }

                        // Teleport the player safely onto the platform
                        if (serverPlayerEntity.getY() < targetY + 1) {
                            serverPlayerEntity.fallDistance = 0.0F;
                            serverPlayerEntity.setDeltaMovement(Vec3.ZERO);

                            Vec3 safePosition = new Vec3(0.5, targetY + 1.0, 0.5);
                            Vec3 zeroMovement = Vec3.ZERO;

                            TeleportTransition transition = new TeleportTransition(
                                    worldInstance,
                                    safePosition,
                                    zeroMovement,
                                    serverPlayerEntity.getYRot(),
                                    serverPlayerEntity.getXRot(),
                                    java.util.Set.of(),
                                    TeleportTransition.DO_NOTHING
                            );

                            serverPlayerEntity.teleport(transition);
                            serverPlayerEntity.connection.resetPosition();
                        }
                    } catch (Exception innerEx) {
                        innerEx.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[FrogPixelsSkyblock] Skyblock platform generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}