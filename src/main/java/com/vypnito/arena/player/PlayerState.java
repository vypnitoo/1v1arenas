package com.vypnito.arena.player;

import com.vypnito.arena.arenas.Arenas;
import org.bukkit.potion.PotionEffectType;

/**
 * A data class to hold information about what a player is currently doing.
 * This is used to handle multi-step actions like selecting a block or typing in chat.
 */
public class PlayerState {
	private final Arenas arena;
	private final SelectionType selectionType; // For block selection
	private final InputType inputType;         // For chat input
	private final PotionEffectType effectType; // For adding a specific effect

	// Constructor for selecting blocks (POS1, POS2)
	public PlayerState(Arenas arena, SelectionType type) {
		this.arena = arena;
		this.selectionType = type;
		this.inputType = null;
		this.effectType = null;
	}

	// Constructor for waiting for generic chat input (Material, Delay)
	public PlayerState(Arenas arena, InputType type) {
		this.arena = arena;
		this.selectionType = null;
		this.inputType = type;
		this.effectType = null;
	}

	// Constructor for adding a specific potion effect
	public PlayerState(Arenas arena, PotionEffectType type) {
		this.arena = arena;
		this.selectionType = null;
		this.inputType = null;
		this.effectType = type;
	}

	public Arenas getArena() { return arena; }
	public SelectionType getSelectionType() { return selectionType; }
	public InputType getInputType() { return inputType; }
	public PotionEffectType getEffectType() { return effectType; }

	// Enum to differentiate between selecting POS1 and POS2
	public enum SelectionType {
		POS1, POS2
	}

	// Enum to differentiate between different types of chat input needed
	public enum InputType {
		WALL_MATERIAL, DELAY, ADD_EFFECT
	}
}