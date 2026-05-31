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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

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
            Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_OTHERSIDE,
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.GUNPOWDER,
            Items.STRING, Items.ROTTEN_FLESH, Items.BREAD, Items.WHEAT,
            Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.COAL, Items.REDSTONE,

            Items.TOTEM_OF_UNDYING, Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION
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

    public record ModificationTask(ResourceKey<Level> dimension, BlockPos pos, boolean isTrialSpawner) {}
    private static final Queue<ModificationTask> TASK_QUEUE = new ConcurrentLinkedQueue<>();

    public static void enqueue(ResourceKey<Level> dimension, BlockPos pos, boolean isTrialSpawner) {
        TASK_QUEUE.add(new ModificationTask(dimension, pos, isTrialSpawner));
    }

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
                    // REPLACE VAULT WITH CHEST
                    level.setBlock(targetPos, Blocks.CHEST.defaultBlockState(), 3);
                    BlockEntity chestTile = level.getBlockEntity(targetPos);

                    if (chestTile instanceof ChestBlockEntity chest) {
                        // Determine a random number of items to put in this specific chest (e.g., 3 to 7 slots populated)
                        int itemsToPlace = 3 + RANDOM.nextInt(5);

                        for (int count = 0; count < itemsToPlace; count++) {
                            // Select a completely random item profile from our array
                            Item randomItem = DUNGEON_LOOT_POOL[RANDOM.nextInt(DUNGEON_LOOT_POOL.length)];

                            // Scale individual stack counts contextually (e.g., resources stack, gear doesn't)
                            int stackSize = 1;
                            if (randomItem == Items.IRON_INGOT || randomItem == Items.GOLD_INGOT || randomItem == Items.COAL) {
                                stackSize = 1 + RANDOM.nextInt(4); // 1-4 items
                            } else if (randomItem == Items.GUNPOWDER || randomItem == Items.STRING || randomItem == Items.ROTTEN_FLESH) {
                                stackSize = 1 + RANDOM.nextInt(5); // 1-5 items
                            } else if (randomItem == Items.BREAD || randomItem == Items.WHEAT) {
                                stackSize = 1 + RANDOM.nextInt(3); // 1-3 items
                            }

                            // Generate the itemstack
                            ItemStack lootStack = new ItemStack(randomItem, stackSize);

                            // 👈 POTION CONTENT PROCESSING BLOCK
                            // If the item rolled is a potion variant, inject a useful buff data component
                            if (randomItem == Items.POTION || randomItem == Items.SPLASH_POTION || randomItem == Items.LINGERING_POTION) {
                                Holder<Potion> randomBuff = HELPFUL_POTIONS.get(RANDOM.nextInt(HELPFUL_POTIONS.size()));
                                lootStack.set(DataComponents.POTION_CONTENTS, new PotionContents(randomBuff));
                            }

                            // Choose a random available slot inside the chest container boundary (0 to 26)
                            int randomSlot = RANDOM.nextInt(chest.getContainerSize());

                            // If the slot is empty or occupied, place the stack inside safely
                            if (chest.getItem(randomSlot).isEmpty()) {
                                chest.setItem(randomSlot, lootStack);
                            } else {
                                chest.removeItemNoUpdate(randomSlot);
                                chest.setItem(randomSlot, lootStack);
                            }
                        }

                        chest.setChanged();
                        level.sendBlockUpdated(targetPos, Blocks.CHEST.defaultBlockState(), Blocks.CHEST.defaultBlockState(), 3);
                        System.out.println("[FrogPixelSkyblock] Successfully transformed Vault into custom scratch item chest at: " + targetPos);
                    }
                } else {
                    // REPLACE TRIAL SPAWNER WITH VANILLA BREAKABLE ONE
                    level.setBlock(targetPos, Blocks.SPAWNER.defaultBlockState(), 3);
                    BlockEntity newBlockEntity = level.getBlockEntity(targetPos);

                    if (newBlockEntity instanceof SpawnerBlockEntity spawnerTile) {
                        EntityType<?> chosenType = HOSTILE_MOBS[RANDOM.nextInt(HOSTILE_MOBS.length)];
                        spawnerTile.getSpawner().setEntityId(chosenType, level, level.getRandom(), targetPos);
                        spawnerTile.setChanged();

                        level.sendBlockUpdated(targetPos, Blocks.SPAWNER.defaultBlockState(), Blocks.SPAWNER.defaultBlockState(), 3);
                        System.out.println("[FrogPixelSkyblock] HARD REPLACED Trial Spawner at: " + targetPos);
                    }
                }
            } else {
                TASK_QUEUE.add(task);
            }
        }
    }
}