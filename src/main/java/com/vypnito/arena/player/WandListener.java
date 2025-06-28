package com.vypnito.arena.player; // Měl by být ve stejném balíčku jako SelectionManager

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block; // Přidáno pro kontrolu bloku
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action; // Přidáno pro typ kliknutí
import org.bukkit.event.player.PlayerInteractEvent; // Přidáno pro událost interakce
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener třída, která obsluhuje interakce hráče s výběrovou hůlkou.
 * Nastavuje pozice 1 a 2 a zabraňuje ničení bloků.
 */
public class WandListener implements Listener {

	private final SelectionManager selectionManager;

	/**
	 * Konstruktor pro WandListener.
	 * @param selectionManager Instance SelectionManageru pro správu pozic.
	 */
	public WandListener(SelectionManager selectionManager) {
		this.selectionManager = selectionManager;
	}

	/**
	 * Obsluhuje událost interakce hráče. Zjišťuje, zda hráč klikl s výběrovou hůlkou.
	 * @param event Událost PlayerInteractEvent.
	 */
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack itemInHand = player.getInventory().getItemInMainHand(); // Získá předmět v ruce

		// Zkontrolujeme, zda hráč drží předmět, který by mohl být hůlkou
		if (itemInHand.getType() != Material.WOODEN_AXE) {
			return; // Není to dřevěná sekera, takže to není hůlka
		}

		ItemMeta meta = itemInHand.getItemMeta();
		if (meta == null) {
			return; // Předmět nemá metadata
		}

		// Zkontrolujeme, zda předmět má náš speciální NamespacedKey
		Boolean isWand = meta.getPersistentDataContainer().get(selectionManager.getWandKey(), PersistentDataType.BOOLEAN);
		if (isWand == null || !isWand) {
			return; // Není to naše výběrová hůlka
		}

		// Pokud jsme se dostali sem, hráč drží naši výběrovou hůlku. Zrušíme událost,
		// aby nedošlo k ničení/umísťování bloků.
		event.setCancelled(true);

		// Zpracování kliknutí na blok
		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock == null) {
			return; // Neklikl na blok
		}

		// Získání lokace kliknutého bloku
		Location clickedLocation = clickedBlock.getLocation();

		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			// Levé kliknutí nastavuje pozici 1
			selectionManager.setPos1(player, clickedLocation);
			player.sendMessage(Component.text("Position 1 set to: " + formatLoc(clickedLocation), NamedTextColor.GREEN));
		} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			// Pravé kliknutí nastavuje pozici 2
			selectionManager.setPos2(player, clickedLocation);
			player.sendMessage(Component.text("Position 2 set to: " + formatLoc(clickedLocation), NamedTextColor.GREEN));
		}
	}

	/**
	 * Pomocná metoda pro formátování lokace do čitelného řetězce.
	 * @param loc Lokace k formátování.
	 * @return Formátovaný řetězec lokace.
	 */
	private String formatLoc(Location loc) {
		if (loc == null) return "Not Set";
		return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
	}
}