package com.vypnito.arena;

import com.vypnito.arena.arenas.ArenaManager;
// Import pro ArenaCommand a ArenaTabCompleter je již správný
import com.vypnito.arena.ArenaCommand;
import com.vypnito.arena.ArenaTabCompleter;
import com.vypnito.arena.gui.GUIManager;
import com.vypnito.arena.player.PlayerManager;
import com.vypnito.arena.player.PlayerChatListener;
import com.vypnito.arena.player.SelectionManager;
import com.vypnito.arena.game.GameListener;
import com.vypnito.arena.game.GameManager;
// Nový import pro WandListener
import com.vypnito.arena.player.WandListener;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;

/**
 * Main class of the SmartArenas plugin.
 * This class handles plugin enabling/disabling, manager initialization,
 * and event/command registration.
 */
public final class arena extends JavaPlugin {

	private ArenaManager arenaManager;
	private SelectionManager selectionManager;
	private PlayerManager playerManager;
	private GUIManager guiManager;
	private GameManager gameManager;

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		this.reloadConfig();

		arenaManager = new ArenaManager(this);
		// --- ZMĚNA ZDE: SelectionManager nyní potřebuje instanci pluginu pro NamespacedKey ---
		selectionManager = new SelectionManager(this);
		// --- KONEC ZMĚNY ---
		playerManager = new PlayerManager();
		guiManager = new GUIManager(this, selectionManager, arenaManager, playerManager);
		gameManager = new GameManager(this, arenaManager);

		// Registrace event listenerů
		Bukkit.getPluginManager().registerEvents(guiManager, this);
		Bukkit.getPluginManager().registerEvents(new PlayerChatListener(this, playerManager, arenaManager, guiManager), this);
		Bukkit.getPluginManager().registerEvents(new GameListener(gameManager), this);
		// --- NOVÁ REGISTRACE: Registrace WandListeneru ---
		Bukkit.getPluginManager().registerEvents(new WandListener(selectionManager), this);
		// --- KONEC NOVÉ REGISTRACE ---

		// Registrace příkazu
		this.getCommand("arena").setExecutor(new ArenaCommand(this, arenaManager, selectionManager, guiManager));
		this.getCommand("arena").setTabCompleter(new ArenaTabCompleter(arenaManager));

		getLogger().info("SmartArenas has been enabled!");
	}

	@Override
	public void onDisable() {
		getLogger().info("SmartArenas has been disabled!");
	}

	public ArenaManager getArenaManager() {
		return arenaManager;
	}

	public SelectionManager getSelectionManager() {
		return selectionManager;
	}

	public PlayerManager getPlayerManager() {
		return playerManager;
	}

	public GUIManager getGuiManager() {
		return guiManager;
	}

	public GameManager getGameManager() {
		return gameManager;
	}
}