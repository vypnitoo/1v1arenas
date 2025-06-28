package com.vypnito.arena.gui;

import com.vypnito.arena.arena;
import com.vypnito.arena.arenas.Arenas;
import com.vypnito.arena.arenas.ArenaManager;
import com.vypnito.arena.arenas.ArenaSettings;
import com.vypnito.arena.player.PlayerManager;
import com.vypnito.arena.player.PlayerState;
import com.vypnito.arena.player.SelectionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GUIManager implements Listener {
	public static final String SETUP_TITLE_PREFIX = "Arena Setup: ";
	public static final String EDIT_TITLE_PREFIX = "Editing Arena: ";
	public static final String EFFECTS_TITLE_PREFIX = "Effects: ";
	public static final String ARENA_TYPE_SELECTION_TITLE = "Select Arena Type";

	private static final int ITEMS_PER_PAGE = 45;

	private final arena plugin;
	private final SelectionManager selectionManager;
	private final ArenaManager arenaManager;
	private final PlayerManager playerManager;
	private final NamespacedKey effectTypeKey;
	private final NamespacedKey requiredPlayersKey;
	private final NamespacedKey customTypeKey;

	public GUIManager(arena plugin, SelectionManager selectionManager, ArenaManager arenaManager, PlayerManager playerManager) {
		this.plugin = plugin;
		this.selectionManager = selectionManager;
		this.arenaManager = arenaManager;
		this.playerManager = playerManager;
		this.effectTypeKey = new NamespacedKey(plugin, "effect_type");
		this.requiredPlayersKey = new NamespacedKey(plugin, "required_players");
		this.customTypeKey = new NamespacedKey(plugin, "custom_type");
	}

	public void openArenaTypeSelectionGUI(Player player, String arenaName) {
		Inventory gui = Bukkit.createInventory(null, 27, Component.text(ARENA_TYPE_SELECTION_TITLE, NamedTextColor.DARK_AQUA, TextDecoration.BOLD));

		playerManager.setPlayerState(player, new PlayerState(arenaName, null));

		gui.setItem(10, createArenaTypeItem(Material.WOODEN_SWORD, "§e1v1 Arena", 2));
		gui.setItem(12, createArenaTypeItem(Material.STONE_SWORD, "§b2v2 Arena", 4));
		gui.setItem(14, createArenaTypeItem(Material.IRON_SWORD, "§a3v3 Arena", 6));
		gui.setItem(16, createArenaTypeItem(Material.GOLDEN_SWORD, "§64v4 Arena", 8));
		gui.setItem(20, createArenaTypeItem(Material.DIAMOND_SWORD, "§d8v8 Arena", 16));
		gui.setItem(22, createArenaTypeItem(Material.NETHERITE_SWORD, "§c16v16 Arena", 32));
		gui.setItem(24, createArenaTypeItem(Material.DRAGON_EGG, "§532v32 Arena", 64));
		gui.setItem(26, createCustomArenaTypeItem(Material.END_CRYSTAL, "§9Custom (e.g., 5v5)"));

		player.openInventory(gui);
	}

	public void openSetupGUI(Player player, String arenaName) {
		Inventory gui = Bukkit.createInventory(null, 27, Component.text(SETUP_TITLE_PREFIX + arenaName, NamedTextColor.DARK_AQUA, TextDecoration.BOLD));

		Location pos1 = selectionManager.getPos1(player);
		Location pos2 = selectionManager.getPos2(player);

		gui.setItem(11, createGuiItem(Material.REDSTONE_TORCH, "§ePosition 1",
				pos1 == null ? "§cNot Set" : "§aSet: " + formatLoc(pos1),
				"§7Click with wand to set."));
		gui.setItem(15, createGuiItem(Material.REDSTONE_TORCH, "§ePosition 2",
				pos2 == null ? "§cNot Set" : "§aSet: " + formatLoc(pos2),
				"§7Click with wand to set."));

		PlayerState playerState = playerManager.getPlayerState(player);
		String requiredPlayersInfo;
		if (playerState != null && playerState.getSelectedRequiredPlayers() != null) {
			requiredPlayersInfo = "§7Required Players: §e" + playerState.getSelectedRequiredPlayers();
		} else {
			requiredPlayersInfo = "§7Required Players: §cNot Selected!";
		}
		gui.setItem(4, createGuiItem(Material.BOOK, "§bArena Type",
				requiredPlayersInfo,
				"§7Select arena type via /arena create."));

		gui.setItem(22, createGuiItem(Material.EMERALD_BLOCK, "§aCreate Arena",
				"§7Click to create with current selection.",
				requiredPlayersInfo));
		gui.setItem(26, createGuiItem(Material.CLOCK, "§eRefresh",
				"§7Update status from wand selection."));
		gui.setItem(0, createNavItem("§cBack to Type Selection", Material.BARRIER));

		player.openInventory(gui);
	}

	public void openEditGUI(Player player, Arenas arena) {
		Inventory gui = Bukkit.createInventory(null, 54, Component.text(EDIT_TITLE_PREFIX + arena.getName(), NamedTextColor.DARK_AQUA, TextDecoration.BOLD));
		ArenaSettings settings = arena.getSettings();

		gui.setItem(4, createGuiItem(Material.COMPASS, "§bArena Type",
				"§7Required Players: §e" + settings.getRequiredPlayers()));

		gui.setItem(10, createGuiItem(Material.REDSTONE_TORCH, "§eSet Position 1",
				"§7Current: " + formatLoc(arena.getPos1()),
				"§aClick to re-select."));
		gui.setItem(11, createGuiItem(Material.REDSTONE_TORCH, "§eSet Position 2",
				"§7Current: " + formatLoc(arena.getPos2()),
				"§aClick to re-select."));

		gui.setItem(19, createToggleItem(settings.isAllowBlockBreak(), "Block Breaking"));
		gui.setItem(20, createToggleItem(settings.isAllowBlockPlace(), "Block Placing"));
		gui.setItem(21, createToggleItem(settings.isAllowItemDrop(), "Item Dropping"));
		gui.setItem(22, createToggleItem(settings.isDisableHunger(), "Hunger Drain"));

		gui.setItem(28, createGuiItem(Material.DIAMOND_BLOCK, "§bSet Wall Material",
				"§7Current: §e" + settings.getWallMaterial().name(),
				"§aClick to set in chat."));
		gui.setItem(29, createGuiItem(Material.CLOCK, "§bSet Death Delay",
				"§7Current: §e" + settings.getWallRemovalDelay() + "s",
				"§aClick to set in chat."));

		gui.setItem(37, createGuiItem(Material.POTION, "§dEdit Effects",
				"§7Current: §e" + settings.getEffects().size() + " effects",
				"§aClick to manage."));

		gui.setItem(49, createGuiItem(Material.BARRIER, "§cClose Arena Editor",
				"§7Click to close the editor."));

		player.openInventory(gui);
	}

	public void openEffectsGUI(Player player, Arenas arena, int page) {
		List<PotionEffectType> allEffects = Arrays.stream(PotionEffectType.values())
				.filter(Objects::nonNull)
				.sorted((t1, t2) -> t1.getName().compareTo(t2.getName()))
				.collect(Collectors.toList());
		int maxPages = (int) Math.ceil((double) allEffects.size() / ITEMS_PER_PAGE);

		Inventory gui = Bukkit.createInventory(null, 54, Component.text(EFFECTS_TITLE_PREFIX + arena.getName() + " (Page " + (page + 1) + ")", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));

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

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}

		String guiTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

		if (guiTitle.startsWith(ARENA_TYPE_SELECTION_TITLE)) {
			handleArenaTypeSelectionClick(event, guiTitle);
		} else if (guiTitle.startsWith(SETUP_TITLE_PREFIX)) {
			handleSetupGUIClick(event, guiTitle);
		} else if (guiTitle.startsWith(EDIT_TITLE_PREFIX)) {
			handleEditGUIClick(event, guiTitle);
		} else if (guiTitle.startsWith(EFFECTS_TITLE_PREFIX)) {
			handleEffectsGUIClick(event, guiTitle);
		}
	}

	private void handleArenaTypeSelectionClick(InventoryClickEvent event, String guiTitle) {
		event.setCancelled(true);
		Player player = (Player) event.getWhoClicked();
		ItemStack clickedItem = event.getCurrentItem();
		if (clickedItem == null) return;

		ItemMeta meta = clickedItem.getItemMeta();
		if (meta == null) return;

		PlayerState playerState = playerManager.getPlayerState(player);
		if (playerState == null || playerState.getArenaNameForCreation() == null) {
			player.sendMessage(Component.text("Error: Arena creation flow interrupted. Please restart with /arena create <name>.", NamedTextColor.RED));
			player.closeInventory();
			return;
		}

		Integer requiredPlayers = meta.getPersistentDataContainer().get(requiredPlayersKey, PersistentDataType.INTEGER);

		Boolean isCustomType = meta.getPersistentDataContainer().get(customTypeKey, PersistentDataType.BOOLEAN);


		if (requiredPlayers != null) {
			playerState.setSelectedRequiredPlayers(requiredPlayers);
			openSetupGUI(player, playerState.getArenaNameForCreation());
		} else if (isCustomType != null && isCustomType) {
			playerManager.setPlayerState(player, new PlayerState(playerState.getArenaNameForCreation(), PlayerState.InputType.CUSTOM_REQUIRED_PLAYERS));
			player.closeInventory();
			player.sendMessage(Component.text("Please type the required players in 'XvX' format (e.g., '5v5') in chat.", NamedTextColor.YELLOW));
		}
	}

	private void handleSetupGUIClick(InventoryClickEvent event, String guiTitle) {
		event.setCancelled(true);
		Player player = (Player) event.getWhoClicked();
		String arenaName = guiTitle.substring(SETUP_TITLE_PREFIX.length());

		PlayerState playerState = playerManager.getPlayerState(player);
		if (playerState == null || playerState.getArenaNameForCreation() == null) {
			player.sendMessage(Component.text("Error: Arena creation flow interrupted. Please restart with /arena create <name>.", NamedTextColor.RED));
			player.closeInventory();
			return;
		}

		switch (event.getCurrentItem().getType()) {
			case CLOCK -> openSetupGUI(player, arenaName);
			case EMERALD_BLOCK -> {
				Location pos1 = selectionManager.getPos1(player);
				Location pos2 = selectionManager.getPos2(player);
				Integer selectedRequiredPlayers = playerState.getSelectedRequiredPlayers();

				if (pos1 != null && pos2 != null && selectedRequiredPlayers != null) {
					arenaManager.createArena(arenaName, pos1, pos2, selectedRequiredPlayers);
					selectionManager.clearSelection(player);
					playerManager.clearPlayerState(player);
					player.sendMessage(Component.text("Arena '" + arenaName + "' created! Use '/arena edit " + arenaName + "' to configure.", NamedTextColor.AQUA));
					player.closeInventory();
				} else {
					player.sendMessage(Component.text("You must set both positions with the wand AND select an arena type first.", NamedTextColor.RED));
				}
			}
			case BARRIER -> {
				if (event.getSlot() == 0) {
					openArenaTypeSelectionGUI(player, playerState.getArenaNameForCreation());
				}
			}
		}
	}

	private void handleEditGUIClick(InventoryClickEvent event, String guiTitle) {
		event.setCancelled(true);
		Player player = (Player) event.getWhoClicked();
		String arenaName = guiTitle.substring(EDIT_TITLE_PREFIX.length());
		Arenas arena = arenaManager.getArena(arenaName);
		if (arena == null || event.getCurrentItem() == null) { player.closeInventory(); return; }

		Material clickedMaterial = event.getCurrentItem().getType();
		int clickedSlot = event.getSlot();

		switch (clickedMaterial) {
			case REDSTONE_TORCH -> {
				PlayerState.SelectionType type = clickedSlot == 10 ? PlayerState.SelectionType.POS1 : PlayerState.SelectionType.POS2;
				playerManager.setPlayerState(player, new PlayerState(arena, type));
				player.closeInventory();
				player.sendMessage(Component.text("Click a block to set " + type.name() + ". Type 'cancel' to abort.", NamedTextColor.YELLOW));
			}
			case LIME_DYE, GRAY_DYE -> {
				switch(clickedSlot) {
					case 19 -> arena.getSettings().setAllowBlockBreak(!arena.getSettings().isAllowBlockBreak());
					case 20 -> arena.getSettings().setAllowBlockPlace(!arena.getSettings().isAllowBlockPlace());
					case 21 -> arena.getSettings().setAllowItemDrop(!arena.getSettings().isAllowItemDrop());
					case 22 -> arena.getSettings().setDisableHunger(!arena.getSettings().isDisableHunger());
				}
				arenaManager.saveArena(arena);
				openEditGUI(player, arena);
			}
			case DIAMOND_BLOCK -> {
				playerManager.setPlayerState(player, new PlayerState(arena, PlayerState.InputType.WALL_MATERIAL));
				player.closeInventory();
				player.sendMessage(Component.text("Type the new wall material name in chat.", NamedTextColor.YELLOW));
			}
			case CLOCK -> {
				playerManager.setPlayerState(player, new PlayerState(arena, PlayerState.InputType.DELAY));
				player.closeInventory();
				player.sendMessage(Component.text("Type the new delay (in seconds) in chat.", NamedTextColor.YELLOW));
			}
			case POTION -> openEffectsGUI(player, arena, 0);

			case BARRIER -> {
				if (clickedSlot == 49) {
					playerManager.clearPlayerState(player);
					player.closeInventory();
					player.sendMessage(Component.text("Arena editor closed.", NamedTextColor.AQUA));
				}
			}
		}
	}

	private void handleEffectsGUIClick(InventoryClickEvent event, String guiTitle) {
		event.setCancelled(true);
		Player player = (Player) event.getWhoClicked();
		String arenaName = guiTitle.substring(EFFECTS_TITLE_PREFIX.length()).split(" \\(Page")[0];
		Arenas arena = arenaManager.getArena(arenaName);
		if (arena == null || event.getCurrentItem() == null) { player.closeInventory(); return; }

		ItemStack clickedItem = event.getCurrentItem();
		int currentPage = Integer.parseInt(guiTitle.replaceAll("[^0-9]", "")) - 1;

		switch (clickedItem.getType()) {
			case ARROW -> {
				if (PlainTextComponentSerializer.plainText().serialize(clickedItem.displayName()).contains("Next")) {
					openEffectsGUI(player, arena, currentPage + 1);
				} else {
					openEffectsGUI(player, arena, currentPage - 1);
				}
			}
			case BARRIER -> openEditGUI(player, arena);
			case POTION -> {
				ItemMeta meta = clickedItem.getItemMeta();
				if (meta == null) return;

				String keyString = meta.getPersistentDataContainer().get(effectTypeKey, PersistentDataType.STRING);
				if (keyString == null) return;

				NamespacedKey key = NamespacedKey.fromString(keyString);
				if (key == null) return;

				PotionEffectType type = Bukkit.getRegistry(PotionEffectType.class).get(key);
				if (type == null) return;

				if (arena.getSettings().hasEffect(type)) {
					arena.getSettings().removeEffect(type);
					player.sendMessage(Component.text("Effect " + formatEffectName(type.getName()) + " removed.", NamedTextColor.RED));
				} else {
					playerManager.setPlayerState(player, new PlayerState(arena, type));
					player.closeInventory();
					player.sendMessage(Component.text("Type the amplifier for " + formatEffectName(type.getName()) + " in chat (e.g., '1' for level II).", NamedTextColor.YELLOW));
					return;
				}
				arenaManager.saveArena(arena);
				openEffectsGUI(player, arena, currentPage);
			}
		}
	}

	private ItemStack createArenaTypeItem(Material material, String name, int requiredPlayers) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
		meta.lore(List.of(
				Component.text("§7Required Players: §e" + requiredPlayers).decoration(TextDecoration.ITALIC, false),
				Component.text("§eClick to select this type.").decoration(TextDecoration.ITALIC, false)
		));
		meta.getPersistentDataContainer().set(requiredPlayersKey, PersistentDataType.INTEGER, requiredPlayers);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack createCustomArenaTypeItem(Material material, String name) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
		meta.lore(List.of(
				Component.text("§7Type players in XvX format in chat.").decoration(TextDecoration.ITALIC, false),
				Component.text("§eClick to define a custom type.").decoration(TextDecoration.ITALIC, false)
		));
		meta.getPersistentDataContainer().set(customTypeKey, PersistentDataType.BOOLEAN, true);
		item.setItemMeta(meta);
		return item;
	}

	private String formatLoc(Location loc) {
		if (loc == null) return "§cNot Set";
		return "§a" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
	}

	private ItemStack createToggleItem(boolean enabled, String name) {
		Material m = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
		String s = enabled ? "§aENABLED" : "§cDISABLED";
		return createGuiItem(m, "§f" + name, "§7Status: " + s, "§eClick to toggle.");
	}

	private ItemStack createEffectItem(PotionEffectType type, boolean hasEffect) {
		String name = hasEffect ? "§a" + formatEffectName(type.getName()) : "§7" + formatEffectName(type.getName());
		List<String> lore = hasEffect ? List.of("§cClick to REMOVE.") : List.of("§eClick to ADD.");
		ItemStack item = createGuiItem(Material.POTION, name, lore.toArray(new String[0]));

		ItemMeta meta = item.getItemMeta();
		meta.getPersistentDataContainer().set(effectTypeKey, PersistentDataType.STRING, type.getKey().toString());

		if (hasEffect) {
			meta.addEnchant(Enchantment.UNBREAKING, 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}
		item.setItemMeta(meta);
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