package com.vypnito.arena.gui;

import com.vypnito.arena.arenas.Arenas;
import com.vypnito.arena.arenas.ArenaManager;
import com.vypnito.arena.player.PlayerManager;
import com.vypnito.arena.player.PlayerState;
import com.vypnito.arena.player.SelectionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Listener class for processing GUI clicks.
 * This class dispatches events to specific GUI classes (SetupGUI, EditGUI, EffectsGUI).
 */
public class GUIListener implements Listener {
	private final ArenaManager arenaManager;
	private final PlayerManager playerManager;
	private final SelectionManager selectionManager;
	private final SetupGUI setupGUI;
	private final EditGUI editGUI;
	private final EffectsGUI effectsGUI;

	/**
	 * Constructor for GUIListener.
	 * @param arenaManager ArenaManager instance.
	 * @param playerManager PlayerManager instance.
	 * @param selectionManager SelectionManager instance.
	 * @param setupGUI SetupGUI instance.
	 * @param editGUI EditGUI instance.
	 * @param effectsGUI EffectsGUI instance.
	 */
	public GUIListener(ArenaManager arenaManager, PlayerManager playerManager, SelectionManager selectionManager, SetupGUI setupGUI, EditGUI editGUI, EffectsGUI effectsGUI) {
		this.arenaManager = arenaManager;
		this.playerManager = playerManager;
		this.selectionManager = selectionManager;
		this.setupGUI = setupGUI;
		this.editGUI = editGUI;
		this.effectsGUI = effectsGUI;
	}

	/**
	 * Main handler for InventoryClickEvent.
	 * Identifies the GUI and dispatches the event to the appropriate method.
	 * @param event The inventory click event.
	 */
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		// Check if the clicking entity is a player
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}

		String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
		if (title.startsWith(SetupGUI.GUI_TITLE_PREFIX)) handleSetupGUIClick(event, title);
		else if (title.startsWith(EditGUI.GUI_TITLE_PREFIX)) handleEditGUIClick(event, title);
		else if (title.startsWith(EffectsGUI.GUI_TITLE_PREFIX)) handleEffectsGUIClick(event, title);
	}

	/**
	 * Handles clicks in the SetupGUI (Arena Creation GUI).
	 * @param event The click event.
	 * @param title The GUI title.
	 */
	private void handleSetupGUIClick(InventoryClickEvent event, String title) {
		event.setCancelled(true); // Cancel the event to prevent item manipulation
		Player player = (Player) event.getWhoClicked();
		String arenaName = title.substring(SetupGUI.GUI_TITLE_PREFIX.length());
		if (event.getCurrentItem() == null) return; // Ignore clicks on empty slots

		switch (event.getCurrentItem().getType()) {
			case CLOCK -> setupGUI.open(player, arenaName); // Refresh GUI
			case EMERALD_BLOCK -> { // "Create Arena" button
				Location pos1 = selectionManager.getPos1(player);
				Location pos2 = selectionManager.getPos2(player);
				// Note: This GUIListener's SetupGUI.open doesn't pass requiredPlayers.
				// If you are using GUIManager's openSetupGUI for actual arena creation flow,
				// this part might need adjustment or is only for display.
				// For consistency with GameManager, a default of 2 players will be used if not specified.
				int requiredPlayers = 2; // Default if not passed through GUI flow

				if (pos1 != null && pos2 != null) {
					arenaManager.createArena(arenaName, pos1, pos2, requiredPlayers); // Passing default requiredPlayers
					selectionManager.clearSelection(player);
					player.sendMessage(Component.text("Arena '" + arenaName + "' created! Use '/arena edit " + arenaName + "' to configure.", NamedTextColor.AQUA));
					player.closeInventory();
				} else {
					player.sendMessage(Component.text("You must set both positions with the wand first.", NamedTextColor.RED));
				}
			}
		}
	}

	/**
	 * Handles clicks in the EditGUI (Arena Editing GUI).
	 * @param event The click event.
	 * @param title The GUI title.
	 */
	private void handleEditGUIClick(InventoryClickEvent event, String title) {
		event.setCancelled(true); // Cancel the event
		Player player = (Player) event.getWhoClicked();
		String arenaName = title.substring(EditGUI.GUI_TITLE_PREFIX.length());
		Arenas arena = arenaManager.getArena(arenaName);
		if (arena == null || event.getCurrentItem() == null) { player.closeInventory(); return; }

		Material clicked = event.getCurrentItem().getType();
		int clickedSlot = event.getSlot();

		switch (clicked) {
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
				editGUI.open(player, arena);
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
			case POTION -> effectsGUI.open(player, arena, 0);

			case BARRIER -> {
				if (clickedSlot == 49) {
					playerManager.clearPlayerState(player);
					player.closeInventory();
					player.sendMessage(Component.text("Arena editor closed.", NamedTextColor.AQUA));
				}
			}
		}
	}

	/**
	 * Handles clicks in the EffectsGUI (Arena Effects Management GUI).
	 * @param event The click event.
	 * @param title The GUI title.
	 */
	private void handleEffectsGUIClick(InventoryClickEvent event, String title) {
		event.setCancelled(true);
		Player player = (Player) event.getWhoClicked();
		String arenaName = title.substring(EffectsGUI.GUI_TITLE_PREFIX.length()).split(" \\(Page")[0];
		Arenas arena = arenaManager.getArena(arenaName);
		if (arena == null || event.getCurrentItem() == null) { player.closeInventory(); return; }

		ItemStack clickedItem = event.getCurrentItem();
		int currentPage = Integer.parseInt(title.replaceAll("[^0-9]", "")) - 1;

		switch (clickedItem.getType()) {
			case ARROW -> {
				if (PlainTextComponentSerializer.plainText().serialize(clickedItem.displayName()).contains("Next")) { effectsGUI.open(player, arena, currentPage + 1); }
				else { effectsGUI.open(player, arena, currentPage - 1); }
			}
			case BARRIER -> editGUI.open(player, arena);
			case POTION -> {
				// This part relies on createEffectItem in EffectsGUI.java or GUIManager.java
				// storing NamespacedKey in Persistent Data Container.
				// If not, it will parse the item name, which is less reliable.
				String effectName = PlainTextComponentSerializer.plainText().serialize(clickedItem.displayName()).toUpperCase().replace(" ", "_").replace("§A", "").replace("§7", "");
				PotionEffectType type = PotionEffectType.getByName(effectName);
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
				effectsGUI.open(player, arena, currentPage);
			}
		}
	}

	/**
	 * Formats an effect name (e.g., "FAST_DIGGING" to "Fast Digging").
	 * @param name The original effect name.
	 * @return The formatted name.
	 */
	private String formatEffectName(String name) {
		return Arrays.stream(name.split("_"))
				.map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}
}