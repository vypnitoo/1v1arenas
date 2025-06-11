package com.vypnito.vyp1v1.arena;

import org.bukkit.configuration.ConfigurationSection;
import java.util.Collections;
import java.util.List;

public class ArenaSettings {

	private final boolean allowBlockBreak;
	private final boolean allowBlockPlace;
	private final boolean allowItemDrop;
	private final boolean disableHunger;
	private final int wallRemovalDelay;
	private final List<String> effects;

	public ArenaSettings(ConfigurationSection section) {
		if (section != null) {
			this.allowBlockBreak = section.getBoolean("allow-block-break", false);
			this.allowBlockPlace = section.getBoolean("allow-block-place", false);
			this.allowItemDrop = section.getBoolean("allow-item-drop", false);
			this.disableHunger = section.getBoolean("disable-hunger", true);
			this.wallRemovalDelay = section.getInt("wall-removal-delay-seconds", 30);
			this.effects = section.getStringList("effects");
		} else {
			// Default values if settings section is missing
			this.allowBlockBreak = false;
			this.allowBlockPlace = false;
			this.allowItemDrop = false;
			this.disableHunger = true;
			this.wallRemovalDelay = 30;
			this.effects = Collections.emptyList();
		}
	}

	// Getters
	public boolean isAllowBlockBreak() { return allowBlockBreak; }
	public boolean isAllowBlockPlace() { return allowBlockPlace; }
	public boolean isAllowItemDrop() { return allowItemDrop; }
	public boolean isDisableHunger() { return disableHunger; }
	public int getWallRemovalDelay() { return wallRemovalDelay; }
	public List<String> getEffects() { return effects; }
}