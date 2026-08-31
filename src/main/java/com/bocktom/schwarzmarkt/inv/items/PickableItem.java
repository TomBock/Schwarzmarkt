package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.PersistentLogger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.InvUI;

import java.util.function.Function;

/**
 * An item the player can physically place into and take out of the gui.
 * <p>
 * InvUI 2 no longer hands the {@code InventoryClickEvent} to items - {@link Click} only
 * carries the player and the click type, and every click on an item is cancelled by the
 * library. Place and pickup are therefore driven off the player's cursor here instead of
 * off {@code InventoryAction}, and the item is moved explicitly rather than by letting
 * vanilla run through an uncancelled event.
 */
public class PickableItem extends IdItem {
	public static final NamespacedKey SLOT_KEY = new NamespacedKey(InvUI.getInstance().getPlugin(), "slot");

	protected final Function<PickableItem, Boolean> tryAdd;
	protected final Function<PickableItem, Boolean> tryRemove;

	public PickableItem(int id, ItemStack item, Function<PickableItem, Boolean> onAdded, Function<PickableItem, Boolean> tryRemove) {
		super(id, item);
		this.tryAdd = onAdded;
		this.tryRemove = tryRemove;
	}

	@Override
	public void handleClick(@NotNull ClickType clicktype, @NotNull Player player, @NotNull Click click) {

		ItemStack cursor = player.getItemOnCursor();
		boolean hasCursor = cursor != null && cursor.getType() != Material.AIR;

		if(hasCursor && InvUtil.isPlaceClick(clicktype, isPartialClickAllowed())) {

			handlePlace(player, cursor);
			notifyWindows();

		} else if(!hasCursor && InvUtil.isPickupClick(clicktype, isPartialClickAllowed())) {

			handlePickup(player, clicktype);
			notifyWindows();
		}
	}

	/**
	 * Strips the slot marker that InvUI stamps onto rendered items.
	 * <p>
	 * {@code AbstractWindow} writes a "slot" key into the item's persistent data - InvUI 1
	 * via ItemMeta, InvUI 2 via {@code ItemStack#editPersistentDataContainer} - namespaced
	 * with the plugin InvUI was initialised with, so it shows up as
	 * {@code phoenixschwarzmarkt:slot}. Without stripping it the marker travels with the
	 * item into player inventories and, from there, back into the database.
	 */
	protected ItemStack stripInternalData(ItemStack stack) {
		if(stack == null || stack.getType() == Material.AIR)
			return stack;

		ItemMeta meta = stack.getItemMeta();
		if(meta == null)
			return stack;

		meta.getPersistentDataContainer().remove(SLOT_KEY);
		stack.setItemMeta(meta);
		return stack;
	}

	protected boolean isPartialClickAllowed() {
		return true;
	}

	protected void handlePlace(@NotNull Player player, @NotNull ItemStack cursor) {
		ItemStack previous = stripInternalData(getCleanItem());
		// Stripped on the way in as well, so the marker never reaches the database
		item = stripInternalData(cursor.clone());
		if(tryAdd != null && tryAdd.apply(this)) {
			player.setItemOnCursor(null);
			if(previous != null && previous.getType() != Material.AIR) {
				// Entity scheduler: touching a player's inventory has to happen on the
				// thread owning that player, which under Folia is their region's.
				player.getScheduler().run(Schwarzmarkt.plugin, task -> {
					player.getInventory().addItem(previous);
					notifyWindows();
				}, null);
			}
		} else {
			item = previous;
		}
	}

	protected boolean handlePickup(@NotNull Player player, @NotNull ClickType clickType) {
		return handlePickup(player, clickType, true);
	}

	/**
	 * @param handOverItem whether the item should end up on the player's cursor. Titles are
	 *                     consumed in exchange for a permission and must not be handed out,
	 *                     otherwise the player keeps the name tag on top of the title.
	 */
	protected boolean handlePickup(@NotNull Player player, @NotNull ClickType clickType, boolean handOverItem) {
		if(tryRemove != null && tryRemove.apply(this)) {
			// Subclasses clean the item (e.g. strip setup lore) before delegating here
			ItemStack picked = handOverItem ? stripInternalData(item == null ? null : item.clone()) : null;
			boolean toInventory = clickType == ClickType.SHIFT_LEFT;
			id = -1;
			player.getScheduler().run(Schwarzmarkt.plugin, task -> {
				if(picked != null && picked.getType() != Material.AIR) {
					handOver(player, picked, toInventory);
				}
				item = new ItemStack(Material.AIR);
				notifyWindows();
			}, () -> {
				// The row is deleted by now and the player is gone before the handover, so
				// all that is left is a record of what has to be restored by hand.
				if(picked != null && picked.getType() != Material.AIR)
					PersistentLogger.logPickupLost(player, picked);
			});
			return true;
		}
		return false;
	}

	/**
	 * Puts the item where vanilla would have put it: a shift click moves it into the
	 * inventory, a plain click takes it onto the cursor.
	 * <p>
	 * InvUI 2 cancels every click on an item, so that split has to be made here. Without it
	 * a shift click ended up on the cursor as well - a player who shift clicks never looks
	 * there, closes the window, and the item drops at their feet unnoticed while its
	 * database row is already gone.
	 * <p>
	 * Whatever does not fit is dropped rather than discarded, for the same reason: at this
	 * point the item exists nowhere else.
	 */
	private void handOver(@NotNull Player player, @NotNull ItemStack stack, boolean toInventory) {
		if(!toInventory && player.getItemOnCursor().getType() == Material.AIR) {
			player.setItemOnCursor(stack);
			return;
		}

		for (ItemStack rest : player.getInventory().addItem(stack).values())
			player.getWorld().dropItem(player.getEyeLocation(), rest);
	}

	public ItemStack getCleanItem() {
		return item;
	}
}
