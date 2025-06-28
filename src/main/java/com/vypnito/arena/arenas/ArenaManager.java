package com.vypnito.arena.arenas;

import com.vypnito.arena.arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ArenaManager {

	private final arena plugin; // Reference na hlavní instanci pluginu
	private final Map<String, Arenas> arenas = new HashMap<>(); // Mapa pro uchovávání všech arén podle názvu
	private File arenasFile; // Soubor pro ukládání dat arén
	private FileConfiguration arenasConfig; // Konfigurace pro přístup k datům v souboru

	private static Set<Material> replaceableWallMaterials = new HashSet<>();

	public ArenaManager(arena plugin) {
		this.plugin = plugin;
		setupFiles();
		loadArenas();
		loadReplaceableMaterials();
	}

	public static Set<Material> getReplaceableWallMaterials() {
		return replaceableWallMaterials;
	}

	public void loadReplaceableMaterials() {
		replaceableWallMaterials.clear();
		List<String> materialNames = plugin.getConfig().getStringList("arena-settings.replaceable-materials");
		for (String name : materialNames) {
			try {
				Material material = Material.valueOf(name.toUpperCase());
				replaceableWallMaterials.add(material);
			} catch (IllegalArgumentException e) {
				plugin.getLogger().warning("Invalid replaceable material in config.yml: " + name);
			}
		}
		plugin.getLogger().info("Loaded " + replaceableWallMaterials.size() + " replaceable wall materials.");
	}

	private void setupFiles() {
		if (!plugin.getDataFolder().exists()) {
			plugin.getDataFolder().mkdir();
		}
		arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
		if (!arenasFile.exists()) {
			try {
				arenasFile.createNewFile();
				plugin.getLogger().info("Arena configuration file arenas.yml created.");
			} catch (IOException e) {
				plugin.getLogger().log(Level.SEVERE, "Error creating arenas.yml file: " + e.getMessage(), e);
			}
		}
		arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
	}

	private void loadArenas() {
		if (arenasConfig.contains("arenas")) {
			ConfigurationSection arenasSection = arenasConfig.getConfigurationSection("arenas");
			if (arenasSection != null) {
				for (String arenaName : arenasSection.getKeys(false)) {
					try {
						Location pos1 = (Location) arenasSection.get(arenaName + ".pos1");
						Location pos2 = (Location) arenasSection.get(arenaName + ".pos2");

						ConfigurationSection settingsSection = arenasSection.getConfigurationSection(arenaName + ".settings");
						ArenaSettings settings = new ArenaSettings();

						if (settingsSection != null) {
							settings.setWallMaterial(Material.valueOf(settingsSection.getString("wall-material", "GLASS")));
							settings.setAllowBlockBreak(settingsSection.getBoolean("allow-block-break", false));
							settings.setAllowBlockPlace(settingsSection.getBoolean("allow-block-place", false));
							settings.setAllowItemDrop(settingsSection.getBoolean("allow-item-drop", false));
							settings.setDisableHunger(settingsSection.getBoolean("disable-hunger", true));
							settings.setWallRemovalDelay(settingsSection.getInt("wall-removal-delay-seconds", 30));
							settings.getEffects().clear();
							settings.getEffects().addAll(settingsSection.getStringList("effects"));
							// NEW: Load requiredPlayers
							settings.setRequiredPlayers(settingsSection.getInt("required-players", 2));
						}

						Arenas arena = new Arenas(arenaName, pos1, pos2, settings);
						arenas.put(arenaName, arena);
						plugin.getLogger().info("Arena '" + arenaName + "' loaded successfully.");
					} catch (Exception e) {
						plugin.getLogger().log(Level.WARNING, "Error loading arena '" + arenaName + "': " + e.getMessage(), e);
					}
				}
			}
		}
	}

	public void saveArena(Arenas arena) {
		arenasConfig.set("arenas." + arena.getName() + ".pos1", arena.getPos1());
		arenasConfig.set("arenas." + arena.getName() + ".pos2", arena.getPos2());

		ConfigurationSection settingsSection = arenasConfig.createSection("arenas." + arena.getName() + ".settings");
		settingsSection.set("wall-material", arena.getSettings().getWallMaterial().name());
		settingsSection.set("allow-block-break", arena.getSettings().isAllowBlockBreak());
		settingsSection.set("allow-block-place", arena.getSettings().isAllowBlockPlace());
		settingsSection.set("allow-item-drop", arena.getSettings().isAllowItemDrop());
		settingsSection.set("disable-hunger", arena.getSettings().isDisableHunger());
		settingsSection.set("wall-removal-delay-seconds", arena.getSettings().getWallRemovalDelay());
		settingsSection.set("effects", arena.getSettings().getEffects());
		// NEW: Save requiredPlayers
		settingsSection.set("required-players", arena.getSettings().getRequiredPlayers());

		try {
			arenasConfig.save(arenasFile);
			plugin.getLogger().info("Arena '" + arena.getName() + "' saved successfully.");
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Error saving arena '" + arena.getName() + "': " + e.getMessage(), e);
		}
	}

	/**
	 * Creates a new arena with the given name, positions, and specified required players.
	 * Automatically saves the arena. Walls are built only when players enter.
	 * @param name The name of the new arena.
	 * @param pos1 The first selected position.
	 * @param pos2 The second selected position.
	 * @param requiredPlayers The number of players required to activate this arena.
	 */
	public void createArena(String name, Location pos1, Location pos2, int requiredPlayers) { // NEW: added requiredPlayers
		if (arenas.containsKey(name)) {
			plugin.getLogger().warning("Cannot create arena: Arena '" + name + "' already exists!");
			return;
		}

		ArenaSettings defaultSettings = new ArenaSettings();
		// NEW: Set requiredPlayers in default settings
		defaultSettings.setRequiredPlayers(requiredPlayers);

		Arenas newArena = new Arenas(name, pos1, pos2, defaultSettings);
		arenas.put(name, newArena);
		saveArena(newArena);

		plugin.getLogger().info("New arena '" + name + "' created with " + requiredPlayers + " required players."); // Anglický text
	}

	public Arenas getArena(String name) {
		return arenas.get(name);
	}

	public void deleteArena(String name) {
		if (arenas.containsKey(name)) {
			arenas.remove(name);
			arenasConfig.set("arenas." + name, null);
			try {
				arenasConfig.save(arenasFile);
				plugin.getLogger().info("Arena '" + name + "' deleted successfully.");
			} catch (IOException e) {
				plugin.getLogger().log(Level.SEVERE, "Error deleting arena '" + name + "': " + e.getMessage(), e);
			}
		} else {
			plugin.getLogger().warning("Cannot delete arena: Arena '" + name + "' does not exist!");
		}
	}

	public List<String> getArenaNames() {
		return new ArrayList<>(arenas.keySet());
	}

	public Arenas findArenaByRegion(Location location) {
		if (location.getWorld() == null) {
			return null;
		}

		for (Arenas arena : arenas.values()) {
			if (arena.getPos1() != null && arena.getPos2() != null &&
					arena.getPos1().getWorld() != null && arena.getPos2().getWorld() != null &&
					arena.getPos1().getWorld().equals(location.getWorld())) {

				int minX = Math.min(arena.getPos1().getBlockX(), arena.getPos2().getBlockX());
				int minY = Math.min(arena.getPos1().getBlockY(), arena.getPos2().getBlockY());
				int minZ = Math.min(arena.getPos1().getBlockZ(), arena.getPos2().getBlockZ());
				int maxX = Math.max(arena.getPos1().getBlockX(), arena.getPos2().getBlockX());
				int maxY = Math.max(arena.getPos1().getBlockY(), arena.getPos2().getBlockY());
				int maxZ = Math.max(arena.getPos1().getBlockZ(), arena.getPos2().getBlockZ());

				int locX = location.getBlockX();
				int locY = location.getBlockY();
				int locZ = location.getBlockZ();

				if (locX >= minX && locX <= maxX &&
						locY >= minY && locY <= maxY &&
						locZ >= minZ && locZ <= maxZ) {
					return arena;
				}
			}
		}
		return null;
	}
}