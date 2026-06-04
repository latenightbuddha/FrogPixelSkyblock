package com.ewaygames.mixin;

import com.ewaygames.FrogPixelSkyblock;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.network.Connection;
import net.minecraft.server.network.CommonListenerCookie;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.ewaygames.FrogPixelSkyblock_Config.force_sky_dimension;

@Mixin(PlayerList.class)
public class PlayerSpawnMixin {

    @Dynamic("Suppresses the un-mapped Sponge placeNewPlayer method.")
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void forcePlayerToSkyblockDimensionOnJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        if (player.level().dimension() == Level.OVERWORLD && force_sky_dimension) {
            com.ewaygames.FrogPixelSkyblock.queueDelayedTeleport(player, 10);
        }
    }

    // Note: respawn returns a ServerPlayer instance, so we use CallbackInfoReturnable<ServerPlayer>
    @Dynamic("Suppresses the un-mapped Sponge respawn method.")
    @Inject(method = "respawn", at = @At("TAIL"))
    private void forcePlayerToSkyblockDimensionOnRespawn(
            ServerPlayer player,
            boolean keepEverything,
            net.minecraft.world.entity.Entity.RemovalReason removalReason,
            CallbackInfoReturnable<ServerPlayer> cir) {

            // Fetch the newly recreated player instance returned by the respawn method
            ServerPlayer newPlayerInstance = cir.getReturnValue();

            // If the game tried to dump their dead soul back into the vanilla Overworld, catch them!
            if (newPlayerInstance.level().dimension() == Level.OVERWORLD && force_sky_dimension) {
                FrogPixelSkyblock.executeSkyblockReroute(newPlayerInstance);
            }

    }
}