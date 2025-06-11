package com.vypnito.vyp1v1;

import com.vypnito.vyp1v1.arena.ArenaManager;
import com.vypnito.vyp1v1.arena.SelectionManager;
import com.vypnito.vyp1v1.arena.WandListener;
import com.vypnito.vyp1v1.game.GameListener;
import com.vypnito.vyp1v1.game.GameManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Vyp1v1 extends JavaPlugin {
	@Override
	public void onEnable() {
		if (!new File(getDataFolder(), "config.yml").exists()) {
			saveDefaultConfig();
		}
		SelectionManager selectionManager = new SelectionManager();
		ArenaManager arenaManager = new ArenaManager(this);
		GameManager gameManager = new GameManager(this, arenaManager);

		// Register the new /arena command
		this.getCommand("arena").setExecutor(new ArenaCommand(this, arenaManager, selectionManager));
		this.getCommand("arena").setTabCompleter(new ArenaTabCompleter(arenaManager));

		getServer().getPluginManager().registerEvents(new WandListener(selectionManager), this);
		getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);
		getLogger().info("Vyp1v1 (Arena System) has been enabled!");
	}
}