package com.ewaygames;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.ewaygames.FrogPixelSkyblock_Config.*;

public class SkyblockCleanUpQueue {

    private static final Random RANDOM = new Random();

    // 1. Array of standard breakdown mobs for the replaced Vanilla Spawners
    private static final EntityType<?>[] HOSTILE_MOBS = {
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CAVE_SPIDER,
            EntityType.WITHER_SKELETON, EntityType.BOGGED, EntityType.WITCH, EntityType.VINDICATOR,
            EntityType.CREEPER, EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.HOGLIN,
            EntityType.HUSK, EntityType.STRAY, EntityType.PILLAGER, EntityType.EVOKER,
            EntityType.RAVAGER, EntityType.BLAZE
    };

    // 2. Custom scratch array mimicking classic mossy cobble dungeon loot chests
    private static final Item[] DUNGEON_LOOT_POOL = {
            Items.SADDLE, Items.NAME_TAG, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.IRON_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR, Items.DIAMOND_HORSE_ARMOR,
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.GUNPOWDER, Items.STRING,
            Items.ROTTEN_FLESH, Items.BREAD, Items.WHEAT, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS,
            Items.COAL, Items.REDSTONE, Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION,

            Items.TOTEM_OF_UNDYING, Items.ELYTRA, Items.TRIDENT, Items.RECOVERY_COMPASS,
            Items.DISC_FRAGMENT_5, Items.DRAGON_BREATH, Items.DRAGON_HEAD, Items.DRAGON_EGG,

            Items.BREEZE_ROD, Items.SHULKER_SHELL, Items.BUNDLE, Items.SPYGLASS
    };

    private static final Item[] DUNGEON_DISC_POOL = {
            Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS, Items.MUSIC_DISC_CHIRP,
            Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL, Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL,
            Items.MUSIC_DISC_STRAD, Items.MUSIC_DISC_WARD, Items.MUSIC_DISC_11, Items.MUSIC_DISC_WAIT,
            Items.MUSIC_DISC_OTHERSIDE, Items.MUSIC_DISC_PIGSTEP, Items.MUSIC_DISC_RELIC,
            Items.MUSIC_DISC_PRECIPICE, Items.MUSIC_DISC_CREATOR, Items.MUSIC_DISC_CREATOR_MUSIC_BOX
    };

    // 3. Registry collection of strictly beneficial potion effects for skyblock utility
    private static final List<Holder<Potion>> HELPFUL_POTIONS = List.of(
            Potions.HEALING,
            Potions.STRONG_HEALING,
            Potions.REGENERATION,
            Potions.LONG_REGENERATION,
            Potions.SWIFTNESS,
            Potions.LONG_SWIFTNESS,
            Potions.STRENGTH,
            Potions.LONG_STRENGTH,
            Potions.FIRE_RESISTANCE,
            Potions.LONG_FIRE_RESISTANCE,
            Potions.NIGHT_VISION
    );

    public record ModificationTask(ResourceKey<Level> dimension, BlockPos pos, boolean isTrialSpawner) {
    }

    private static final Queue<ModificationTask> TASK_QUEUE = new ConcurrentLinkedQueue<>();

    public static void enqueue(ResourceKey<Level> dimension, BlockPos pos, boolean isTrialSpawner) {
        TASK_QUEUE.add(new ModificationTask(dimension, pos, isTrialSpawner));
    }

    private static final List<Item> DISC_LOOKUP = Arrays.asList(DUNGEON_DISC_POOL);

    @SuppressWarnings("deprecation")
    public static void processQueue(ServerLevel level) {
        if (TASK_QUEUE.isEmpty()) return;

        int size = TASK_QUEUE.size();
        for (int i = 0; i < size; i++) {
            ModificationTask task = TASK_QUEUE.poll();
            if (task == null) break;

            if (!task.dimension().equals(level.dimension())) {
                TASK_QUEUE.add(task);
                continue;
            }

            BlockPos targetPos = task.pos();

            if (level.hasChunkAt(targetPos)) {
                if (!task.isTrialSpawner()) {

                    // CLEAR THE OLD BLOCK ENTITY (VAULT) CACHE FIRST
                    level.removeBlockEntity(targetPos);

                    // REPLACE VAULT WITH BARREL
                    level.setBlock(targetPos, Blocks.BARREL.defaultBlockState(), 3);
                    BlockEntity barrelTile = level.getBlockEntity(targetPos);

                    // Load from config, do we want full loot in the trial barrels or normal chance?
                    if (fullLootReward) {
                        if (barrelTile instanceof BarrelBlockEntity barrel) {
                            // Determine a random number of items to put in this specific barrel (e.g., 3 to 7 slots populated)
                            int itemsToPlace = 3 + RANDOM.nextInt(5);

                            boolean hasDiscAlready = false;

                            for (int o = 0; o < barrel.getContainerSize(); o++) {
                                if (DISC_LOOKUP.contains(barrel.getItem(o).getItem())) {
                                    hasDiscAlready = true;
                                    break;
                                }
                            }

                            for (int count = 0; count < itemsToPlace; count++) {
                                Item randomItem;

                                if (!hasDiscAlready && RANDOM.nextInt(4) == 0) {
                                    randomItem = DUNGEON_DISC_POOL[RANDOM.nextInt(DUNGEON_DISC_POOL.length)];
                                } else {
                                    randomItem = DUNGEON_LOOT_POOL[RANDOM.nextInt(DUNGEON_LOOT_POOL.length)];
                                }

                                // Scale individual stack counts contextually (e.g., resources stack, gear doesn't)
                                int stackSize = 1;
                                if (randomItem == Items.IRON_INGOT || randomItem == Items.GOLD_INGOT || randomItem == Items.COAL) {
                                    stackSize = 1 + RANDOM.nextInt(4); // 1-4 items
                                } else if (randomItem == Items.GUNPOWDER || randomItem == Items.STRING || randomItem == Items.ROTTEN_FLESH) {
                                    stackSize = 1 + RANDOM.nextInt(5); // 1-5 items
                                } else if (randomItem == Items.BREAD || randomItem == Items.WHEAT) {
                                    stackSize = 1 + RANDOM.nextInt(3); // 1-3 items
                                } else if (randomItem == Items.DRAGON_BREATH) {
                                    stackSize = 1 + RANDOM.nextInt(3); // 1-3 items
                                } else if (randomItem == Items.DISC_FRAGMENT_5) {
                                    stackSize = 1 + RANDOM.nextInt(10); // 1-10 items
                                } else if (randomItem == Items.BREEZE_ROD) {
                                    stackSize = 1 + RANDOM.nextInt(2); // 1-2 items
                                }

                                if (DISC_LOOKUP.contains(randomItem)) {
                                    stackSize = 1;  // 1 disc
                                }

                                ItemStack lootStack = new ItemStack(randomItem, stackSize);

                                if (randomItem == Items.POTION || randomItem == Items.SPLASH_POTION || randomItem == Items.LINGERING_POTION) {
                                    Holder<Potion> randomBuff = HELPFUL_POTIONS.get(RANDOM.nextInt(HELPFUL_POTIONS.size()));
                                    lootStack.set(DataComponents.POTION_CONTENTS, new PotionContents(randomBuff));
                                }

                                // Choose a random available slot inside the barrel container boundary (0 to 26)
                                int randomSlot = RANDOM.nextInt(barrel.getContainerSize());

                                if (barrel.getItem(randomSlot).isEmpty()) {
                                    barrel.setItem(randomSlot, lootStack);
                                } else {
                                    barrel.removeItemNoUpdate(randomSlot);
                                    barrel.setItem(randomSlot, lootStack);
                                }
                            }

                            barrel.setChanged();
                            level.sendBlockUpdated(targetPos, Blocks.BARREL.defaultBlockState(), Blocks.BARREL.defaultBlockState(), 3);

                            if (verboseStructureConversionLogging) {
                                System.out.println("[FrogPixelSkyblock] Successfully transformed Vault into item barrel at: " + targetPos);
                            }
                        }
                    } else {
                        // if full loot in the trial replaced barrels (aka fullLootReward) was set to false
                        if (barrelTile instanceof BarrelBlockEntity barrel) {
                            // Clear out any residual structure data or old items to guarantee a blank slate
                            barrel.clearContent();

                            // Exact target bound: 3 to 8 unique slots max (3 + rolls 0-5 = 3 to 8)
                            int itemsToPlace = 3 + RANDOM.nextInt(6);
                            int itemsPlaced = 0;
                            boolean hasDiscAlready = false;

                            // Keep rolling until we satisfy our target item slots cleanly
                            while (itemsPlaced < itemsToPlace) {
                                Item randomItem;
                                if (!hasDiscAlready && RANDOM.nextInt(4) == 0) {
                                    randomItem = DUNGEON_DISC_POOL[RANDOM.nextInt(DUNGEON_DISC_POOL.length)];
                                } else {
                                    randomItem = DUNGEON_LOOT_POOL[RANDOM.nextInt(DUNGEON_LOOT_POOL.length)];
                                }

                                int stackSize = 1;
                                if (randomItem == Items.IRON_INGOT || randomItem == Items.GOLD_INGOT || randomItem == Items.COAL) {
                                    stackSize = 1 + RANDOM.nextInt(4);  // 1-4 items
                                } else if (randomItem == Items.GUNPOWDER || randomItem == Items.STRING || randomItem == Items.ROTTEN_FLESH) {
                                    stackSize = 1 + RANDOM.nextInt(5);  // 1-5 items
                                } else if (randomItem == Items.BREAD || randomItem == Items.WHEAT) {
                                    stackSize = 1 + RANDOM.nextInt(3); // 1-3 items
                                } else if (randomItem == Items.DRAGON_BREATH) {
                                    stackSize = 1 + RANDOM.nextInt(3); // 1-3 items
                                } else if (randomItem == Items.DISC_FRAGMENT_5) {
                                    stackSize = 1 + RANDOM.nextInt(10); // 1-10 items
                                } else if (randomItem == Items.BREEZE_ROD) {
                                    stackSize = 1 + RANDOM.nextInt(2); // 1-2 items
                                }

                                if (DISC_LOOKUP.contains(randomItem)) {
                                    stackSize = 1; // 1 disc
                                }

                                ItemStack lootStack = new ItemStack(randomItem, stackSize);

                                if (randomItem == Items.POTION || randomItem == Items.SPLASH_POTION || randomItem == Items.LINGERING_POTION) {
                                    Holder<Potion> randomBuff = HELPFUL_POTIONS.get(RANDOM.nextInt(HELPFUL_POTIONS.size()));
                                    lootStack.set(DataComponents.POTION_CONTENTS, new PotionContents(randomBuff));
                                }

                                // Pick a random index across the 27 available chest slots
                                int randomSlot = RANDOM.nextInt(barrel.getContainerSize());

                                // ONLY drop the item in if the slot is completely empty!
                                if (barrel.getItem(randomSlot).isEmpty()) {
                                    barrel.setItem(randomSlot, lootStack);
                                    itemsPlaced++; // Only advance toward completing the loop on a successful placement
                                }
                            }

                            barrel.setChanged();
                            level.sendBlockUpdated(targetPos, Blocks.BARREL.defaultBlockState(), Blocks.BARREL.defaultBlockState(), 3);

                            if (verboseStructureConversionLogging) {
                                System.out.println("[FrogPixelSkyblock] Successfully transformed Vault into item barrel at: " + targetPos);
                            }
                        }
                    }
                } else {
                    // Check what block is currently at the target position first
                    if (level.getBlockState(targetPos).is(Blocks.TRIAL_SPAWNER)) {

                        // CLEAR THE OLD BLOCK ENTITY (TRIAL SPAWNER) CACHE FIRST
                        level.removeBlockEntity(targetPos);

                        // REPLACE TRIAL SPAWNER WITH VANILLA BREAKABLE ONE
                        level.setBlock(targetPos, Blocks.SPAWNER.defaultBlockState(), 3);
                        BlockEntity newBlockEntity = level.getBlockEntity(targetPos);

                        if (newBlockEntity instanceof SpawnerBlockEntity spawnerTile) {
                            EntityType<?> chosenType = HOSTILE_MOBS[RANDOM.nextInt(HOSTILE_MOBS.length)];
                            spawnerTile.getSpawner().setEntityId(chosenType, level, level.getRandom(), targetPos);
                            spawnerTile.setChanged();

                            level.sendBlockUpdated(targetPos, Blocks.SPAWNER.defaultBlockState(), Blocks.SPAWNER.defaultBlockState(), 3);

                            if (verboseStructureConversionLogging) {
                                System.out.println("[FrogPixelSkyblock] REPLACED Trial Spawner with " + chosenType.getDescription().getString() + " Spawner at: " + targetPos);
                            }
                        }

                        // Load from config, do we want a torch on these spawners?
                        if (forceTorch) {
                            BlockPos torchPos = targetPos.above();

                            // Ensure we only place the torch if the space above is empty air
                            if (level.isEmptyBlock(torchPos)) {
                                level.setBlock(torchPos, Blocks.TORCH.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            } else {
                TASK_QUEUE.add(task);
            }
        }
    }
}