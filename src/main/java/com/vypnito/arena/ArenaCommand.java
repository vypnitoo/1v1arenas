package com.vypnito.arena;

import com.vypnito.arena.arenas.Arenas;
import com.vypnito.arena.arenas.ArenaManager;
import com.vypnito.arena.gui.GUIManager;
import com.vypnito.arena.player.PlayerManager;
import com.vypnito.arena.player.SelectionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaCommand implements CommandExecutor {
	private final ArenaManager arenaManager;
	private final SelectionManager selectionManager;
	private final GUIManager guiManager;

	public ArenaCommand(ArenaManager arenaManager, SelectionManager selectionManager, GUIManager guiManager) {
		this.arenaManager = arenaManager;
		this.selectionManager = selectionManager;
		this.guiManager = guiManager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command is for players only.", NamedTextColor.RED));
			return true;
		}
		if (!player.hasPermission("1v1rooms.admin")) {
			player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
			return true;
		}
		if (args.length == 0) {
			sendHelp(player);
			return true;
		}
		String subCommand = args[0].toLowerCase();
		if (args.length == 1) {
			if ("wand".equals(subCommand)) {
				selectionManager.giveWand(player);
				return true;
			}
			if ("reload".equals(subCommand)) {
				arenaManager.loadArenas();
				player.sendMessage(Component.text("Configuration and arenas reloaded.", NamedTextColor.GREEN));
				return true;
			}
		}
		if (args.length < 2) {
			sendHelp(player);
			return true;
		}
		String arenaName = args[1];
		switch (subCommand) {
			case "create" -> {
				Location pos1 = selectionManager.getPos1(player);
				Location pos2 = selectionManager.getPos2(player);
				if (pos1 == null || pos2 == null) {
					player.sendMessage(Component.text("You must set Pos1 and Pos2 with the wand first.", NamedTextColor.RED));
					return true;
				}
				arenaManager.createArena(arenaName, pos1, pos2);
				selectionManager.clearSelection(player);
				player.sendMessage(Component.text("Arena '" + arenaName + "' created! Use '/arena edit " + arenaName + "' to configure.", NamedTextColor.AQUA));
			}
			case "delete" -> {
				arenaManager.deleteArena(arenaName);
				player.sendMessage(Component.text("Arena '" + arenaName + "' has been deleted.", NamedTextColor.GREEN));
			}
			case "edit" -> {
				Arenas arena = arenaManager.getArena(arenaName);
				if (arena == null) {
					player.sendMessage(Component.text("Arena '" + arenaName + "' not found.", NamedTextColor.RED));
					return true;
				}
				guiManager.openEditGUI(player, arena);
			}
			default -> sendHelp(player);
		}
		return true;
	}

	private void sendHelp(Player player) {
		player.sendMessage(Component.text("--- Arena Admin Help ---", NamedTextColor.GOLD));
		player.sendMessage(Component.text("/arena wand", NamedTextColor.YELLOW).append(Component.text(" - Get the selection wand.", NamedTextColor.GRAY)));
		player.sendMessage(Component.text("/arena create <name>", NamedTextColor.YELLOW).append(Component.text(" - Create an arena.", NamedTextColor.GRAY)));
		player.sendMessage(Component.text("/arena edit <name>", NamedTextColor.YELLOW).append(Component.text(" - Open the GUI editor for an arena.", NamedTextColor.GRAY)));
		player.sendMessage(Component.text("/arena delete <name>", NamedTextColor.YELLOW).append(Component.text(" - Delete an arena.", NamedTextColor.GRAY)));
		player.sendMessage(Component.text("/arena reload", NamedTextColor.YELLOW).append(Component.text(" - Reload the config.", NamedTextColor.GRAY)));
	}
}