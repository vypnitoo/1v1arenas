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

/**
 * Manages all GUI interactions for the arena plugin, including setup, editing, and effects.
 * This class also acts as the primary InventoryClickEvent listener.
 */
public class GUIManager implements Listener {
	// Constants for GUI titles to easily identify different GUIs
	public static final String SETUP_TITLE_PREFIX = "Arena Creation: ";
	public static final String EDIT_TITLE_PREFIX = "Editing Arena: ";
	public static final String EFFECTS_TITLE_PREFIX = "Effects: ";

	// Number of effect items to display per page in the Effects GUI
	private static final int ITEMS_PER_PAGE = 45;

	// Plugin instance and various managers for core functionalities
	private final arena plugin;
	private final SelectionManager selectionManager;
	private final ArenaManager arenaManager;
	private final PlayerManager playerManager;
	private final NamespacedKey effectTypeKey; // Custom key for storing PotionEffectType data on ItemStack

	/**
	 * Constructor for the GUIManager.
	 *
	 * @param plugin The main plugin instance.
	 * @param selectionManager Manages player's block selections (wand).
	 * @param arenaManager Manages arena data (creation, saving, loading).
	 * @param playerManager Manages transient player states (e.g., waiting for chat input).
	 */
	public GUIManager(arena plugin, SelectionManager selectionManager, ArenaManager arenaManager, PlayerManager playerManager) {
		this.plugin = plugin;
		this.selectionManager = selectionManager;
		this.arenaManager = arenaManager;
		this.playerManager = playerManager;
		// Initialize the NamespacedKey unique to this plugin for custom item data
		this.effectTypeKey = new NamespacedKey(plugin, "effect_type");
	}

	// --- GUI OPENING METHODS ---

	/**
	 * Opens the Arena Setup GUI for a player. This GUI allows the player to define
	 * the two corners (pos1 and pos2) of a new arena.
	 *
	 * @param player The player to open the GUI for.
	 * @param arenaName The name of the arena being set up.
	 */
	public void openSetupGUI(Player player, String arenaName) {
		// Create an inventory with a size of 27 slots (3 rows) and a custom title
		Inventory gui = Bukkit.createInventory(null, 27, Component.text(SETUP_TITLE_PREFIX + arenaName));
		Location pos1 = selectionManager.getPos1(player); // Get current pos1 from selection manager
		Location pos2 = selectionManager.getPos2(player); // Get current pos2 from selection manager

		// Set items for Position 1 and Position 2 display, showing their current status
		gui.setItem(11, createGuiItem(Material.REDSTONE_TORCH, "Position 1", pos1 == null ? "§cNot Set" : "§aSet: " + formatLoc(pos1)));
		gui.setItem(15, createGuiItem(Material.REDSTONE_TORCH, "Position 2", pos2 == null ? "§cNot Set" : "§aSet: " + formatLoc(pos2)));

		// Set items for creating the arena and refreshing the GUI
		gui.setItem(22, createGuiItem(Material.EMERALD_BLOCK, "§aCreate Arena", "Click to create with current selection."));
		gui.setItem(26, createGuiItem(Material.CLOCK, "§eRefresh", "Update status from wand selection."));
		player.openInventory(gui); // Open the GUI for the player
	}

	/**
	 * Opens the Arena Edit GUI for a player, allowing them to configure various
	 * settings of an existing arena.
	 *
	 * @param player The player to open the GUI for.
	 * @param arena The arena to be edited.
	 */
	public void openEditGUI(Player player, Arenas arena) {
		// Create an inventory with a size of 54 slots (6 rows) and a custom title
		Inventory gui = Bukkit.createInventory(null, 54, Component.text(EDIT_TITLE_PREFIX + arena.getName()));
		ArenaSettings settings = arena.getSettings(); // Get current settings of the arena

		// Set items for re-selecting arena positions
		gui.setItem(10, createGuiItem(Material.REDSTONE_TORCH, "§eSet Position 1", "§7Current: " + formatLoc(arena.getPos1()), "§aClick to re-select."));
		gui.setItem(11, createGuiItem(Material.REDSTONE_TORCH, "§eSet Position 2", "§7Current: " + formatLoc(arena.getPos2()), "§aClick to re-select."));

		// Set toggle items for various arena rules (block breaking, placing, item dropping, hunger)
		gui.setItem(19, createToggleItem(settings.isAllowBlockBreak(), "Block Breaking"));
		gui.setItem(20, createToggleItem(settings.isAllowBlockPlace(), "Block Placing"));
		gui.setItem(21, createToggleItem(settings.isAllowItemDrop(), "Item Dropping"));
		gui.setItem(22, createToggleItem(settings.isDisableHunger(), "Hunger Drain"));

		// Set items for configuring wall material and death delay via chat input
		gui.setItem(28, createGuiItem(Material.DIAMOND_BLOCK, "§bSet Wall Material", "§7Current: §e" + settings.getWallMaterial().name(), "§aClick to set in chat."));
		gui.setItem(29, createGuiItem(Material.CLOCK, "§bSet Death Delay", "§7Current: §e" + settings.getWallRemovalDelay() + "s", "§aClick to set in chat."));

		// Set item to open the Effects GUI for managing potion effects
		gui.setItem(37, createGuiItem(Material.POTION, "§dEdit Effects", "§7Current: §e" + settings.getEffects().size() + " effects", "§aClick to manage."));

		// --- NEW: Button to completely close the arena editor ---
		gui.setItem(49, createGuiItem(Material.BARRIER, "§cClose Arena Editor", "§7Click to close the editor."));
		// --- END NEW BUTTON ---

		player.openInventory(gui); // Open the GUI for the player
	}

	/**
	 * Opens the Effects GUI for a player, allowing them to add or remove
	 * potion effects for an arena. Supports pagination.
	 *
	 * @param player The player to open the GUI for.
	 * @param arena The arena whose effects are being managed.
	 * @param page The current page number (0-indexed).
	 */
	public void openEffectsGUI(Player player, Arenas arena, int page) {
		// Get all available potion effect types, filter out nulls, sort alphabetically, and collect into a list
		List<PotionEffectType> allEffects = Arrays.stream(PotionEffectType.values())
				.filter(Objects::nonNull)
				.sorted((t1, t2) -> t1.getName().compareTo(t2.getName()))
				.collect(Collectors.toList());
		// Calculate the maximum number of pages needed
		int maxPages = (int) Math.ceil((double) allEffects.size() / ITEMS_PER_PAGE);

		// Create an inventory with a size of 54 slots and a dynamic title including arena name and page number
		Inventory gui = Bukkit.createInventory(null, 54, Component.text(EFFECTS_TITLE_PREFIX + arena.getName() + " (Page " + (page + 1) + ")"));

		// Calculate the starting index for effects on the current page
		int startIndex = page * ITEMS_PER_PAGE;
		// Populate the GUI with effect items for the current page
		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			// Break if we've gone past the last available effect
			if (startIndex + i >= allEffects.size()) break;
			PotionEffectType effectType = allEffects.get(startIndex + i);
			// Create an item for the effect, indicating if the arena already has it
			gui.setItem(i, createEffectItem(effectType, arena.getSettings().hasEffect(effectType)));
		}

		// Add navigation arrows for previous and next pages if applicable
		if (page > 0) gui.setItem(45, createNavItem("§aPrevious Page", Material.ARROW));
		if (page < maxPages - 1) gui.setItem(53, createNavItem("§aNext Page", Material.ARROW));
		// Add a "Back to Main Editor" button
		gui.setItem(49, createNavItem("§cBack to Main Editor", Material.BARRIER));

		player.openInventory(gui); // Open the GUI for the player
	}

	// --- EVENT HANDLER ---

	/**
	 * Main event handler for all InventoryClickEvents. It dispatches clicks
	 * to the appropriate handler method based on the GUI title.
	 *
	 * @param event The InventoryClickEvent.
	 */
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		// Ensure the clicked entity is a player
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}

		String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

		// Dispatch to the correct handler based on the GUI title prefix
		if (title.startsWith(SETUP_TITLE_PREFIX)) {
			handleSetupGUIClick(event, title);
		} else if (title.startsWith(EDIT_TITLE_PREFIX)) {
			handleEditGUIClick(event, title);
		} else if (title.startsWith(EFFECTS_TITLE_PREFIX)) {
			handleEffectsGUIClick(event, title);
		}
	}

	/**
	 * Handles clicks within the Arena Setup GUI.
	 *
	 * @param event The InventoryClickEvent.
	 * @param title The title of the clicked GUI.
	 */
	private void handleSetupGUIClick(InventoryClickEvent event, String title) {
		event.setCancelled(true); // Cancel the event to prevent item manipulation
		Player player = (Player) event.getWhoClicked();
		// Extract arena name from the GUI title
		String arenaName = title.substring(SETUP_TITLE_PREFIX.length());
		if (event.getCurrentItem() == null) return; // Ignore clicks on empty slots

		// Handle clicks based on the type of the clicked item
		switch (event.getCurrentItem().getType()) {
			case CLOCK -> openSetupGUI(player, arenaName); // Refresh GUI
			case EMERALD_BLOCK -> { // Create Arena button
				Location pos1 = selectionManager.getPos1(player); // Get selected pos1
				Location pos2 = selectionManager.getPos2(player); // Get selected pos2
				if (pos1 != null && pos2 != null) {
					arenaManager.createArena(arenaName, pos1, pos2); // Create the arena
					selectionManager.clearSelection(player); // Clear player's wand selection
					player.sendMessage(Component.text("Arena '" + arenaName + "' created! Use '/arena edit " + arenaName + "' to configure.", NamedTextColor.AQUA));
					player.closeInventory(); // Close the GUI
				} else {
					player.sendMessage(Component.text("You must set both positions with the wand first.", NamedTextColor.RED));
				}
			}
		}
	}

	/**
	 * Handles clicks within the Arena Edit GUI.
	 *
	 * @param event The InventoryClickEvent.
	 * @param title The title of the clicked GUI.
	 */
	private void handleEditGUIClick(InventoryClickEvent event, String title) {
		event.setCancelled(true); // Cancel the event to prevent item manipulation
		Player player = (Player) event.getWhoClicked();
		// Extract arena name from the GUI title
		String arenaName = title.substring(EDIT_TITLE_PREFIX.length());
		Arenas arena = arenaManager.getArena(arenaName); // Get the arena object
		if (arena == null || event.getCurrentItem() == null) { player.closeInventory(); return; } // Invalid arena or empty slot

		Material clicked = event.getCurrentItem().getType();
		int clickedSlot = event.getSlot(); // Get the slot where the click occurred

		// Handle clicks based on the type of the clicked item and its slot
		switch (clicked) {
			case REDSTONE_TORCH -> { // Set Position 1 or 2
				PlayerState.SelectionType type = clickedSlot == 10 ? PlayerState.SelectionType.POS1 : PlayerState.SelectionType.POS2;
				playerManager.setPlayerState(player, new PlayerState(arena, type)); // Set player into selection mode
				player.closeInventory(); // Close GUI
				player.sendMessage(Component.text("Click a block to set " + type.name() + ". Type 'cancel' to abort.", NamedTextColor.YELLOW));
			}
			case LIME_DYE, GRAY_DYE -> { // Toggle options
				switch(clickedSlot) {
					case 19 -> arena.getSettings().setAllowBlockBreak(!arena.getSettings().isAllowBlockBreak());
					case 20 -> arena.getSettings().setAllowBlockPlace(!arena.getSettings().isAllowBlockPlace());
					case 21 -> arena.getSettings().setAllowItemDrop(!arena.getSettings().isAllowItemDrop());
					case 22 -> arena.getSettings().setDisableHunger(!arena.getSettings().isDisableHunger());
				}
				arenaManager.saveArena(arena); // Save updated settings
				openEditGUI(player, arena); // Refresh GUI
			}
			case DIAMOND_BLOCK -> { // Set Wall Material
				playerManager.setPlayerState(player, new PlayerState(arena, PlayerState.InputType.WALL_MATERIAL)); // Set player into chat input mode
				player.closeInventory(); // Close GUI
				player.sendMessage(Component.text("Type the new wall material name in chat.", NamedTextColor.YELLOW));
			}
			case CLOCK -> { // Set Death Delay
				playerManager.setPlayerState(player, new PlayerState(arena, PlayerState.InputType.DELAY)); // Set player into chat input mode
				player.closeInventory(); // Close GUI
				player.sendMessage(Component.text("Type the new delay (in seconds) in chat.", NamedTextColor.YELLOW));
			}
			case POTION -> openEffectsGUI(player, arena, 0); // Open Effects GUI

			// --- NEW: Handle the "Close Arena Editor" button ---
			case BARRIER -> {
				if (clickedSlot == 49) { // Check if it's our specific barrier item for closing
					playerManager.clearPlayerState(player); // Clear any pending player state
					player.closeInventory(); // Close the current inventory for the player
					player.sendMessage(Component.text("Arena editor closed.", NamedTextColor.AQUA));
				}
			}
			// --- END NEW HANDLING ---
		}
	}

	/**
	 * Handles clicks within the Effects GUI.
	 *
	 * @param event The InventoryClickEvent.
	 * @param title The title of the clicked GUI.
	 */
	private void handleEffectsGUIClick(InventoryClickEvent event, String title) {
		event.setCancelled(true); // Cancel the event to prevent item manipulation
		Player player = (Player) event.getWhoClicked();
		// Extract arena name from the GUI title, removing the page part
		String arenaName = title.substring(EFFECTS_TITLE_PREFIX.length()).split(" \\(Page")[0];
		Arenas arena = arenaManager.getArena(arenaName); // Get the arena object
		if (arena == null || event.getCurrentItem() == null) { player.closeInventory(); return; } // Invalid arena or empty slot

		ItemStack clickedItem = event.getCurrentItem();
		// Parse current page number from GUI title
		int currentPage = Integer.parseInt(title.replaceAll("[^0-9]", "")) - 1;

		switch (clickedItem.getType()) {
			case ARROW -> { // Navigation arrows (Previous/Next Page)
				if (PlainTextComponentSerializer.plainText().serialize(clickedItem.displayName()).contains("Next")) {
					openEffectsGUI(player, arena, currentPage + 1);
				} else {
					openEffectsGUI(player, arena, currentPage - 1);
				}
			}
			case BARRIER -> openEditGUI(player, arena); // Back to Main Editor button
			case POTION -> { // Potion effect item
				ItemMeta meta = clickedItem.getItemMeta();
				if (meta == null) return;

				// Retrieve the effect key directly from the item's persistent data.
				String keyString = meta.getPersistentDataContainer().get(effectTypeKey, PersistentDataType.STRING);
				if (keyString == null) return;

				NamespacedKey key = NamespacedKey.fromString(keyString);
				if (key == null) return;

				PotionEffectType type = Bukkit.getRegistry(PotionEffectType.class).get(key);
				if (type == null) return;

				// Toggle effect: add or remove
				if (arena.getSettings().hasEffect(type)) {
					arena.getSettings().removeEffect(type); // Remove existing effect
					player.sendMessage(Component.text("Effect " + formatEffectName(type.getName()) + " removed.", NamedTextColor.RED));
				} else {
					// Set player into chat input mode to enter amplifier for the new effect
					playerManager.setPlayerState(player, new PlayerState(arena, type));
					player.closeInventory(); // Close current GUI
					player.sendMessage(Component.text("Type the amplifier for " + formatEffectName(type.getName()) + " in chat (e.g., '1' for level II).", NamedTextColor.YELLOW));
					return; // Do not refresh GUI immediately, wait for chat input
				}
				arenaManager.saveArena(arena); // Save updated arena settings
				openEffectsGUI(player, arena, currentPage); // Refresh Effects GUI
			}
		}
	}

	// --- HELPER METHODS FOR ITEM CREATION AND FORMATTING ---

	/**
	 * Formats a Location object into a readable string (X, Y, Z coordinates).
	 *
	 * @param loc The Location to format.
	 * @return A formatted string representation of the location, or "Not Set" if null.
	 */
	private String formatLoc(Location loc) {
		if (loc == null) return "§cNot Set";
		return "§a" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
	}

	/**
	 * Creates a toggle item (Lime Dye for enabled, Gray Dye for disabled) with status lore.
	 *
	 * @param enabled True if the toggle is enabled, false otherwise.
	 * @param name The base name of the toggle option (e.g., "Block Breaking").
	 * @return The ItemStack representing the toggle option.
	 */
	private ItemStack createToggleItem(boolean enabled, String name) {
		Material m = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
		String s = enabled ? "§aENABLED" : "§cDISABLED";
		return createGuiItem(m, "§f" + name, "§7Status: " + s, "§eClick to toggle.");
	}

	/**
	 * Creates an ItemStack specifically for a potion effect in the GUI.
	 * It stores the PotionEffectType's NamespacedKey in the item's persistent data.
	 *
	 * @param type The PotionEffectType to create the item for.
	 * @param hasEffect True if the arena currently has this effect, false otherwise.
	 * @return The ItemStack representing the potion effect.
	 */
	private ItemStack createEffectItem(PotionEffectType type, boolean hasEffect) {
		// Set item display name based on whether the effect is active or not
		String name = hasEffect ? "§a" + formatEffectName(type.getName()) : "§7" + formatEffectName(type.getName());
		// Set item lore based on whether the effect can be removed or added
		List<String> lore = hasEffect ? List.of("§cClick to REMOVE.") : List.of("§eClick to ADD.");
		ItemStack item = createGuiItem(Material.POTION, name, lore.toArray(new String[0]));

		ItemMeta meta = item.getItemMeta();
		// IMPORTANT: Store the PotionEffectType's NamespacedKey string in the item's Persistent Data Container.
		meta.getPersistentDataContainer().set(effectTypeKey, PersistentDataType.STRING, type.getKey().toString());

		// Add glow effect and hide enchant flags if the effect is active on the arena
		if (hasEffect) {
			meta.addEnchant(Enchantment.UNBREAKING, 1, true); // Dummy enchant for glow
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); // Hide the dummy enchant
		}
		item.setItemMeta(meta);
		return item;
	}

	/**
	 * Formats a raw potion effect name (e.g., "FAST_DIGGING") into a more readable string (e.g., "Fast Digging").
	 *
	 * @param name The raw potion effect name from PotionEffectType.getName().
	 * @return The formatted, human-readable name.
	 */
	private String formatEffectName(String name) {
		return Arrays.stream(name.split("_"))
				.map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}

	/**
	 * Creates a navigation item (e.g., arrow for next/previous page).
	 *
	 * @param name The display name of the navigation item.
	 * @param material The Material of the item.
	 * @return The ItemStack for the navigation item.
	 */
	private ItemStack createNavItem(String name, Material material) {
		return createGuiItem(material, name);
	}

	/**
	 * A generic helper method to create an ItemStack with a custom name and lore.
	 *
	 * @param material The Material of the item.
	 * @param name The display name of the item.
	 * @param loreLines Optional array of strings for the item's lore.
	 * @return The created ItemStack.
	 */
	private ItemStack createGuiItem(Material material, String name, String... loreLines) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		// Set display name, removing italics by default
		meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
		// Add lore lines, removing italics by default
		List<Component> lore = new ArrayList<>();
		for (String line : loreLines) {
			lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
		}
		meta.lore(lore);
		item.setItemMeta(meta);
		return item;
	}
}