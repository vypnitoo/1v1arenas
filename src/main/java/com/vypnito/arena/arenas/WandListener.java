package com.vypnito.arena.arenas;

import com.vypnito.arena.gui.GUIManager;
import com.vypnito.arena.player.PlayerManager;
import com.vypnito.arena.player.PlayerState;
import com.vypnito.arena.player.SelectionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class WandListener implements Listener {
	private final SelectionManager selectionManager;
	private final PlayerManager playerManager;
	private final ArenaManager arenaManager;
	private final GUIManager guiManager;

	public WandListener(SelectionManager selectionManager, PlayerManager playerManager, ArenaManager arenaManager, GUIManager guiManager) {
		this.selectionManager = selectionManager;
		this.playerManager = playerManager;
		this.arenaManager = arenaManager;
		this.guiManager = guiManager;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		PlayerState playerState = playerManager.getPlayerState(player);
		ItemStack itemInHand = event.getItem();

		if (playerState != null && playerState.getSelectionType() != null && event.getAction() == Action.LEFT_CLICK_BLOCK) {
			event.setCancelled(true);
			Arenas arena = playerState.getArena();
			if (playerState.getSelectionType() == PlayerState.SelectionType.POS1) { arena.setPos1(event.getClickedBlock().getLocation()); }
			else { arena.setPos2(event.getClickedBlock().getLocation()); }
			arenaManager.saveArena(arena);
			playerManager.clearPlayerState(player);
			player.sendMessage(Component.text("Arena corner updated!", NamedTextColor.GREEN));
			guiManager.openEditGUI(player, arena);
			return;
		}

		if (itemInHand == null || itemInHand.getType() != Material.WOODEN_AXE || !itemInHand.hasItemMeta() || !itemInHand.getItemMeta().hasDisplayName()) return;
		if (!player.hasPermission("1v1rooms.admin")) return;

		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			event.setCancelled(true);
			selectionManager.setPos1(player, event.getClickedBlock().getLocation());
			player.sendMessage(Component.text("Position 1 set!", NamedTextColor.GREEN));
		} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			event.setCancelled(true);
			selectionManager.setPos2(player, event.getClickedBlock().getLocation());
			player.sendMessage(Component.text("Position 2 set!", NamedTextColor.GREEN));
		}
	}
}