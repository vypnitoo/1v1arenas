package com.vypnito.vyp1v1.game;

import com.vypnito.vyp1v1.Vyp1v1;
import com.vypnito.vyp1v1.arena.Arena;
import com.vypnito.vyp1v1.arena.ArenaManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class GameManager {
	private final Vyp1v1 plugin;
	private final ArenaManager arenaManager;

	public GameManager(Vyp1v1 plugin, ArenaManager arenaManager) {
		this.plugin = plugin;
		this.arenaManager = arenaManager;
	}

	public ArenaManager getArenaManager() {
		return arenaManager;
	}

	public void checkWallCreation(Arena arena) {
		if (arena == null) return;

		if (arena.getPlayers().size() >= 2) {
			if (!arena.isWallActive()) {
				arena.createBoundaryWall();
				arena.getPlayers().forEach(uuid -> {
					Player p = Bukkit.getPlayer(uuid);
					if (p != null) {
						p.sendMessage(Component.text("The arena has been sealed!", NamedTextColor.YELLOW));
					}
				});
			}
		}
	}

	public void removeWallInstantly(Arena arena) {
		if (arena != null && arena.isWallActive()) {
			arena.removeBoundaryWall();
		}
	}

	public void removeWallWithDelay(Arena arena) {
		if (arena != null && arena.isWallActive()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					if (arena.getPlayers().size() < 2) {
						arena.removeBoundaryWall();
					}
				}
			}.runTaskLater(plugin, arena.getSettings().getWallRemovalDelay() * 20L);
		}
	}

	public void applyArenaEffects(Player player, Arena arena) {
		// --- THIS IS THE FIX ---
		// First, clear all existing potion effects to ensure a clean slate.
		clearAllEffects(player);

		// Then, apply the new effects from the config.
		for (String effectString : arena.getSettings().getEffects()) {
			try {
				String[] parts = effectString.split(":");
				PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
				int amplifier = Integer.parseInt(parts[1]);

				if (type != null) {
					// Apply effect with a very long duration
					player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, false, false));
				} else {
					plugin.getLogger().warning("Invalid effect type in config: " + parts[0]);
				}
			} catch (Exception e) {
				plugin.getLogger().severe("Error parsing effect '" + effectString + "': " + e.getMessage());
			}
		}
	}

	public void clearAllEffects(Player player) {
		for (PotionEffect effect : player.getActivePotionEffects()) {
			player.removePotionEffect(effect.getType());
		}
	}

	public void sendDelayedWallMessage(Player player, Arena arena) {
		int delay = arena.getSettings().getWallRemovalDelay();
		player.sendMessage(Component.text("The arena wall will be removed in " + delay + " seconds.", NamedTextColor.GRAY));

		new BukkitRunnable() {
			private int ticks = 0;
			@Override
			public void run() {
				if (ticks >= 100 || !player.isOnline()) { // 5 seconds * 20 ticks
					this.cancel();
					return;
				}
				player.sendActionBar(Component.text("Arena opens in " + delay + " seconds", NamedTextColor.AQUA));
				ticks++;
			}
		}.runTaskTimer(plugin, 0L, 1L);
	}
}