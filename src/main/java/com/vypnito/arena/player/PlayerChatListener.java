package com.vypnito.arena.player;

import com.vypnito.arena.arena;
import com.vypnito.arena.arenas.Arenas;
import com.vypnito.arena.arenas.ArenaManager;
import com.vypnito.arena.gui.GUIManager; // Using GUIManager for opening GUIs

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
import java.util.stream.Collectors;

/**
 * This listener catches chat input from players who are in an "input state"
 * (e.g., after clicking an item in the GUI that requires them to type something).
 */
public class PlayerChatListener implements Listener {
	private final arena plugin;
	private final PlayerManager playerManager;
	private final ArenaManager arenaManager;
	private final GUIManager guiManager; // Using GUIManager for opening GUIs

	/**
	 * Constructor for PlayerChatListener.
	 * @param plugin The main plugin instance.
	 * @param playerManager Manages player's transient states.
	 * @param arenaManager Manages arena data.
	 * @param guiManager Manages all GUI interactions.
	 */
	public PlayerChatListener(arena plugin, PlayerManager playerManager, ArenaManager arenaManager, GUIManager guiManager) {
		this.plugin = plugin;
		this.playerManager = playerManager;
		this.arenaManager = arenaManager;
		this.guiManager = guiManager;
	}

	/**
	 * Handles chat input from players who are in a specific input state.
	 * @param event The AsyncPlayerChatEvent.
	 */
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		PlayerState state = playerManager.getPlayerState(player);
		if (state == null) return; // If the player is not in a special state, let the chat event pass

		event.setCancelled(true); // Cancel the chat event to prevent the message from appearing in public chat
		String message = event.getMessage();
		Arenas arena = state.getArena();

		// Allow players to cancel their current input state
		if (message.equalsIgnoreCase("cancel")) {
			playerManager.clearPlayerState(player);
			player.sendMessage(Component.text("Action cancelled.", NamedTextColor.RED));
			Bukkit.getScheduler().runTask(plugin, () -> guiManager.openEditGUI(player, arena));
			return;
		}

		// Handle the input based on the type of state the player is in
		if (state.getInputType() != null) {
			handleChatInput(player, arena, message, state.getInputType());
		} else if (state.getEffectType() != null) {
			handleAddEffect(player, arena, message, state.getEffectType());
		}
	}

	/**
	 * Handles general chat input for setting arena properties like wall material or delay.
	 * @param player The player providing input.
	 * @param arena The arena being configured.
	 * @param message The chat message input by the player.
	 * @param type The type of input expected.
	 */
	private void handleChatInput(Player player, Arenas arena, String message, PlayerState.InputType type) {
		switch(type) {
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
		arenaManager.saveArena(arena);
		playerManager.clearPlayerState(player);
		Bukkit.getScheduler().runTask(plugin, () -> guiManager.openEditGUI(player, arena));
	}

	/**
	 * Handles chat input specifically for setting the amplifier of a potion effect.
	 * @param player The player providing input.
	 * @param arena The arena being configured.
	 * @param message The chat message (amplifier value) input by the player.
	 * @param effectType The PotionEffectType that the amplifier is for.
	 */
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

	/**
	 * Formats a raw potion effect name (e.g., "FAST_DIGGING") into a more readable string (e.g., "Fast Digging").
	 * @param name The raw potion effect name.
	 * @return The formatted name.
	 */
	private String formatEffectName(String name) {
		return Arrays.stream(name.split("_"))
				.map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}
}