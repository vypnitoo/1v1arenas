package com.vypnito.arena.player;

import com.vypnito.arena.arena; // Přidáno pro NamespacedKey
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey; // Přidáno pro NamespacedKey
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType; // Přidáno pro PersistentDataType

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the wand selections (pos1 and pos2) for creating new arenas.
 * Also handles giving the selection wand to players.
 */
public class SelectionManager {
	private final Map<UUID, Location> pos1 = new HashMap<>();
	private final Map<UUID, Location> pos2 = new HashMap<>();
	private final NamespacedKey wandKey; // Nový NamespacedKey pro spolehlivou identifikaci hůlky

	/**
	 * Konstruktor pro SelectionManager.
	 * @param plugin Hlavní instance pluginu (potřebná pro NamespacedKey).
	 */
	public SelectionManager(arena plugin) {
		this.wandKey = new NamespacedKey(plugin, "arena_selection_wand"); // Inicializace klíče
	}

	public void setPos1(Player player, Location location) {
		pos1.put(player.getUniqueId(), location);
	}

	public void setPos2(Player player, Location location) {
		pos2.put(player.getUniqueId(), location);
	}

	public Location getPos1(Player player) {
		return pos1.get(player.getUniqueId());
	}

	public Location getPos2(Player player) {
		return pos2.get(player.getUniqueId());
	}

	public void clearSelection(Player player) {
		pos1.remove(player.getUniqueId());
		pos2.remove(player.getUniqueId());
	}

	/**
	 * Dává hráči výběrovou hůlku (Wooden Axe).
	 * Hůlka je označena speciálním PersistentDataContainer, aby ji plugin mohl spolehlivě rozpoznat.
	 * @param player Hráč, kterému se má dát hůlka.
	 */
	public void giveWand(Player player) {
		ItemStack wand = new ItemStack(Material.WOODEN_AXE);
		ItemMeta meta = wand.getItemMeta();
		if (meta != null) {
			meta.displayName(Component.text("Arena Selection Wand", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
			meta.lore(Arrays.asList(
					Component.text("Left-Click to set Position 1", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
					Component.text("Right-Click to set Position 2", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
			));
			// Uložíme speciální data do PersistentDataContainer, aby bylo možné hůlku spolehlivě identifikovat
			meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BOOLEAN, true);
			wand.setItemMeta(meta);
		}
		player.getInventory().addItem(wand);
		player.sendMessage(Component.text("You have received the selection wand!", NamedTextColor.GREEN));
	}

	/**
	 * Získá NamespacedKey použitý pro identifikaci výběrové hůlky.
	 * @return NamespacedKey hůlky.
	 */
	public NamespacedKey getWandKey() {
		return wandKey;
	}
}