package com.vypnito.arena.player;

import com.vypnito.arena.arena;
import com.vypnito.arena.arenas.Arenas;
import com.vypnito.arena.arenas.ArenaManager;
import com.vypnito.arena.gui.GUIManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This listener catches chat input from players who are in an "input state"
 * (e.g., after clicking an item in the GUI that requires them to type something).
 */
public class PlayerChatListener implements Listener {
	private final arena plugin;
	private final PlayerManager playerManager;
	private final ArenaManager arenaManager;
	private final GUIManager guiManager;

	private static final Pattern X_V_X_PATTERN = Pattern.compile("(\\d+)v(\\d+)", Pattern.CASE_INSENSITIVE);

	public PlayerChatListener(arena plugin, PlayerManager playerManager, ArenaManager arenaManager, GUIManager guiManager) {
		this.plugin = plugin;
		this.playerManager = playerManager;
		this.arenaManager = arenaManager;
		this.guiManager = guiManager;
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		PlayerState state = playerManager.getPlayerState(player);
		if (state == null) return;

		event.setCancelled(true);
		String message = event.getMessage();
		// Arena object might be null if we are in the arena creation flow
		Arenas arena = state.getArena();

		if (message.equalsIgnoreCase("cancel")) {
			playerManager.clearPlayerState(player);
			if (state.getArenaNameForCreation() != null) {
				// If cancelling during creation, go back to type selection GUI
				Bukkit.getScheduler().runTask(plugin, () -> guiManager.openArenaTypeSelectionGUI(player, state.getArenaNameForCreation()));
			} else if (arena != null) {
				// If cancelling during editing, go back to edit GUI
				Bukkit.getScheduler().runTask(plugin, () -> guiManager.openEditGUI(player, arena));
			} else {
				player.sendMessage(Component.text("Action cancelled, but unable to return to previous GUI.", NamedTextColor.RED));
			}
			player.sendMessage(Component.text("Action cancelled.", NamedTextColor.RED));
			return;
		}

		if (state.getInputType() != null) {
			handleChatInput(player, arena, message, state);
		} else if (state.getEffectType() != null) {
			handleAddEffect(player, arena, message, state.getEffectType());
		}
	}

	/**
	 * Handles general chat input for setting arena properties like wall material or delay.
	 * Now also handles custom arena type input.
	 * @param player The player providing input.
	 * @param arena The arena being configured (can be null for creation flow).
	 * @param message The chat message input by the player.
	 * @param state The current PlayerState, contains input type and potential arena creation data.
	 */
	private void handleChatInput(Player player, Arenas arena, String message, PlayerState state) {
		// --- KLÍČOVÁ ZMĚNA ZDE ---
		// Logika pro CUSTOM_REQUIRED_PLAYERS se nyní řeší odděleně a nevolá saveArena na konci.
		if (state.getInputType() == PlayerState.InputType.CUSTOM_REQUIRED_PLAYERS) {
			handleCustomRequiredPlayersInput(player, message, state);
			return; // Důležité: Ukončíme metodu, aby se nevolala saveArena a openEditGUI
		}
		// --- KONEC KLÍČOVÉ ZMĚNY ---

		// Zbytek logiky handleChatInput pro WALL_MATERIAL a DELAY
		switch(state.getInputType()) {
			case WALL_MATERIAL:
				try {
					Material newMaterial = Material.valueOf(message.toUpperCase());
					arena.getSettings().setWallMaterial(newMaterial);
					player.sendMessage(Component.text("Wall material updated to " + newMaterial.name(), NamedTextColor.GREEN));
				} catch (IllegalArgumentException e) {
					player.sendMessage(Component.text("Invalid material name. Please try again.", NamedTextColor.RED));
					return;
				}
				break;
			case DELAY:
				try {
					int newDelay = Integer.parseInt(message);
					if (newDelay < 0) {
						player.sendMessage(Component.text("Delay cannot be negative. Please enter a positive number.", NamedTextColor.RED));
						return;
					} else {
						arena.getSettings().setWallRemovalDelay(newDelay);
						player.sendMessage(Component.text("Wall removal delay updated to " + newDelay + "s.", NamedTextColor.GREEN));
					}
				} catch (NumberFormatException e) {
					player.sendMessage(Component.text("That is not a valid number. Please enter a number.", NamedTextColor.RED));
					return;
				}
				break;
		}
		// Tyto řádky se spustí POUZE pro WALL_MATERIAL a DELAY (kdy je 'arena' vždy platná)
		arenaManager.saveArena(arena);
		playerManager.clearPlayerState(player);
		Bukkit.getScheduler().runTask(plugin, () -> guiManager.openEditGUI(player, arena));
	}

	/**
	 * NEW: Handles input specifically for custom arena player counts (XvX format).
	 * @param player The player providing input.
	 * @param message The chat message (e.g., "5v5").
	 * @param state The current PlayerState for arena creation.
	 */
	private void handleCustomRequiredPlayersInput(Player player, String message, PlayerState state) {
		Matcher matcher = X_V_X_PATTERN.matcher(message);
		if (matcher.matches()) {
			try {
				int players1 = Integer.parseInt(matcher.group(1));
				int players2 = Integer.parseInt(matcher.group(2));
				int totalPlayers = players1 + players2;

				if (players1 <= 0 || players2 <= 0) {
					player.sendMessage(Component.text("Player counts must be positive. Example: '5v5'.", NamedTextColor.RED));
					return;
				}
				if (totalPlayers > 64) { // Max 32v32 or reasonable limit
					player.sendMessage(Component.text("Total players (" + totalPlayers + ") is too high. Max 32v32 (64 total).", NamedTextColor.RED));
					return;
				}

				state.setSelectedRequiredPlayers(totalPlayers); // Store total players
				// We DON'T clear the player state completely here, just reset input type
				playerManager.clearPlayerState(player); // Clear current input state
				playerManager.setPlayerState(player, state); // Re-set player state with updated requiredPlayers and arena creation flow info

				player.sendMessage(Component.text("Arena type set to " + players1 + "v" + players2 + " (" + totalPlayers + " players).", NamedTextColor.GREEN));

				// Proceed to the next step of arena creation (Setup GUI) on the main thread
				Bukkit.getScheduler().runTask(plugin, () -> guiManager.openSetupGUI(player, state.getArenaNameForCreation()));
			} catch (NumberFormatException e) {
				player.sendMessage(Component.text("Invalid numbers in XvX format. Example: '5v5'.", NamedTextColor.RED));
			}
		} else {
			player.sendMessage(Component.text("Invalid format. Please use 'XvX' (e.g., '5v5').", NamedTextColor.RED));
		}
	}


	private void handleAddEffect(Player player, Arenas arena, String message, PotionEffectType effectType) {
		try {
			int amplifier = Integer.parseInt(message);
			if (amplifier < 0 || amplifier > 255) {
				player.sendMessage(Component.text("Amplifier must be between 0 and 255. Please try again.", NamedTextColor.RED));
				return;
			}

			String effectString = effectType.getName() + ":" + amplifier;
			arena.getSettings().addEffect(effectString);
			arenaManager.saveArena(arena);

			player.sendMessage(Component.text("Effect '" + formatEffectName(effectType.getName()) + " " + (amplifier + 1) + "' added!", NamedTextColor.GREEN));
			playerManager.clearPlayerState(player);

			Bukkit.getScheduler().runTask(plugin, () -> guiManager.openEffectsGUI(player, arena, 0));
		} catch (NumberFormatException e) {
			player.sendMessage(Component.text("That is not a valid number. Please enter a number.", NamedTextColor.RED));
		}
	}

	private String formatEffectName(String name) {
		return Arrays.stream(name.split("_"))
				.map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}
}