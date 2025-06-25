package com.vypnito.arena.gui;

import com.vypnito.arena.arenas.Arenas;
import com.vypnito.arena.arenas.ArenaSettings;
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

public class EditGUI {
	public static final String GUI_TITLE_PREFIX = "Editing Arena: ";

	public void open(Player player, Arenas arena) {
		Inventory gui = Bukkit.createInventory(null, 54, Component.text(GUI_TITLE_PREFIX + arena.getName()));
		ArenaSettings settings = arena.getSettings();

		gui.setItem(10, createGuiItem(Material.REDSTONE_TORCH, "§eSet Position 1", "§7Current: " + formatLoc(arena.getPos1()), "§aClick to re-select."));
		gui.setItem(11, createGuiItem(Material.REDSTONE_TORCH, "§eSet Position 2", "§7Current: " + formatLoc(arena.getPos2()), "§aClick to re-select."));

		gui.setItem(19, createToggleItem(settings.isAllowBlockBreak(), "Block Breaking"));
		gui.setItem(20, createToggleItem(settings.isAllowBlockPlace(), "Block Placing"));
		gui.setItem(21, createToggleItem(settings.isAllowItemDrop(), "Item Dropping"));
		gui.setItem(22, createToggleItem(settings.isDisableHunger(), "Hunger Drain"));

		gui.setItem(28, createGuiItem(Material.DIAMOND_BLOCK, "§bSet Wall Material", "§7Current: §e" + settings.getWallMaterial().name(), "§aClick to set in chat."));
		gui.setItem(29, createGuiItem(Material.CLOCK, "§bSet Death Delay", "§7Current: §e" + settings.getWallRemovalDelay() + "s", "§aClick to set in chat."));

		gui.setItem(37, createGuiItem(Material.POTION, "§dEdit Effects", "§7Current: §e" + settings.getEffects().size() + " effects", "§aClick to manage."));

		player.openInventory(gui);
	}

	private String formatLoc(Location loc) {
		if (loc == null) return "§cNot Set";
		return "§a" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
	}

	private ItemStack createToggleItem(boolean enabled, String name) {
		Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
		String status = enabled ? "§aENABLED" : "§cDISABLED";
		return createGuiItem(material, "§f" + name, "§7Status: " + status, "§eClick to toggle.");
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