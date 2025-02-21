package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.AuctionItem;
import com.bocktom.schwarzmarkt.inv.items.CloseItem;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.window.Window;

import java.util.List;
import java.util.Map;

public class AuctionInventory {

	private final Player player;

	public AuctionInventory(Player player) {

		Map<Integer, ItemStack> itemStacks = Schwarzmarkt.db.getAuctions();
		List<Item> items = InvUtil.createItems(itemStacks,
				entry -> new AuctionItem(entry.getKey(), entry.getValue(), this::onBid));

		for (int i = items.size(); i < 3; i++) {
			items.add(InvUtil.AIR);
		}

		Gui gui = Gui.normal()
				.setStructure(
						"# # # # # # # # #",
						"# # # 0 1 2 # # #",
						"# # # # # # # # #",
						"# # # # c # # # #")
				.addIngredient('#', InvUtil.BORDER)
				.addIngredient('0', items.get(0))
				.addIngredient('1', items.get(1))
				.addIngredient('2', items.get(2))
				.addIngredient('c', new CloseItem())
				.build();

		Window window = Window.single()
				.setViewer(player)
				.setTitle(MSG.get("auction.title"))
				.setGui(gui)
				.addCloseHandler(this::onClose)
				.build();

		window.open();
		this.player = player;
	}

	private void onBid(AuctionItem item) {



	}

	private void onClose() {

	}

}
