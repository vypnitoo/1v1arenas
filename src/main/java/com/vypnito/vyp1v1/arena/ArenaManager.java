package com.vypnito.vyp1v1.arena;

import com.vypnito.vyp1v1.Vyp1v1;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ArenaManager {
	private final Vyp1v1 plugin;
	private final Map<String, Arena> arenas = new HashMap<>();

	public ArenaManager(Vyp1v1 plugin) {
		this.plugin = plugin;
		loadArenas();
	}

	public void loadArenas() {
		arenas.clear();
		ConfigurationSection arenasSection = plugin.getConfig().getConfigurationSection("arenas");
		if (arenasSection != null) {
			for (String arenaName : arenasSection.getKeys(false)) {
				String path = "arenas." + arenaName;
				Location pos1 = plugin.getConfig().getLocation(path + ".pos1");
				Location pos2 = plugin.getConfig().getLocation(path + ".pos2");
				ConfigurationSection settingsSection = plugin.getConfig().getConfigurationSection(path + ".settings");

				if (pos1 != null && pos2 != null) {
					ArenaSettings settings = new ArenaSettings(settingsSection);
					arenas.put(arenaName, new Arena(arenaName, pos1, pos2, settings));
				}
			}
			plugin.getLogger().info("Loaded " + arenas.size() + " arenas.");
		}
	}

	public Set<String> getArenaNames() {
		ConfigurationSection arenasSection = plugin.getConfig().getConfigurationSection("arenas");
		return (arenasSection == null) ? Set.of() : arenasSection.getKeys(false);
	}

	public void createArena(String name, Location pos1, Location pos2) {
		String path = "arenas." + name;
		plugin.getConfig().set(path + ".pos1", pos1);
		plugin.getConfig().set(path + ".pos2", pos2);

		ConfigurationSection defaultSettings = plugin.getConfig().getConfigurationSection("default-arena-settings");
		if (defaultSettings != null) {
			plugin.getConfig().set(path + ".settings", defaultSettings.getValues(true));
		}

		plugin.saveConfig();
		loadArenas();
	}

	public void deleteArena(String name) {
		plugin.getConfig().set("arenas." + name, null);
		plugin.saveConfig();
		arenas.remove(name);
	}

	public Arena findArenaByPlayer(Player player) {
		return arenas.values().stream().filter(a -> a.getPlayers().contains(player.getUniqueId())).findFirst().orElse(null);
	}

	public Arena findArenaByRegion(Location location) {
		return arenas.values().stream().filter(a -> a.isWithinRegion(location)).findFirst().orElse(null);
	}
}