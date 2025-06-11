package com.vypnito.vyp1v1.game;

import com.vypnito.vyp1v1.arena.Arena;
import com.vypnito.vyp1v1.arena.ArenaManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {
	private final GameManager gameManager;
	private final ArenaManager arenaManager;

	public GameListener(GameManager gameManager) {
		this.gameManager = gameManager;
		this.arenaManager = gameManager.getArenaManager();
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
			return;
		}

		Player player = event.getPlayer();
		Arena fromArena = arenaManager.findArenaByRegion(event.getFrom());
		Arena toArena = arenaManager.findArenaByRegion(event.getTo());

		if (fromArena != toArena) {
			// Player is leaving an arena
			if (fromArena != null) {
				fromArena.removePlayer(player);
				gameManager.clearAllEffects(player);
				// If a player leaves by walking out, remove the wall instantly
				if (fromArena.getPlayers().size() < 2) {
					gameManager.removeWallInstantly(fromArena);
				}
			}
			// Player is entering an arena
			if (toArena != null) {
				toArena.addPlayer(player);
				gameManager.applyArenaEffects(player, toArena);
				gameManager.checkWallCreation(toArena);
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		gameManager.clearAllEffects(player);

		// Find the arena the player was in by checking their last location
		Arena arena = arenaManager.findArenaByRegion(player.getLocation());
		if (arena != null) {
			arena.removePlayer(player);
			// If a player quits, remove the wall instantly
			if (arena.getPlayers().size() < 2) {
				gameManager.removeWallInstantly(arena);
			}
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		gameManager.clearAllEffects(player);

		// Find the arena the player died in
		Arena arena = arenaManager.findArenaByRegion(player.getLocation());
		if (arena != null) {
			arena.removePlayer(player);
			// If a player dies, remove the wall WITH A DELAY
			if (arena.getPlayers().size() < 2) {
				gameManager.sendDelayedWallMessage(player, arena);
				gameManager.removeWallWithDelay(arena);
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Arena blockArena = arenaManager.findArenaByRegion(event.getBlock().getLocation());
		if (blockArena != null && blockArena.isWallActive()) {
			event.setCancelled(true);
		} else {
			Arena playerArena = arenaManager.findArenaByRegion(event.getPlayer().getLocation());
			if (playerArena != null && !playerArena.getSettings().isAllowBlockBreak()) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Arena arena = arenaManager.findArenaByRegion(event.getBlock().getLocation());
		if (arena != null && !arena.getSettings().isAllowBlockPlace()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onItemDrop(PlayerDropItemEvent event) {
		Arena arena = arenaManager.findArenaByRegion(event.getPlayer().getLocation());
		if (arena != null && !arena.getSettings().isAllowItemDrop()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		Arena arena = arenaManager.findArenaByRegion(player.getLocation());
		if (arena != null && arena.getSettings().isDisableHunger()) {
			event.setCancelled(true);
		}
	}
}