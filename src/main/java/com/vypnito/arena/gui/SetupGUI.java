package com.vypnito.arena.gui;

import com.vypnito.arena.player.SelectionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SetupGUI {
	private final SelectionManager selectionManager;
	public static final String GUI_TITLE_PREFIX = "Arena Creation: ";

	public SetupGUI(SelectionManager selectionManager) {
		this.selectionManager = selectionManager;
	}

	public void open(Player player, String arenaName) {
		Inventory gui = Bukkit.createInventory(null, 27, Component.text(GUI_TITLE_PREFIX + arenaName));
		Location pos1 = selectionManager.getPos1(player);
		Location pos2 = selectionManager.getPos2(player);

		gui.setItem(11, createGuiItem(Material.REDSTONE_TORCH, "Position 1", pos1 == null ? "§cNot Set" : "§aSet: " + formatLoc(pos1)));
		gui.setItem(15, createGuiItem(Material.REDSTONE_TORCH, "Position 2", pos2 == null ? "§cNot Set" : "§aSet: " + formatLoc(pos2)));

		gui.setItem(22, createGuiItem(Material.EMERALD_BLOCK, "§aCreate Arena", "Click to create with current selection."));
		gui.setItem(26, createGuiItem(Material.CLOCK, "§eRefresh", "Update status from wand selection."));
		player.openInventory(gui);
	}

	private String formatLoc(Location loc) {
		if (loc == null) return "Not Set";
		return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
	}

	private ItemStack createGuiItem(Material material, String name, String... loreLines) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
		List<Component> lore = new ArrayList<>();
		for (String line : loreLines) {
			lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
		}
		meta.lore(lore);
		item.setItemMeta(meta);
		return item;
	}
}