package com.vypnito.vyp1v1.arena;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class WandListener implements Listener {
	private final SelectionManager selectionManager;

	public WandListener(SelectionManager selectionManager) {
		this.selectionManager = selectionManager;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (event.getItem() == null || event.getItem().getType() != SelectionManager.WAND_MATERIAL) return;
		if (event.getItem().getItemMeta() == null || !event.getItem().getItemMeta().hasDisplayName()) return;
		if (!player.hasPermission("1v1rooms.admin")) return;

		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			event.setCancelled(true);
			selectionManager.setPos1(player.getUniqueId(), event.getClickedBlock().getLocation());
			player.sendMessage(Component.text("Position 1 set!", NamedTextColor.GREEN));
		} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			event.setCancelled(true);
			selectionManager.setPos2(player.getUniqueId(), event.getClickedBlock().getLocation());
			player.sendMessage(Component.text("Position 2 set!", NamedTextColor.GREEN));
		}
	}
}