package com.ewaygames.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import com.ewaygames.FrogPixelSkyblock;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerInterceptionMixin {

    @Dynamic("Suppresses the un-mapped Sponge tick method.")
    @Inject(method = "tick", at = @At("HEAD"))
    private void interceptVoidFall(CallbackInfo ci) {
        // Now 'this' is safely a ServerPlayer!
        ServerPlayer player = (ServerPlayer) (Object) this;

        ServerLevel skyblockLevel = FrogPixelSkyblock.ModDimensions.getSkyblock(player);
        if (player.level() == skyblockLevel) {
            if (player.getY() < -64.0) {
                player.fallDistance = 0.0F;
                player.setDeltaMovement(Vec3.ZERO);

                // Route them back to the top
                FrogPixelSkyblock.executeSkyblockReroute(player);
            }
        }
    }
}