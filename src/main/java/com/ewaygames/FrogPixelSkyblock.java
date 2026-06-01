package com.ewaygames;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;

public class FrogPixelSkyblock implements ModInitializer {
	public static final String MOD_ID = "frogpixelskyblock";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final String SKY_DIM = MOD_ID + ":skyblock_dimension";

	private static final List<DelayedTeleport> delayedTeleports = new ArrayList<>();

	@Override
	public void onInitialize() {

		// Triggers when running under strict dedicated production server conditions
		if (FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER)
		{
			System.out.println("[FrogPixelSkyblock] Initializing Skyblock Configuration");
			FrogPixelSkyblock_Config.loadConfig();
		}

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Iterator<DelayedTeleport> iterator = delayedTeleports.iterator();
			while (iterator.hasNext()) {
				DelayedTeleport task = iterator.next();
				task.ticksRemaining--;

				ServerPlayer player = task.player;

				// Verify the player is still active and online
				if (server.getPlayerList().getPlayer(player.getUUID()) != null) {

					if (task.ticksRemaining <= 0) {
						// TIME IS UP: Reset all momentum and drop them safely on the platform
						player.fallDistance = 0.0F;
						player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

						// Route them to the Skyblock platform
						executeSkyblockReroute(player);

						iterator.remove();
					} else {
						// STILL WAITING: Freeze the player in the air, so they don't fall during the delay
						player.fallDistance = 0.0F;
						player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
					}
				} else {
					// Player disconnected during the loading screen, clean up the task
					iterator.remove();
				}
			}
		});

		// This fires as soon as a player joins
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
		/* Optional Debugging Information
			var player = handler.getPlayer();
			String uuid = player.getUUID().toString();
			String ipAddress = handler.getRemoteAddress().toString().split(":")[0].replace("/", "");

			// Trigger your auth logic here (e.g., check database or send a packet)
			LOGGER.info("Player {} ({}) connected from IP: {}", player.getName().getString(), uuid, ipAddress);
		*/

		});

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			// Get the lookup for the dimension registry
			var dimensionLookup = server.registryAccess().lookupOrThrow(Registries.DIMENSION);

			if (dimensionLookup.get(ModDimensions.SKY.getKey()).isPresent()) {
				System.out.println("Skyblock dimension \"" + SKY_DIM + "\" was found in registry!");
			} else {
				System.out.println("Skyblock dimension \"" + SKY_DIM + "\" is still missing!");
			}
		});

		// Process the queue at the end of every tick
		ServerTickEvents.END_SERVER_TICK.register(server -> {

				// Create a temporary list for players ready to be teleported
				List<UUID> readyToTeleport = new ArrayList<>();

				for (UUID uuid : readyToTeleport) {
					ServerPlayer player = server.getPlayerList().getPlayer(uuid);

					movePlayerToDimension(player, ModDimensions.SKY.getLevel(player), new Vec3(0, 80.0, 0));
				}


		});

		// Register the disconnection event
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.getPlayer();

			// Capture position data
			Vec3 pos = player.position();
			float yaw = player.getYRot();
			float pitch = player.getXRot();

			String fullDimensionString = player.level().dimension().toString();
			String[] parts = fullDimensionString.split("/");
			String dimension = parts[parts.length - 1];

			// Log the data for your security/session audit
			LOGGER.info("Player {}({}) disconnected position [X:{}, Y:{}, Z:{}] player [yaw:{}, pitch:{}]  in dimension: {}",
					player.getName().getString(), player.getUUID(), pos.x, pos.y, pos.z, yaw, pitch, dimension);

		});

		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, player, params) -> {

			String rawMessage = message.signedContent();
			if (rawMessage.equalsIgnoreCase("!skyblock")) {

				try {
					Vec3 targetLocation = new Vec3(0.5, 65.0, 0.5);
					player.sendSystemMessage(Component.literal("§aTeleporting to the Skyblock island..."));
					// Use your verified, cross-world dimension routing system to teleport them
					FrogPixelSkyblock.movePlayerToDimension(player, ModDimensions.SKY.getLevel(player), targetLocation);

				} catch (Exception e) {
					System.err.println("[FrogPixelSkyblock] Failed to process !skyblock chat command: " + e.getMessage());
					e.printStackTrace();
				}

				return false;
			}
			return true;
		});



		ServerTickEvents.START_SERVER_TICK.register(server -> {

		});

		ServerTickEvents.END_SERVER_TICK.register(server ->
		{
			for (ServerLevel level : server.getAllLevels())
			{
				SkyblockCleanUpQueue.processQueue(level);
			}
		});
	}

	// A helper method you can call from your Login Mixin to queue a player
	public static void queueDelayedTeleport(ServerPlayer player, int ticks) {
		delayedTeleports.add(new DelayedTeleport(player, ticks));
	}



	// Small container class to hold our delay data
	private static class DelayedTeleport {
		final ServerPlayer player;
		int ticksRemaining;

		DelayedTeleport(ServerPlayer player, int ticks) {
			this.player = player;
			this.ticksRemaining = ticks;
		}
	}

	public enum ModDimensions {
		OVERWORLD("minecraft:overworld", Level.OVERWORLD),
		NETHER("minecraft:the_nether", Level.NETHER),
		END("minecraft:the_end", Level.END),
		SKY(SKY_DIM, null);

		private final String id;
		private final ResourceKey<Level> key;

		// Constructor for existing keys (Overworld/Nether/End)
		ModDimensions(String id, ResourceKey<Level> key) {
			this.id = id;
			this.key = key;
		}

		// Helper to get or create the key
		public ResourceKey<Level> getKey() {
			if (this.key != null) return this.key;
			return ResourceKey.create(Registries.DIMENSION, Identifier.tryParse(this.id));
		}

		public ServerLevel getLevel(ServerPlayer player) {
			return player.level().getServer().getLevel(this.getKey());
		}

		public static ServerLevel getSkyblock(ServerPlayer player) {
			return ModDimensions.SKY.getLevel(player);
		}


		public static Optional<ModDimensions> fromId(String id) {
			return Arrays.stream(values())
					.filter(d -> d.id.equals(id))
					.findFirst();
		}
	}

	public static void kickPlayer(ServerPlayer player, String reason) {
		// Disconnect the player with a formatted message
		player.connection.disconnect(Component.literal("§c FrogPixelSkyblock: " + reason));

		// Broadcast the message
		sendServerMessage(player, reason);

		LOGGER.info("Player {} was kicked: {}", player.getName().getString(), reason);
	}

	@Unique
	public static void executeSkyblockReroute(ServerPlayer player) {
		FrogPixelSkyblock.movePlayerToDimension(
				player,
				FrogPixelSkyblock.ModDimensions.getSkyblock(player),
				new net.minecraft.world.phys.Vec3(0.5, 65.0, 0.5),
				new net.minecraft.world.phys.Vec2(0, 0),
				false
		);
	}

	public static void sendServerMessage(ServerPlayer player, String message) {
		// Access the server through the level field (standard in all versions)
		var server = player.level().getServer();

		// Broadcast the message
		server.getPlayerList().broadcastSystemMessage(
				Component.literal("§eFrogPixelSkyblock: " + message),
				false
		);
	}

	public static void movePlayerToDimension(ServerPlayer player, ServerLevel targetLevel, Vec3 pos, Vec2 yawpitch, boolean resetCamera) {

		if (targetLevel == null || targetLevel.getServer().getLevel(targetLevel.dimension()) == null) {
			System.err.println("[FrogPixelSkyblock] Teleport aborted: Target dimension does not exist or is not loaded!");
			player.sendSystemMessage(Component.literal("§c[FrogPixelSkyblock] Teleport Error: The dimension does not exist or is not loaded!"));
			return;
		}

		player.teleportTo(
				targetLevel,            // Dimension
				pos.x, pos.y, pos.z,    // X, Y, Z
				Set.of(),               // Empty set for 'relatives' (means absolute coordinates)
				yawpitch.y,             // Yaw
				yawpitch.x,             // Pitch
				resetCamera             // Reset Camera
		);
	}

	public static void movePlayerToDimension(ServerPlayer player, ServerLevel targetLevel, Vec3 pos) {
		movePlayerToDimension(
				player,                                        // This Player
				targetLevel,                                   // Dimension
				new Vec3(pos.x, pos.y, pos.z),                 // X, Y, Z
				new Vec2(player.getYRot(), player.getXRot()),  // Yaw and Pitch
				true                                           // Reset Camera
		);
	}
}