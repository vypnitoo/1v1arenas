package com.vypnito.arena.arenas;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents an arena with its defined boundaries and settings.
 * Manages players within the arena and the state of its boundary walls,
 * including preserving and restoring original blocks.
 */
public class Arenas {

	private final String name;
	private Location pos1;
	private Location pos2;
	private ArenaSettings settings;

	// --- Variables for arena state management ---
	private final Set<UUID> playersInArena = new HashSet<>(); // Sleduje UUID hráčů aktuálně v aréně
	private boolean wallActive = false; // Sleduje, zda je zeď arény aktivní (postavená)
	private final Map<Location, BlockData> originalWallBlocks = new HashMap<>(); // Ukládá původní stav bloků zdi
	// --- End of arena state management variables ---

	/**
	 * Constructor for the Arenas class.
	 * @param name The name of the arena.
	 * @param pos1 The first selected position of the arena.
	 * @param pos2 The second selected position of the arena.
	 * @param settings The settings for the arena.
	 */
	public Arenas(String name, Location pos1, Location pos2, ArenaSettings settings) {
		this.name = name;
		this.pos1 = pos1;
		this.pos2 = pos2;
		this.settings = settings;
	}

	// --- Getters and Setters ---

	public String getName() {
		return name;
	}

	public Location getPos1() {
		return pos1;
	}

	public void setPos1(Location pos1) {
		this.pos1 = pos1;
	}

	public Location getPos2() {
		return pos2;
	}

	public void setPos2(Location pos2) {
		this.pos2 = pos2;
	}

	public ArenaSettings getSettings() {
		return settings;
	}

	public void setSettings(ArenaSettings settings) {
		this.settings = settings;
	}

	// --- Methods for managing players within the arena ---

	/**
	 * Adds a player's UUID to the set of players currently in the arena.
	 * @param playerUuid The UUID of the player to add.
	 */
	public void addPlayer(UUID playerUuid) {
		playersInArena.add(playerUuid);
	}

	/**
	 * Removes a player's UUID from the set of players currently in the arena.
	 * @param playerUuid The UUID of the player to remove.
	 */
	public void removePlayer(UUID playerUuid) {
		playersInArena.remove(playerUuid);
	}

	/**
	 * Gets a copy of the set of UUIDs of all players currently in the arena.
	 * @return A new Set containing the UUIDs of players in the arena.
	 */
	public Set<UUID> getPlayers() {
		return new HashSet<>(playersInArena);
	}

	/**
	 * Checks if the given player is inside this arena (by their UUID).
	 * @param player The Player object to check.
	 * @return True if the player is in the arena, otherwise false.
	 */
	public boolean isPlayerInArena(Player player) {
		return playersInArena.contains(player.getUniqueId());
	}

	// --- Metody pro správu stavu zdi ---

	/**
	 * Gets the current wall active status.
	 * @return True if the wall is currently active (built), otherwise false.
	 */
	public boolean isWallActive() {
		return wallActive;
	}

	/**
	 * Builds the boundary wall of the arena using the material defined in settings.
	 * It replaces ONLY replaceable blocks (air, water, etc.) on the outer boundary of the arena.
	 * Stores the original state of replaced blocks for later restoration.
	 * This method MUST be called on the main server thread due to block manipulation.
	 */
	public void createBoundaryWall() {
		if (pos1 == null || pos2 == null || settings.getWallMaterial() == null) {
			return;
		}

		Material wallMaterial = settings.getWallMaterial();
		originalWallBlocks.clear(); // Clear any old stored blocks from previous wall states

		// Získáme nahraditelné materiály z ArenaManageru
		Set<Material> replaceableMaterials = ArenaManager.getReplaceableWallMaterials();
		if (replaceableMaterials.isEmpty()) {
			// Zde by bylo vhodné logovat, pokud nejsou definovány žádné nahraditelné materiály
			// plugin.getLogger().warning("No replaceable materials defined for arena walls!");
			return; // Zabraňte stavbě zdi, pokud nevíme, co nahradit
		}

		// Determine min and max coordinates for iterating through the arena region
		int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
		int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
		int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
		int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
		int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
		int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

		// Iterate through all blocks in the defined region
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Location currentLocation = new Location(pos1.getWorld(), x, y, z);

					if (isLocationInWall(currentLocation)) {
						Block block = currentLocation.getBlock();

						// --- KLÍČOVÁ PODMÍNKA: Používáme dynamicky načtené nahraditelné materiály ---
						if (replaceableMaterials.contains(block.getType())) {
							originalWallBlocks.put(currentLocation.clone(), block.getBlockData());
							block.setType(wallMaterial);
						} else if (block.getType() == wallMaterial) {
							// Pokud je blok už materiálem zdi, nezměníme ho, ale uložíme jako "už zeď"
							originalWallBlocks.put(currentLocation.clone(), block.getBlockData());
						}
					}
				}
			}
		}
		wallActive = true;
	}

	/**
	 * Removes the boundary wall of the arena and restores the original blocks that were replaced.
	 * This method MUST be called on the main server thread due to block manipulation.
	 */
	public void removeBoundaryWall() {
		if (pos1 == null || pos2 == null) {
			return;
		}

		for (Map.Entry<Location, BlockData> entry : originalWallBlocks.entrySet()) {
			Location loc = entry.getKey();
			BlockData data = entry.getValue();
			if (loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getChunk())) {
				loc.getBlock().setBlockData(data);
			}
		}
		originalWallBlocks.clear();
		wallActive = false;
	}

	/**
	 * Checks if the given location is on the "boundary" of the arena's bounding box.
	 * This method defines which blocks form the arena wall (the outline of the 3D box).
	 * @param location The block location to check.
	 * @return True if the location is on the boundary (part of the wall), otherwise false.
	 */
	public boolean isLocationInWall(Location location) {
		if (pos1 == null || pos2 == null || location.getWorld() == null ||
				!location.getWorld().equals(pos1.getWorld())) {
			return false;
		}

		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();

		int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
		int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
		int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
		int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
		int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
		int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

		boolean isInBoundedRegion = (x >= minX && x <= maxX &&
				y >= minY && y <= maxY &&
				z >= minZ && z <= maxZ);

		if (!isInBoundedRegion) {
			return false;
		}

		boolean isXBoundary = (x == minX || x == maxX);
		boolean isYBoundary = (y == minY || y == maxY);
		boolean isZBoundary = (z == minZ || z == maxZ);

		return isXBoundary || isYBoundary || isZBoundary;
	}
}