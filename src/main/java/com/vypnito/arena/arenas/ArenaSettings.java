package com.vypnito.arena.arenas;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Settings for an individual arena (wall material, allowed actions, effects, etc.).
 */
public class ArenaSettings {
	private Material wallMaterial = Material.GLASS; // Default wall material
	private boolean allowBlockBreak = false;
	private boolean allowBlockPlace = false;
	private boolean allowItemDrop = false;
	private boolean disableHunger = true;
	private int wallRemovalDelay = 30;
	private List<String> effects = new ArrayList<>(); // Stored as String for easy saving/loading
	private int requiredPlayers = 2; // NEW: Default to 2 players for 1v1

	public ArenaSettings() {
		// Default constructor
	}

	public Material getWallMaterial() { return wallMaterial; }
	public void setWallMaterial(Material wallMaterial) { this.wallMaterial = wallMaterial; }

	public boolean isAllowBlockBreak() { return allowBlockBreak; }
	public void setAllowBlockBreak(boolean allowBlockBreak) { this.allowBlockBreak = allowBlockBreak; }

	public boolean isAllowBlockPlace() { return allowBlockPlace; }
	public void setAllowBlockPlace(boolean allowBlockPlace) { this.allowBlockPlace = allowBlockPlace; }

	public boolean isAllowItemDrop() { return allowItemDrop; }
	public void setAllowItemDrop(boolean allowItemDrop) { this.allowItemDrop = allowItemDrop; }

	public boolean isDisableHunger() { return disableHunger; }
	public void setDisableHunger(boolean disableHunger) { this.disableHunger = disableHunger; }

	public int getWallRemovalDelay() { return wallRemovalDelay; }
	public void setWallRemovalDelay(int wallRemovalDelay) { this.wallRemovalDelay = wallRemovalDelay; }

	public List<String> getEffects() { return effects; }
	public void addEffect(String effectString) {
		String newEffectType = effectString.split(":")[0];
		boolean alreadyHas = effects.stream().anyMatch(e -> e.startsWith(newEffectType + ":"));
		if (!alreadyHas) {
			effects.add(effectString);
		} else {
			effects = effects.stream()
					.map(e -> e.startsWith(newEffectType + ":") ? effectString : e)
					.collect(Collectors.toList());
		}
	}
	public void removeEffect(PotionEffectType type) {
		effects.removeIf(s -> s.startsWith(type.getName() + ":"));
	}
	public boolean hasEffect(PotionEffectType type) {
		return effects.stream().anyMatch(s -> s.startsWith(type.getName() + ":"));
	}

	// NEW: Getter and Setter for requiredPlayers
	public int getRequiredPlayers() { return requiredPlayers; }
	public void setRequiredPlayers(int requiredPlayers) { this.requiredPlayers = requiredPlayers; }
}