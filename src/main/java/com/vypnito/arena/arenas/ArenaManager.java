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

/**
 * Třída ArenaManager spravuje všechny arény pluginu. Zahrnuje funkcionalitu
 * pro načítání, ukládání, vytváření a získávání arén.
 */
public class ArenaManager {

	private final arena plugin;
	private final Map<String, Arenas> arenas = new HashMap<>();
	private File arenasFile;
	private FileConfiguration arenasConfig;

	// --- Nová statická proměnná pro nahraditelné materiály ---
	private static Set<Material> replaceableWallMaterials = new HashSet<>();
	// --- Konec nové proměnné ---

	/**
	 * Konstruktor ArenaManageru. Inicializuje správce souborů a načte existující arény.
	 * @param plugin Hlavní instance tvého pluginu.
	 */
	public ArenaManager(arena plugin) {
		this.plugin = plugin;
		setupFiles();
		loadArenas();
		loadReplaceableMaterials(); // Voláme při spuštění, po načtení configu
	}

	/**
	 * Získá statickou sadu nahraditelných materiálů.
	 * @return Set<Material> s materiály, které lze nahradit zdí.
	 */
	public static Set<Material> getReplaceableWallMaterials() {
		return replaceableWallMaterials;
	}

	/**
	 * Načte materiály, které lze nahradit zdí, z konfiguračního souboru.
	 */
	private void loadReplaceableMaterials() {
		replaceableWallMaterials.clear(); // Vyčistíme staré materiály před načtením nových
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

	/**
	 * Zajišťuje existenci složky s daty pluginu a souboru arenas.yml.
	 */
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

	/**
	 * Načte všechny arény ze souboru arenas.yml do paměti.
	 */
	public void loadArenas() {
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

	/**
	 * Uloží data jedné arény do souboru arenas.yml.
	 * @param arena Aréna, která má být uložena nebo aktualizována.
	 */
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

		try {
			arenasConfig.save(arenasFile);
			plugin.getLogger().info("Arena '" + arena.getName() + "' saved successfully.");
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Error saving arena '" + arena.getName() + "': " + e.getMessage(), e);
		}
	}

	/**
	 * Vytvoří novou arénu s daným názvem a pozicami.
	 * Automaticky uloží arénu. Zeď se staví až při vstupu hráčů.
	 * @param name Název nové arény.
	 * @param pos1 První vybraná pozice.
	 * @param pos2 Druhá vybraná pozice.
	 */
	public void createArena(String name, Location pos1, Location pos2) {
		if (arenas.containsKey(name)) {
			plugin.getLogger().warning("Cannot create arena: Arena '" + name + "' already exists!");
			return;
		}

		ArenaSettings defaultSettings = new ArenaSettings();
		Arenas newArena = new Arenas(name, pos1, pos2, defaultSettings);
		arenas.put(name, newArena);
		saveArena(newArena);

		plugin.getLogger().info("New arena '" + name + "' created.");
	}

	/**
	 * Získá instanci arény podle jejího názvu.
	 * @param name Název arény, kterou hledáte.
	 * @return Objekt Arenas, nebo null, pokud aréna s daným názvem neexistuje.
	 */
	public Arenas getArena(String name) {
		return arenas.get(name);
	}

	/**
	 * Odstraní arénu z paměti a z konfiguračního souboru.
	 * @param name Název arény k odstranění.
	 */
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

	/**
	 * Vrací seznam názvů všech arén.
	 * @return List Stringů obsahující názvy všech arén.
	 */
	public List<String> getArenaNames() {
		return new ArrayList<>(arenas.keySet());
	}

	/**
	 * Najde arénu, která obsahuje danou lokaci.
	 * @param location Lokace, kterou chceme zkontrolovat.
	 * @return Objekt Arenas, pokud je lokace uvnitř nějaké arény, jinak null.
	 */
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