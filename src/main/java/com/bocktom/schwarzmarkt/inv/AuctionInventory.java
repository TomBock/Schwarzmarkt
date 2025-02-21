package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.inv.items.CloseItem;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.window.Window;

public class AuctionInventory {

	private Player player;

	public AuctionInventory(Player player) {

		Gui gui = Gui.normal()
				.setStructure(
						"# # # # # # # # #",
						"# # # x x x # # #",
						"# # # # # # # # #",
						"# # # # c # # # #")
				.addIngredient('#', InvUtil.BORDER)
				.addIngredient('x', InvUtil.AIR)
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

	private void onClose() {

	}

}
