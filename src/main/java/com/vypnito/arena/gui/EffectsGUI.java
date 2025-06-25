package com.vypnito.arena.gui;

import com.vypnito.arena.arenas.Arenas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EffectsGUI {
	public static final String GUI_TITLE_PREFIX = "Effects: ";
	private static final int ITEMS_PER_PAGE = 45;

	public void open(Player player, Arenas arena, int page) {
		List<PotionEffectType> allEffects = Arrays.stream(PotionEffectType.values()).filter(java.util.Objects::nonNull).sorted((t1, t2) -> t1.getName().compareTo(t2.getName())).collect(Collectors.toList());
		int maxPages = (int) Math.ceil((double) allEffects.size() / ITEMS_PER_PAGE);

		Inventory gui = Bukkit.createInventory(null, 54, Component.text(GUI_TITLE_PREFIX + arena.getName() + " (Page " + (page + 1) + ")"));

		int startIndex = page * ITEMS_PER_PAGE;
		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			if (startIndex + i >= allEffects.size()) break;
			PotionEffectType effectType = allEffects.get(startIndex + i);
			gui.setItem(i, createEffectItem(effectType, arena.getSettings().hasEffect(effectType)));
		}

		if (page > 0) gui.setItem(45, createNavItem("§aPrevious Page", Material.ARROW));
		if (page < maxPages - 1) gui.setItem(53, createNavItem("§aNext Page", Material.ARROW));
		gui.setItem(49, createNavItem("§cBack to Main Editor", Material.BARRIER));

		player.openInventory(gui);
	}

	private ItemStack createEffectItem(PotionEffectType type, boolean hasEffect) {
		String name = hasEffect ? "§a" + formatEffectName(type.getName()) : "§7" + formatEffectName(type.getName());
		List<String> lore = hasEffect ? List.of("§cClick to REMOVE.") : List.of("§eClick to ADD.");
		ItemStack item = createGuiItem(Material.POTION, name, lore.toArray(new String[0]));
		if (hasEffect) {
			ItemMeta meta = item.getItemMeta();
			meta.addEnchant(Enchantment.UNBREAKING, 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			item.setItemMeta(meta);
		}
		return item;
	}

	private String formatEffectName(String name) {
		return Arrays.stream(name.split("_"))
				.map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}

	private ItemStack createNavItem(String name, Material material) {
		return createGuiItem(material, name);
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