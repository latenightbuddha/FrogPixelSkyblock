package com.ewaygames.mixin;

import com.ewaygames.SkyblockCleanUpQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureTemplate.class)
public class TrialSpawnerEraserMixin {
    @Dynamic("Suppresses potential un-mapped/re-mapped runtime translation warnings for setLevel.")
    @Inject(
            method = "placeInWorld",
            at = @At("RETURN")
    )
    private static void postStructureGeneration(
            ServerLevelAccessor accessor,
            BlockPos targetPos,
            BlockPos offsetPos,
            StructurePlaceSettings settings,
            RandomSource random,
            int flags,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (accessor.getLevel() instanceof ServerLevel serverLevel) {

            boolean isSkyblock = serverLevel.dimension().toString().contains("skyblock");

            if (isSkyblock) {
                // using the helper method built into StructureTemplate

                BlockPos minPos = targetPos.offset(-32, -16, -32);
                BlockPos maxPos = targetPos.offset(32, 32, 32);

                // Scan every single block within the actual generated bounds
                BlockPos.betweenClosedStream(minPos, maxPos).forEach(pos -> {
                    BlockState state = accessor.getBlockState(pos);

                    if (state.is(Blocks.VAULT)) {
                        SkyblockCleanUpQueue.enqueue(serverLevel.dimension(), pos.immutable(), false);
                    }
                    else if (state.is(Blocks.TRIAL_SPAWNER)) {
                        SkyblockCleanUpQueue.enqueue(serverLevel.dimension(), pos.immutable(), true);
                    }
                });
            }
        }
    }
}