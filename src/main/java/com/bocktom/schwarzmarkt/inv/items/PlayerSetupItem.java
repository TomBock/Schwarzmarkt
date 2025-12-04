package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.util.ItemUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public class PlayerSetupItem extends SetupItem {

	private final boolean inAuction;

	public PlayerSetupItem(int id, ItemStack item, int amount, boolean inAuction, Function<PickableItem, Boolean> onAdded, Function<PickableItem, Boolean> tryRemove) {
		super(id, item, amount, onAdded, tryRemove);
		this.inAuction = inAuction;

		customAddLore();
	}

	@Override
	protected List<String> getRawLore() {
		return MSG.getList("playersetup.item.lore.outofauction");
	}

	@Override
	protected void addLore() {
		// Ignore the default lore and add custom in-auction lore after init
	}

	@Override
	protected void handlePlace(@NotNull InventoryClickEvent event) {
		super.handlePlace(event);
		customAddLore();
	}

	private void customAddLore() {
		if(!inAuction) {
			super.addLore();
			return;
		}

		ItemMeta meta = item.getItemMeta();
		List<String> lore = ItemUtil.getLore(meta);
		amountLoreIndex = lore.size();
		lore.add(MSG.get("playersetup.item.lore.inauction"));
		ItemUtil.setLoreWithoutEvents(meta, lore);
		item.setItemMeta(meta);
	}

	@Override
	protected void cleanLore() {
		if(inAuction) {
			ItemMeta meta = item.getItemMeta();
			List<String> lore = ItemUtil.getLore(meta);
			if(lore.size() > amountLoreIndex) {
				lore.remove(amountLoreIndex);
				ItemUtil.setLoreWithoutEvents(meta, lore);
				item.setItemMeta(meta);
			}
			amountLoreIndex = -1; // Reset index since lore is removed
		} else {
			super.cleanLore();
		}
	}

	@Override
	public void handleClick(@NotNull ClickType clicktype, @NotNull Player player, @NotNull InventoryClickEvent event) {
		if(inAuction) {
			player.sendMessage(MSG.get("playersetup.inauction"));
		} else {
			super.handleClick(clicktype, player, event);
		}
	}

	@Override
	protected int getAddition() {
		return 100;
	}

	@Override
	protected int getSubtraction() {
		return -100;
	}

}
