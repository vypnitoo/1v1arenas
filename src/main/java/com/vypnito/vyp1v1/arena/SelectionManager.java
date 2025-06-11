package com.vypnito.vyp1v1.arena;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {
	public static final Material WAND_MATERIAL = Material.WOODEN_AXE;
	private final Map<UUID, Location> pos1 = new HashMap<>();
	private final Map<UUID, Location> pos2 = new HashMap<>();

	public void setPos1(UUID uuid, Location location) { pos1.put(uuid, location); }
	public void setPos2(UUID uuid, Location location) { pos2.put(uuid, location); }
	public Location getPos1(UUID uuid) { return pos1.get(uuid); }
	public Location getPos2(UUID uuid) { return pos2.get(uuid); }
	public void clearSelection(UUID uuid) {
		pos1.remove(uuid);
		pos2.remove(uuid);
	}
	public void giveWand(Player player) {
		ItemStack wand = new ItemStack(WAND_MATERIAL);
		ItemMeta meta = wand.getItemMeta();
		meta.displayName(Component.text("Arena Selection Wand", NamedTextColor.AQUA));
		meta.lore(Arrays.asList(Component.text("Left-Click to set Position 1", NamedTextColor.GREEN), Component.text("Right-Click to set Position 2", NamedTextColor.GREEN)));
		wand.setItemMeta(meta);
		player.getInventory().addItem(wand);
	}
}