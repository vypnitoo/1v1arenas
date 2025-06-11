package com.vypnito.vyp1v1.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Arena {
	private final String name;
	private final Location pos1, pos2;
	private final ArenaSettings settings;
	private final List<UUID> players = new ArrayList<>();
	private boolean isWallActive = false;

	public Arena(String name, Location pos1, Location pos2, ArenaSettings settings) {
		this.name = name;
		this.pos1 = pos1;
		this.pos2 = pos2;
		this.settings = settings;
	}

	public String getName() { return name; }
	public ArenaSettings getSettings() { return settings; }
	public List<UUID> getPlayers() { return players; }
	public boolean isWallActive() { return isWallActive; }

	public void addPlayer(Player player) {
		if (!players.contains(player.getUniqueId())) {
			players.add(player.getUniqueId());
		}
	}

	public void removePlayer(Player player) {
		players.remove(player.getUniqueId());
	}

	private void setBlockIf(World world, int x, int y, int z, Material check, Material place) {
		Block block = world.getBlockAt(x, y, z);
		if (block.getType() == check) {
			block.setType(place, false);
		}
	}

	private void changeBoundaryWall(Material targetMaterial, Material sourceMaterial) {
		World world = pos1.getWorld();
		if (world == null) return;

		int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
		int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
		int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
		int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
		int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
		int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ) {
						setBlockIf(world, x, y, z, sourceMaterial, targetMaterial);
					}
				}
			}
		}
	}

	public void createBoundaryWall() {
		if (isWallActive) return;
		changeBoundaryWall(Material.GLASS, Material.AIR);
		isWallActive = true;
	}

	public void removeBoundaryWall() {
		if (!isWallActive) return;
		changeBoundaryWall(Material.AIR, Material.GLASS);
		isWallActive = false;
	}

	public boolean isWithinRegion(Location loc) {
		if (pos1.getWorld() == null || !pos1.getWorld().equals(loc.getWorld())) return false;

		double minX = Math.min(pos1.getX(), pos2.getX());
		double minY = Math.min(pos1.getY(), pos2.getY());
		double minZ = Math.min(pos1.getZ(), pos2.getZ());
		double maxX = Math.max(pos1.getX(), pos2.getX());
		double maxY = Math.max(pos1.getY(), pos2.getY());
		double maxZ = Math.max(pos1.getZ(), pos2.getZ());

		return loc.getX() >= minX && loc.getX() <= maxX &&
				loc.getY() >= minY && loc.getY() <= maxY &&
				loc.getZ() >= minZ && loc.getZ() <= maxZ;
	}
}