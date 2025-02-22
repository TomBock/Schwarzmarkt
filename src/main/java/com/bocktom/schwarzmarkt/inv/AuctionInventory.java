package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.AuctionItem;
import com.bocktom.schwarzmarkt.inv.items.CloseItem;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.window.Window;

import java.util.List;

public class AuctionInventory {

	private final Player player;

	public AuctionInventory(Player player) {

		List<Auction> auctions = Schwarzmarkt.db.getAuctions();
		List<Item> items = InvUtil.createItems(auctions,
				auction -> new AuctionItem(auction.id, auction.item, this::onBid));

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
		// Check if player already bid
		int amount = Schwarzmarkt.db.getBid(item.id, player.getUniqueId());

		player.closeInventory();

		TextComponent msg = Component.empty()
				.content(MSG.get("bid.info", "%item%", InvUtil.getName(item.item)))
				.clickEvent(ClickEvent.suggestCommand("/schwarzmarkt bieten "));
		player.sendMessage(msg);

		if(amount > 0) {
			player.sendMessage(MSG.get("bid.currentbid", "%amount%", String.valueOf(amount)));
		}

		Schwarzmarkt.plugin.registerForBidding(player, item);
	}

	private void onClose() {

	}

}
