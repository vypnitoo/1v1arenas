package com.vypnito.arena.game;

import com.vypnito.arena.arenas.Arenas;
import com.vypnito.arena.arenas.ArenaManager;
import org.bukkit.Location; // Import pro Location
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * GameListener obsluhuje herní události související s interakcí hráčů v arénách.
 * Detekuje vstup/výstup z arén, smrt, položení/rozbití bloků, zahazování předmětů a změnu hladiny jídla.
 */
public class GameListener implements Listener {
	private final GameManager gameManager;
	private final ArenaManager arenaManager;

	/**
	 * Konstruktor pro GameListener.
	 * @param gameManager Instance GameManageru.
	 */
	public GameListener(GameManager gameManager) {
		this.gameManager = gameManager;
		this.arenaManager = gameManager.getArenaManager(); // Získá ArenaManager z GameManageru
	}

	/**
	 * Obsluhuje událost pohybu hráče. Ignoruje teleportaci a kontroluje, zda se hráč přesunul mezi arénami.
	 * @param event Událost pohybu hráče.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		// Ignorujeme PlayerTeleportEvent, protože ten je obsluhován samostatně
		if (event instanceof PlayerTeleportEvent) return;
		// Ignorujeme pohyb, pokud se hráč nepohnul do jiného bloku
		if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
			return;
		}
		handleMovement(event.getPlayer(), event.getFrom(), event.getTo());
	}

	/**
	 * Obsluhuje událost teleportace hráče. Kontroluje, zda se hráč přesunul mezi arénami.
	 * @param event Událost teleportace hráče.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		handleMovement(event.getPlayer(), event.getFrom(), event.getTo());
	}

	/**
	 * Společná metoda pro obsluhu pohybu a teleportace, detekující vstup/výstup z arény.
	 * @param player Hráč, který se pohnul.
	 * @param from Původní lokace hráče.
	 * @param to Nová lokace hráče.
	 */
	private void handleMovement(Player player, Location from, Location to) {
		Arenas fromArena = arenaManager.findArenaByRegion(from);
		Arenas toArena = arenaManager.findArenaByRegion(to);
		// Pokud se aréna, ve které se hráč nachází, změnila
		if (fromArena != toArena) {
			if (fromArena != null) {
				fromArena.removePlayer(player.getUniqueId()); // Používáme UUID pro odstranění
				gameManager.onPlayerLeaveArena(player, fromArena);
			}
			if (toArena != null) {
				toArena.addPlayer(player.getUniqueId()); // Používáme UUID pro přidání
				gameManager.onPlayerEnterArena(player, toArena);
			}
		}
	}

	/**
	 * Obsluhuje událost odchodu hráče ze serveru.
	 * @param event Událost odchodu hráče.
	 */
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Arenas arena = arenaManager.findArenaByRegion(player.getLocation());
		if (arena != null) {
			arena.removePlayer(player.getUniqueId()); // Používáme UUID pro odstranění
			gameManager.onPlayerLeaveArena(player, arena);
		}
	}

	/**
	 * Obsluhuje událost smrti hráče.
	 * @param event Událost smrti hráče.
	 */
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		Arenas arena = arenaManager.findArenaByRegion(player.getLocation());
		if (arena != null) {
			arena.removePlayer(player.getUniqueId()); // Používáme UUID pro odstranění
			gameManager.onPlayerDieInArena(player, arena);
		}
	}

	/**
	 * Obsluhuje událost rozbití bloku.
	 * Zabrání rozbíjení bloků ve zdi arény nebo v aréně, pokud to není povoleno.
	 * @param event Událost rozbití bloku.
	 */
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		// Kontrola, zda je blok součástí aktivní zdi arény
		// Předpokládáme, že isWallActive() správně funguje a zahrnuje i checkLocationInWall()
		Arenas blockArena = arenaManager.findArenaByRegion(event.getBlock().getLocation());
		if (blockArena != null && blockArena.isWallActive() && blockArena.isLocationInWall(event.getBlock().getLocation())) { // Přidáno isLocationInWall
			event.setCancelled(true);
		} else {
			// Kontrola, zda je hráč v aréně, kde je rozbíjení bloků zakázáno
			Arenas playerArena = arenaManager.findArenaByRegion(event.getPlayer().getLocation());
			if (playerArena != null && !playerArena.getSettings().isAllowBlockBreak()) {
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Obsluhuje událost položení bloku.
	 * Zabrání pokládání bloků v aréně, pokud to není povoleno.
	 * @param event Událost položení bloku.
	 */
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Arenas arena = arenaManager.findArenaByRegion(event.getBlock().getLocation());
		if (arena != null && !arena.getSettings().isAllowBlockPlace()) {
			event.setCancelled(true);
		}
	}

	/**
	 * Obsluhuje událost zahození předmětu.
	 * Zabrání zahazování předmětů v aréně, pokud to není povoleno.
	 * @param event Událost zahození předmětu.
	 */
	@EventHandler
	public void onItemDrop(PlayerDropItemEvent event) {
		Arenas arena = arenaManager.findArenaByRegion(event.getPlayer().getLocation());
		if (arena != null && !arena.getSettings().isAllowItemDrop()) {
			event.setCancelled(true);
		}
	}

	/**
	 * Obsluhuje událost změny hladiny jídla.
	 * Zabrání vyčerpání jídla v aréně, pokud je to nastaveno.
	 * @param event Událost změny hladiny jídla.
	 */
	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (!(event.getEntity() instanceof Player player)) return; // Použito pattern matching pro jednodušší syntaxi
		Arenas arena = arenaManager.findArenaByRegion(player.getLocation());
		if (arena != null && arena.getSettings().isDisableHunger()) {
			event.setCancelled(true);
		}
	}
}