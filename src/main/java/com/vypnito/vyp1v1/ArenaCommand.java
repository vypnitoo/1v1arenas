package com.vypnito.vyp1v1;

import com.vypnito.vyp1v1.arena.ArenaManager;
import com.vypnito.vyp1v1.arena.SelectionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaCommand implements CommandExecutor {
	private final Vyp1v1 plugin;
	private final ArenaManager arenaManager;
	private final SelectionManager selectionManager;

	public ArenaCommand(Vyp1v1 plugin, ArenaManager arenaManager, SelectionManager selectionManager) {
		this.plugin = plugin;
		this.arenaManager = arenaManager;
		this.selectionManager = selectionManager;
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
				player.sendMessage(Component.text("You received the selection wand! Left-click for Pos1, Right-click for Pos2.", NamedTextColor.GREEN));
				return true;
			}
			if ("reload".equals(subCommand)) {
				plugin.reloadConfig();
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
			case "create" -> handleCreate(player, arenaName);
			case "delete" -> {
				arenaManager.deleteArena(arenaName);
				player.sendMessage(Component.text("Arena '" + arenaName + "' has been deleted.", NamedTextColor.GREEN));
			}
			default -> sendHelp(player);
		}
		return true;
	}

	private void handleCreate(Player player, String arenaName) {
		Location pos1 = selectionManager.getPos1(player.getUniqueId());
		Location pos2 = selectionManager.getPos2(player.getUniqueId());

		if (pos1 == null || pos2 == null) {
			player.sendMessage(Component.text("You must set Pos1 and Pos2 with the wand before creating.", NamedTextColor.RED));
			return;
		}
		arenaManager.createArena(arenaName, pos1, pos2);
		selectionManager.clearSelection(player.getUniqueId());
		player.sendMessage(Component.text("Arena '" + arenaName + "' has been successfully created!", NamedTextColor.AQUA));
	}

	private void sendHelp(Player player) {
		player.sendMessage(Component.text("--- Vyp1v1 Admin Help ---", NamedTextColor.GOLD));
		player.sendMessage(Component.text("/vyp wand", NamedTextColor.YELLOW).append(Component.text(" - Get the arena selection wand.", NamedTextColor.GRAY)));
		player.sendMessage(Component.text("/vyp create <name>", NamedTextColor.YELLOW).append(Component.text(" - Create arena using selections.", NamedTextColor.GRAY)));
		player.sendMessage(Component.text("/vyp delete <name>", NamedTextColor.YELLOW).append(Component.text(" - Delete an arena.", NamedTextColor.GRAY)));
		player.sendMessage(Component.text("/vyp reload", NamedTextColor.YELLOW).append(Component.text(" - Reload the config.", NamedTextColor.GRAY)));
	}
}