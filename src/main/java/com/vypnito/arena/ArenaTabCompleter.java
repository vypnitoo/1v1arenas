package com.vypnito.arena; // Změněno na com.vypnito.arena

import com.vypnito.arena.arenas.ArenaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections; // Pro Collections.sort
import java.util.List;

/**
 * Poskytuje tab completion návrhy pro příkaz /arena.
 * Tato třída implementuje TabCompleter, čímž odděluje logiku tab completion od CommandExecutoru.
 */
public class ArenaTabCompleter implements TabCompleter {

	private final ArenaManager arenaManager; // Reference na správce arén, potřebujeme pro názvy arén

	// Seznam všech dostupných subpříkazů.
	private static final List<String> SUBCOMMANDS_ALL = Arrays.asList("wand", "create", "delete", "reload", "edit");
	// Seznam subpříkazů, které očekávají název arény jako druhý argument.
	private static final List<String> SUBCOMMANDS_WITH_NAME_ARG = Arrays.asList("delete", "edit");


	/**
	 * Konstruktor pro ArenaTabCompleter.
	 * @param arenaManager Instance ArenaManageru pro získání seznamu názvů arén.
	 */
	public ArenaTabCompleter(ArenaManager arenaManager) {
		this.arenaManager = arenaManager;
	}

	/**
	 * Poskytuje tab completion návrhy.
	 * @param sender Odesílatel příkazu.
	 * @param command Objekt příkazu.
	 * @param label Alias příkazu.
	 * @param args Aktuální argumenty příkazu.
	 * @return Seznam možných dokončení.
	 */
	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		final List<String> completions = new ArrayList<>(); // Seznam pro shromáždění návrhů.

		// Poznámka: Oprávnění zde nekontrolujeme přímo, protože TabCompleter by měl vždy vrátit návrhy,
		// i když je hráč nemůže použít. Oprávnění se kontrolují v CommandExecutoru.

		// První argument: Navrhnout všechny subpříkazy.
		if (args.length == 1) {
			StringUtil.copyPartialMatches(args[0], SUBCOMMANDS_ALL, completions);
		}
		// Druhý argument: Navrhnout názvy arén pro specifické subpříkazy.
		else if (args.length == 2) {
			if (SUBCOMMANDS_WITH_NAME_ARG.contains(args[0].toLowerCase())) {
				// Získání aktuálních názvů arén a filtrování podle částečné shody.
				StringUtil.copyPartialMatches(args[1], arenaManager.getArenaNames(), completions);
			}
			// Pro 'create' obvykle nenavrhujeme konkrétní názvy, protože se vytváří nový.
		}
		// Pro argumenty nad druhým momentálně žádné dokončení neposkytujeme.

		Collections.sort(completions); // Seřadíme návrhy abecedně pro lepší vzhled.
		return completions;
	}
}