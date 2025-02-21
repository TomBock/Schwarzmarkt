package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.IdItem;
import com.bocktom.schwarzmarkt.inv.items.PickableItem;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.List;
import java.util.Map;

public class WinningsInventory {

	private final Player player;

	public WinningsInventory(Player player) {

		Map<Integer, ItemStack> itemStackMap = Schwarzmarkt.db.getWinnings(player.getUniqueId());
		List<Item> items = InvUtil.createItems(itemStackMap,
				entry -> new PickableItem(entry.getKey(), entry.getValue(), null, this::tryItemRemove));

		//fill up to 9 with air
		for (int i = 0; i < 9 - items.size(); i++) {
			items.add(new SimpleItem(new ItemStack(Material.AIR)));
		}

		Gui gui = Gui.normal()
				.setStructure(
						"3 4 5",
						"2 0 1",
						"6 7 8")
				.addIngredient('0', items.get(0))
				.addIngredient('1', items.get(1))
				.addIngredient('2', items.get(2))
				.addIngredient('3', items.get(3))
				.addIngredient('4', items.get(4))
				.addIngredient('5', items.get(5))
				.addIngredient('6', items.get(6))
				.addIngredient('7', items.get(7))
				.addIngredient('8', items.get(8))
				.build();

		Window window = Window.single()
				.setViewer(player)
				.setTitle(MSG.get("winnings.title"))
				.setGui(gui)
				.build();

		window.open();
		this.player = player;
	}

	private boolean tryItemRemove(IdItem item) {
		boolean removed = Schwarzmarkt.db.removeWinnings(item.id);
		if(!removed) {
			player.sendMessage(MSG.get("error"));
		}
		return true;
	}
}
