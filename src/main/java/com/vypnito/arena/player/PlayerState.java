package com.vypnito.arena.player;

import com.vypnito.arena.arenas.Arenas;
import org.bukkit.potion.PotionEffectType;

public class PlayerState {
	private final Arenas arena;
	private final SelectionType selectionType;
	private final InputType inputType;
	private final PotionEffectType effectType;
	private Integer selectedRequiredPlayers;
	private String arenaNameForCreation;

	public PlayerState(Arenas arena, SelectionType type) {
		this.arena = arena;
		this.selectionType = type;
		this.inputType = null;
		this.effectType = null;
		this.selectedRequiredPlayers = null;
		this.arenaNameForCreation = null;
	}

	public PlayerState(Arenas arena, InputType type) {
		this.arena = arena;
		this.selectionType = null;
		this.inputType = type;
		this.effectType = null;
		this.selectedRequiredPlayers = null;
		this.arenaNameForCreation = null;
	}

	public PlayerState(Arenas arena, PotionEffectType type) {
		this.arena = arena;
		this.selectionType = null;
		this.inputType = null;
		this.effectType = type;
		this.selectedRequiredPlayers = null;
		this.arenaNameForCreation = null;
	}

	public PlayerState(String arenaNameForCreation, InputType inputType) {
		this.arena = null;
		this.selectionType = null;
		this.inputType = inputType;
		this.effectType = null;
		this.arenaNameForCreation = arenaNameForCreation;
		this.selectedRequiredPlayers = null;
	}

	public Arenas getArena() { return arena; }
	public SelectionType getSelectionType() { return selectionType; }
	public InputType getInputType() { return inputType; }
	public PotionEffectType getEffectType() { return effectType; }

	public Integer getSelectedRequiredPlayers() { return selectedRequiredPlayers; }
	public void setSelectedRequiredPlayers(Integer selectedRequiredPlayers) { this.selectedRequiredPlayers = selectedRequiredPlayers; }
	public String getArenaNameForCreation() { return arenaNameForCreation; }
	public void setArenaNameForCreation(String arenaNameForCreation) { this.arenaNameForCreation = arenaNameForCreation; }

	public enum SelectionType {
		POS1, POS2
	}

	public enum InputType {
		WALL_MATERIAL, DELAY, CUSTOM_REQUIRED_PLAYERS
	}
}