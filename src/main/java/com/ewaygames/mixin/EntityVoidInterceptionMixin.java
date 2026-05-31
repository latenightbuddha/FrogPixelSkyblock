package com.ewaygames.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityVoidInterceptionMixin {

    // Helper method to determine if an entity should be saved from the void
    @Unique
    private boolean shouldSaveEntity(Entity entity) {
        // We only care about interactive mobs (animals, villagers, golems, etc.)
        if (entity instanceof Mob mob) {
            // Get the official spawn category classification
            MobCategory category = mob.getType().getCategory();

            // Reject anything explicitly classified as a monster
            return category != MobCategory.MONSTER;
        }
        return false;
    }

    @Dynamic("Suppresses the un-mapped Sponge baseTick method.")
    @Inject(method = "baseTick", at = @At("HEAD"))
    private void interceptEntityVoidFall(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel currentLevel)) return;

        if (shouldSaveEntity(entity)) {
            // Catch them early before they sink below the engine's hard floor
            if (entity.getY() < -64.0) {

                entity.fallDistance = 0.0F;
                entity.setDeltaMovement(Vec3.ZERO);

                // Target position (Y=65 to match where they are landing)
                BlockPos targetSpawn = new BlockPos(0, 65, 0);
                Vec3 destination = new Vec3(targetSpawn.getX() + 0.5, targetSpawn.getY(), targetSpawn.getZ() + 0.5);

                TeleportTransition transition = new TeleportTransition(
                        currentLevel,
                        destination,
                        Vec3.ZERO,
                        entity.getYRot(),
                        entity.getXRot(),
                        TeleportTransition.DO_NOTHING
                );

                entity.teleport(transition);

                // Clear physics again post-teleport
                entity.fallDistance = 0.0F;
                entity.setOnGround(true);
            }
        }
    }

    // Intercepts the exact method that processes landing impact damage
    @Dynamic("Suppresses the un-mapped Sponge causeFallDamage method.")
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void cancelVillagerVoidFallDamage(double fallDistance, float damageMultiplier, net.minecraft.world.damagesource.DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;

        if (entity instanceof Villager) {
            // Updated comparison to handle the double type safely
            if (fallDistance > 20.0 || entity.getY() < -60.0) {
                entity.fallDistance = 0.0F; // Note: entity.fallDistance field itself remains a float in vanilla
                cir.setReturnValue(false); // Forcefully tells Minecraft: "This entity takes 0 damage"
            }
        }
    }

    // Injects into the method that checks if the entity's head is trapped inside a solid block
    @Dynamic("Suppresses the un-mapped Sponge isInWall method.")
    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    private void overrideIsInWallCheck(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;

        // Forcefully tell the engine that non-hostile mobs are NEVER inside a wall,
        // which completely short-circuits the internal vanilla suffocation execution block!
        if (shouldSaveEntity(entity)) {
            cir.setReturnValue(false);
        }
    }

    // Intercepts when the entity takes damage from being stuck inside a block
    @Dynamic("Suppresses the un-mapped Sponge hurt method.")
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void rescueSuffocatingEntity(DamageSource source, float amount, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel currentLevel)) return;

        if (shouldSaveEntity(entity)) {
            // Check if the damage type is specifically 'inWall' (Suffocation)
            if (source.is(DamageTypes.IN_WALL)) {

                // Find a safe spot to eject them. We scan a small area around them for air blocks.
                BlockPos currentPos = entity.blockPosition();
                BlockPos safePos = null;

                // Simple 3x3 search pattern to find a spot that isn't solid
                outerLoop:
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        for (int y = 0; y <= 2; y++) {
                            BlockPos checkPos = currentPos.offset(x, y, z);
                            // If the block is air or non-solid, and has space above it
                            if (currentLevel.getBlockState(checkPos).isAir() && currentLevel.getBlockState(checkPos.above()).isAir()) {
                                safePos = checkPos;
                                break outerLoop;
                            }
                        }
                    }
                }

                // If we found a safe spot nearby, nudge them into it
                if (safePos != null) {
                    entity.setPos(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                } else {
                    // Fallback: If completely buried by leaves/logs, snap them back to the center spawn platform anchor
                    BlockPos fallbackSpawn = new BlockPos(0, 65, 0);
                    entity.setPos(fallbackSpawn.getX() + 0.5, fallbackSpawn.getY(), fallbackSpawn.getZ() + 0.5);
                }

                entity.clearFire();

                // Cancel the void processing chain early to drop out before damage updates occur
                ci.cancel();
            }
        }
    }
}