package com.vypnito.vyp1v1;

import com.vypnito.vyp1v1.arena.ArenaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArenaTabCompleter implements TabCompleter {
	private final ArenaManager arenaManager;
	private static final List<String> SUBCOMMANDS_ALL = Arrays.asList("wand", "create", "delete", "reload");
	private static final List<String> SUBCOMMANDS_WITH_NAME = Arrays.asList("create", "delete");

	public ArenaTabCompleter(ArenaManager arenaManager) {
		this.arenaManager = arenaManager;
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		final List<String> completions = new ArrayList<>();
		if (args.length == 1) {
			StringUtil.copyPartialMatches(args[0], SUBCOMMANDS_ALL, completions);
		} else if (args.length == 2) {
			if (SUBCOMMANDS_WITH_NAME.contains(args[0].toLowerCase())) {
				List<String> arenaNames = new ArrayList<>(arenaManager.getArenaNames());
				StringUtil.copyPartialMatches(args[1], arenaNames, completions);
			}
		}
		return completions;
	}
}