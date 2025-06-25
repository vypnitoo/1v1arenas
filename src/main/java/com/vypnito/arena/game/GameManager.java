package com.vypnito.arena.game;

import com.vypnito.arena.arena;
import com.vypnito.arena.arenas.Arenas;
import com.vypnito.arena.arenas.ArenaManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GameManager manages the game logic within arenas, such as effects,
 * wall manipulation, and player states within the arena.
 */
public class GameManager {
	private final arena plugin;
	private final ArenaManager arenaManager;
	private final Map<UUID, PlayerEffectSnapshot> savedPlayerEffects = new HashMap<>(); // Ukládá efekty hráčů při vstupu do arény

	// Record for saving a player's effect state
	private record PlayerEffectSnapshot(long timestamp, Collection<PotionEffect> effects) {}

	/**
	 * Constructor for GameManager.
	 * @param plugin The main plugin instance.
	 * @param arenaManager The ArenaManager instance.
	 */
	public GameManager(arena plugin, ArenaManager arenaManager) {
		this.plugin = plugin;
		this.arenaManager = arenaManager;
	}

	/**
	 * Gets the ArenaManager instance.
	 * @return ArenaManager.
	 */
	public ArenaManager getArenaManager() {
		return arenaManager;
	}

	/**
	 * Called when a player enters an arena.
	 * Saves the player's current effects and applies arena-specific effects.
	 * @param player The player who entered.
	 * @param arena The arena the player entered.
	 */
	public void onPlayerEnterArena(Player player, Arenas arena) {
		Collection<PotionEffect> currentEffects = new ArrayList<>(player.getActivePotionEffects());
		savedPlayerEffects.put(player.getUniqueId(), new PlayerEffectSnapshot(System.currentTimeMillis(), currentEffects));
		applyArenaEffects(player, arena);
		checkWallCreation(arena); // Check if the wall should be created (if 2+ players are present)
	}

	/**
	 * Called when a player leaves an arena.
	 * Restores the player's original effects.
	 * @param player The player who left.
	 * @param arena The arena the player left.
	 */
	public void onPlayerLeaveArena(Player player, Arenas arena) {
		restoreOriginalEffects(player); // Obnoví efekty hráče
		// If there are less than 2 players in the arena, instantly remove the wall
		if (arena.getPlayers().size() < 2) {
			removeWallInstantly(arena);
		}
	}

	/**
	 * Called when a player dies in an arena.
	 * Restores the player's original effects and starts a countdown for wall removal.
	 * @param player The player who died.
	 * @param arena The arena where the player died.
	 */
	public void onPlayerDieInArena(Player player, Arenas arena) {
		restoreOriginalEffects(player); // Obnoví efekty hráče
		// If there are less than 2 players in the arena, send a message and schedule delayed wall removal
		if (arena.getPlayers().size() < 2) {
			sendDelayedWallMessage(player, arena);
			removeWallWithDelay(arena);
		}
	}

	/**
	 * Checks if the arena wall should be created.
	 * The wall is created if there are at least 2 players in the arena and the wall is not already active.
	 * @param arena The arena to check.
	 */
	public void checkWallCreation(Arenas arena) {
		if (arena == null) return;
		// Wall is created ONLY if there are 2 or more players AND it's not already active.
		if (arena.getPlayers().size() >= 2 && !arena.isWallActive()) {
			arena.createBoundaryWall(); // Create the arena wall
			arena.getPlayers().forEach(uuid -> { // Notify all players in the arena
				Player p = Bukkit.getPlayer(uuid);
				if (p != null) {
					p.sendMessage(Component.text("The arena has been sealed!", NamedTextColor.YELLOW));
				}
			});
		}
	}

	/**
	 * Instantly removes the arena wall if it's active.
	 * @param arena The arena whose wall should be removed.
	 */
	public void removeWallInstantly(Arenas arena) {
		if (arena != null && arena.isWallActive()) {
			arena.removeBoundaryWall();
		}
	}

	/**
	 * Removes the arena wall with a defined delay.
	 * @param arena The arena whose wall should be removed.
	 */
	public void removeWallWithDelay(Arenas arena) {
		if (arena != null && arena.isWallActive()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					// Check if there are still less than 2 players in the arena before removing the wall
					if (arena.getPlayers().size() < 2) {
						arena.removeBoundaryWall();
						arena.getPlayers().forEach(uuid -> { // Notify remaining players
							Player p = Bukkit.getPlayer(uuid);
							if (p != null) {
								p.sendMessage(Component.text("The arena wall has been removed.", NamedTextColor.GRAY));
							}
						});
					}
				}
			}.runTaskLater(plugin, arena.getSettings().getWallRemovalDelay() * 20L); // 20 ticks = 1 second
		}
	}

	/**
	 * Applies effects defined in the arena settings to the player.
	 * All current player effects are removed before applying arena effects.
	 * @param player The player to apply effects to.
	 * @param arena The arena from which to get effect settings.
	 */
	private void applyArenaEffects(Player player, Arenas arena) {
		// First, remove all active effects from the player
		for (PotionEffectType type : player.getActivePotionEffects().stream().map(PotionEffect::getType).collect(Collectors.toList())) {
			player.removePotionEffect(type);
		}

		// Apply effects defined in arena settings
		for (String effectString : arena.getSettings().getEffects()) {
			try {
				String[] parts = effectString.split(":");
				String effectName = parts[0].toUpperCase().trim();
				// Mapping old effect names to new ones (for backward compatibility if needed)
				effectName = switch (effectName) {
					case "STRENGTH" -> "INCREASE_DAMAGE";
					case "HASTE" -> "FAST_DIGGING";
					case "MINING_FATIGUE" -> "SLOW_DIGGING";
					case "SLOWNESS" -> "SLOW";
					case "NAUSEA" -> "CONFUSION";
					default -> effectName;
				};
				PotionEffectType type = PotionEffectType.getByName(effectName);
				int amplifier = Integer.parseInt(parts[1].trim()); // Amplifier is typically 1 less than level (level 1 = amplifier 0)
				if (type != null) {
					// Apply the effect for an infinite duration (Integer.MAX_VALUE) with the given amplifier
					player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, false, false));
				}
			} catch (Exception e) {
				plugin.getLogger().warning("Error parsing effect '" + effectString + "'. Check format (e.g., 'SPEED:1').");
			}
		}
	}

	/**
	 * Restores the player's original effects that they had before entering the arena.
	 * @param player The player whose effects should be restored.
	 */
	private void restoreOriginalEffects(Player player) {
		// First, remove all current effects from the player (including arena effects)
		for (PotionEffectType type : player.getActivePotionEffects().stream().map(PotionEffect::getType).collect(Collectors.toList())) {
			player.removePotionEffect(type);
		}
		// Get the saved effect snapshot
		PlayerEffectSnapshot snapshot = savedPlayerEffects.remove(player.getUniqueId());
		if (snapshot == null) return; // No snapshot exists, nothing to restore

		// Calculate elapsed ticks since the snapshot was taken
		long elapsedTicks = (System.currentTimeMillis() - snapshot.timestamp()) / 50; // 50 ms = 1 tick

		// Restore each saved effect
		for (PotionEffect savedEffect : snapshot.effects()) {
			// Calculate remaining duration of the effect
			int newDuration = savedEffect.getDuration() - (int) elapsedTicks;
			if (newDuration > 0) {
				// Reapply the effect with the adjusted duration
				player.addPotionEffect(new PotionEffect(savedEffect.getType(), newDuration, savedEffect.getAmplifier(), savedEffect.isAmbient(), savedEffect.hasParticles()));
			}
		}
	}

	/**
	 * Sends a message to the player's action bar about the delayed wall removal.
	 * @param player The player to send the message to.
	 * @param arena The arena related to the message.
	 */
	public void sendDelayedWallMessage(Player player, Arenas arena) {
		int delay = arena.getSettings().getWallRemovalDelay(); // Delay in seconds
		player.sendMessage(Component.text("The arena wall will be removed in " + delay + " seconds.", NamedTextColor.GRAY));

		// BukkitRunnable for action bar updates
		new BukkitRunnable() {
			private int ticks = 0; // Tick counter
			private final int totalTicks = delay * 20; // Total ticks for the delay

			@Override
			public void run() {
				// Cancel the task if time has elapsed or the player is no longer online
				if (ticks >= totalTicks || !player.isOnline()) {
					this.cancel();
					player.sendActionBar(Component.empty()); // Clear action bar
					return;
				}
				// Calculate remaining seconds and display them in the action bar
				int remainingSeconds = (totalTicks - ticks) / 20;
				if (remainingSeconds > 0) { // Only display if at least 1 second remains
					player.sendActionBar(Component.text("Arena opens in " + remainingSeconds + " seconds", NamedTextColor.AQUA));
				}
				ticks++;
			}
		}.runTaskTimer(plugin, 0L, 1L); // Start task immediately and repeat every tick
	}
}