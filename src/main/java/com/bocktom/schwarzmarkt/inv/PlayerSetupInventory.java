package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.IdItem;
import com.bocktom.schwarzmarkt.inv.items.PlayerSetupItem;
import com.bocktom.schwarzmarkt.util.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.Item;

import java.util.List;

public class PlayerSetupInventory extends SetupInventory {

	public PlayerSetupInventory(Player player, Player owner) {
		super(player, owner, "playersetup", MSG.get("playersetup.name"));
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
	protected Item getFallback() {
		return new PlayerSetupItem(-1, new ItemStack(Material.AIR), 100, false, this::tryAddItem, this::tryRemoveItem);
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
				if(!Schwarzmarkt.economy.withdrawMoney(player, cost, true)) {
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
			if(!Schwarzmarkt.economy.depositMoney(player, revenue, true)) {
				player.sendMessage(MSG.get("playersetup.deposit.revenuefailed"));
			}
		}

		if(!itemsAdded.isEmpty() || !itemsUpdated.isEmpty() || !itemsRemoved.isEmpty()) {
			Schwarzmarkt.db.updatePlayerItems(owner.getUniqueId(), itemsAdded, itemsUpdated, itemsRemoved, depositCost);

			int total = -cost + revenue;
			if(total > 0) {
				sendAfterSaveMessage("playersetup.deposit.earning", itemsAdded.size(), itemsRemoved.size(), total);
			} else if(total < 0){
				sendAfterSaveMessage("playersetup.deposit.cost", itemsAdded.size(), itemsRemoved.size(), -total);
			} else {
				sendAfterSaveMessage("playersetup.deposit.neutral", itemsAdded.size(), itemsRemoved.size(), total);
			}
		}
	}

	private void sendAfterSaveMessage(String msg, int itemsAdded, int itemsRemoved, int total) {
		player.sendMessage(MSG.get(msg,
				"%items_added%", String.valueOf(itemsAdded),
				"%items_removed%", String.valueOf(itemsRemoved),
				"%amount%", String.valueOf(total)));

	}
}
