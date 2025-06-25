package com.vypnito.arena.arenas;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Nastavení pro jednotlivou arénu (materiál stěn, povolené akce, efekty atd.).
 */
public class ArenaSettings {
	private Material wallMaterial = Material.GLASS; // Výchozí materiál
	private boolean allowBlockBreak = false;
	private boolean allowBlockPlace = false;
	private boolean allowItemDrop = false;
	private boolean disableHunger = true;
	private int wallRemovalDelay = 30;
	private List<String> effects = new ArrayList<>(); // Ukládáme jako String pro snadné ukládání/načítání

	// --- KLÍČOVÁ ZMĚNA: Veřejný konstruktor bez argumentů ---
	public ArenaSettings() {
		// Zde se inicializují výchozí hodnoty, pokud je potřeba.
		// Všechny privátní proměnné jsou již inicializovány nahoře.
	}
	// --- KONEC KLÍČOVÉ ZMĚNY ---

	// Pokud jsi měl dříve jiný konstruktor, například s ConfigurationSection,
	// ten by zůstal jako přetížení konstruktoru:
    /*
    public ArenaSettings(ConfigurationSection section) {
        this(); // Volá výchozí konstruktor pro inicializaci
        // Zde bys načítal nastavení ze section
        this.wallMaterial = Material.valueOf(section.getString("wall-material", "GLASS"));
        // ... a tak dále pro všechny ostatní proměnné
    }
    */

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
		// Kontrola duplicity, aby se stejný efekt nepřidal vícekrát
		// Porovnávám jen typ efektu, ne celou string reprezentaci s amplifierem
		String newEffectType = effectString.split(":")[0];
		boolean alreadyHas = effects.stream().anyMatch(e -> e.startsWith(newEffectType + ":"));
		if (!alreadyHas) {
			effects.add(effectString);
		} else {
			// Pokud už efekt existuje, aktualizovat ho namísto přidávání duplikátu
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

}