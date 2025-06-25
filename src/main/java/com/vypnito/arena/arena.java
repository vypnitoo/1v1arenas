package com.vypnito.arena;

import com.vypnito.arena.arenas.ArenaManager;
import com.vypnito.arena.arenas.WandListener;
import com.vypnito.arena.game.GameListener;
import com.vypnito.arena.game.GameManager;
import com.vypnito.arena.gui.GUIManager;
import com.vypnito.arena.player.PlayerChatListener;
import com.vypnito.arena.player.PlayerManager;
import com.vypnito.arena.player.SelectionManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class arena extends JavaPlugin {

	@Override
	public void onEnable() {
		saveDefaultConfig();

		PlayerManager playerManager = new PlayerManager();
		SelectionManager selectionManager = new SelectionManager();
		ArenaManager arenaManager = new ArenaManager(this);
		GameManager gameManager = new GameManager(this, arenaManager);
		GUIManager guiManager = new GUIManager(this, selectionManager, arenaManager, playerManager);

		this.getCommand("arena").setExecutor(new ArenaCommand(arenaManager, selectionManager, guiManager));
		this.getCommand("arena").setTabCompleter(new ArenaTabCompleter(arenaManager));

		getServer().getPluginManager().registerEvents(new WandListener(selectionManager, playerManager, arenaManager, guiManager), this);
		getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);
		getServer().getPluginManager().registerEvents(guiManager, this);
		getServer().getPluginManager().registerEvents(new PlayerChatListener(this, playerManager, arenaManager, guiManager), this);

		getLogger().info("1v1Arena has been enabled!");
	}
}