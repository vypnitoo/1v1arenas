package com.vypnito.arena.player;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the transient state of players, such as what they are currently editing in a GUI.
 */
public class PlayerManager {
	private final Map<UUID, PlayerState> playerStates = new HashMap<>();

	/**
	 * Sets a specific state for a player, usually indicating they are waiting for input.
	 * @param player The player to set the state for.
	 * @param state The state to set.
	 */
	public void setPlayerState(Player player, PlayerState state) {
		playerStates.put(player.getUniqueId(), state);
	}

	/**
	 * Gets the current state of a player.
	 * @param player The player to get the state of.
	 * @return The PlayerState, or null if the player has no special state.
	 */
	public PlayerState getPlayerState(Player player) {
		return playerStates.get(player.getUniqueId());
	}

	/**
	 * Clears the state of a player, usually after they have provided input or cancelled an action.
	 * @param player The player whose state to clear.
	 */
	public void clearPlayerState(Player player) {
		playerStates.remove(player.getUniqueId());
	}
}