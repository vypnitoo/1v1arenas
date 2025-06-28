package com.vypnito.arena; // Změněno na com.vypnito.arena

import com.vypnito.arena.arenas.ArenaManager;
import com.vypnito.arena.arenas.Arenas;
import com.vypnito.arena.gui.GUIManager;
import com.vypnito.arena.player.SelectionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Zpracovává hlavní příkaz /arena a jeho subpříkazy.
 * Tato třída implementuje pouze CommandExecutor, obsluha tab completion je v ArenaTabCompleter.
 */
public class ArenaCommand implements CommandExecutor { // Již neimplementuje TabCompleter

	private final arena plugin; // Reference na hlavní instanci pluginu
	private final ArenaManager arenaManager; // Správce arén
	private final SelectionManager selectionManager; // Správce výběrů
	private final GUIManager guiManager; // Správce GUI

	/**
	 * Konstruktor pro ArenaCommand.
	 * @param plugin Hlavní instance pluginu.
	 * @param arenaManager Správce dat arén.
	 * @param selectionManager Správce výběrů hráče (hůlka).
	 * @param guiManager Správce všech interakcí s GUI.
	 */
	public ArenaCommand(arena plugin, ArenaManager arenaManager, SelectionManager selectionManager, GUIManager guiManager) {
		this.plugin = plugin;
		this.arenaManager = arenaManager;
		this.selectionManager = selectionManager;
		this.guiManager = guiManager;
	}

	/**
	 * Vykonává příkaz /arena.
	 * @param sender Odesílatel příkazu.
	 * @param command Objekt příkazu.
	 * @param label Alias použitý pro příkaz.
	 * @param args Argumenty příkazu.
	 * @return True, pokud byl příkaz úspěšně zpracován, jinak false.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// Ujistíme se, že příkaz může používat pouze hráč pro interakce s GUI a hůlkou.
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
			return true;
		}

		// Kontrola potřebných oprávnění.
		if (!player.hasPermission("smartarenas.admin")) {
			player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
			return true;
		}

		// Zobrazení základního použití, pokud nejsou poskytnuty žádné argumenty.
		if (args.length == 0) {
			player.sendMessage(Component.text("Usage: /arena <wand|create|delete|reload|edit>", NamedTextColor.YELLOW));
			return true;
		}

		// Zpracování subpříkazů.
		switch (args[0].toLowerCase()) {
			case "wand":
				selectionManager.giveWand(player);
				break;
			case "create":
				// Vyžaduje název arény pro vytvoření.
				if (args.length < 2) {
					player.sendMessage(Component.text("Usage: /arena create <name>", NamedTextColor.YELLOW));
					return true;
				}
				String arenaName = args[1];
				// Otevře GUI pro výběr typu arény (1v1, 2v2, atd.).
				guiManager.openArenaTypeSelectionGUI(player, arenaName);
				break;
			case "delete":
				// Vyžaduje název arény pro smazání.
				if (args.length < 2) {
					player.sendMessage(Component.text("Usage: /arena delete <name>", NamedTextColor.YELLOW));
					return true;
				}
				arenaManager.deleteArena(args[1]);
				break;
			case "reload":
				// Znovu načte konfiguraci pluginu (implementace v hlavní třídě pluginu).
				plugin.reloadConfig(); // Tímto se znovu načte hlavní konfigurace pluginu
				arenaManager.loadReplaceableMaterials(); // A znovu se načtou vlastní materiály
				player.sendMessage(Component.text("SmartArenas configuration reloaded.", NamedTextColor.GREEN));
				break;
			case "edit":
				// Vyžaduje název arény pro úpravu.
				if (args.length < 2) {
					player.sendMessage(Component.text("Usage: /arena edit <name>", NamedTextColor.YELLOW));
					return true;
				}
				Arenas arenaToEdit = arenaManager.getArena(args[1]);
				if (arenaToEdit == null) {
					player.sendMessage(Component.text("Arena '" + args[1] + "' not found.", NamedTextColor.RED));
					return true;
				}
				guiManager.openEditGUI(player, arenaToEdit);
				break;
			default:
				player.sendMessage(Component.text("Unknown subcommand. Usage: /arena <wand|create|delete|reload|edit>", NamedTextColor.YELLOW));
				break;
		}
		return true;
	}
}