package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.IdItem;
import com.bocktom.schwarzmarkt.inv.items.PlayerSetupItem;
import com.bocktom.schwarzmarkt.util.*;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.item.Item;

import java.util.List;

public class PlayerSetupInventory extends SetupInventory {

	private final Player owner;

	public PlayerSetupInventory(Player player, Player owner) {
		super(player, "playersetup", MSG.get("playersetup.name"));
		this.owner = owner;
	}

	@Override
	protected List<Item> getItems() {
		List<PlayerDbItem> dbItems = Schwarzmarkt.db.getPlayerItems(owner.getUniqueId());

		List<Item> items = InvUtil.createItems(dbItems,
				dbItem -> new PlayerSetupItem(dbItem.id, dbItem.item, dbItem.amount, dbItem.inAuction, this::tryAddItem, this::tryRemoveItem));

		// One fallback to always have space for new items
		int maxItems = Config.gui.get.getInt("playersetup.maxitems");
		int additions = Math.min(maxItems - items.size(), maxItems);
		for (int i = 0; i < additions; i++) {
			items.add(getFallback());
		}
		return items;
	}

	@Override
	protected boolean tryAddItem(IdItem item) {
		boolean hasCooldown = Schwarzmarkt.db.hasItemCooldown(item.item);
		if(hasCooldown) {
			player.sendMessage(MSG.get("playersetup.cooldown"));
			return false;
		}
		return true;
	}

	@Override
	protected void saveItems(List<DbItem> itemsAdded, List<DbItem> itemsUpdated) {
		int depositCost = Config.gui.get.getInt("playersetup.deposit");

		int cost = itemsAdded.size() * depositCost;
		int revenue = itemsRemoved.size() * depositCost;

		boolean returnAddedItems = false;

		if(cost > 0) {
			if(!Schwarzmarkt.economy.hasEnoughBalance(player, cost)) {
				player.sendMessage(MSG.get("playersetup.notenoughmoney"));
				returnAddedItems = true;
			} else {
				if(!Schwarzmarkt.economy.withdrawMoney(player, cost)) {
					player.sendMessage(MSG.get("playersetup.deposit.withdrawfailed"));
					returnAddedItems = true;
				}
			}
		}

		if(returnAddedItems) {
			// Return added items to the player
			for (DbItem item : itemsAdded) {
				PlayerUtil.give(player, item.item);
			}
			itemsAdded.clear();
		}

		if(revenue > 0) {
			if(!Schwarzmarkt.economy.depositMoney(player, revenue)) {
				player.sendMessage(MSG.get("playersetup.deposit.revenuefailed"));
			}
		}

		if(!itemsAdded.isEmpty() || !itemsUpdated.isEmpty() || !itemsRemoved.isEmpty()) {
			Schwarzmarkt.db.updatePlayerItems(owner.getUniqueId(), itemsAdded, itemsUpdated, itemsRemoved);
			player.sendMessage(MSG.get("playersetup.deposit.success"));
		}
	}
}
