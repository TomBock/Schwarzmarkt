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
import java.util.Map;

public class AuctionInventory {

	private final Player player;

	public AuctionInventory(Player player) {
		List<Auction> auctions = Schwarzmarkt.db.getAuctions();
		Map<Integer, Integer> bidsPerAuction = Schwarzmarkt.db.getBids(player.getUniqueId());

		List<Item> items = InvUtil.createItems(auctions,
				auction -> new AuctionItem(auction.id, auction.item, bidsPerAuction.getOrDefault(auction.id, 0), this::onBid));

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
				.setTitle(MSG.get("auction.name"))
				.setGui(gui)
				.addCloseHandler(this::onClose)
				.build();

		window.open();
		this.player = player;
	}

	private void onBid(AuctionItem item) {
		player.closeInventory();

		TextComponent msg;
		if(item.currentBid == 0) {
			msg = Component.empty()
					.content(MSG.get("bid.info", "%item%", InvUtil.getName(item.item)))
					.clickEvent(ClickEvent.suggestCommand("/schwarzmarkt bieten "));
		} else {
			msg = Component.empty()
					.content(MSG.get("bid.infowithbid",
							"%item%", InvUtil.getName(item.item),
							"%amount%", String.valueOf(item.currentBid)))
					.clickEvent(ClickEvent.suggestCommand("/schwarzmarkt bieten "));
		}

		player.sendMessage(msg);
		Schwarzmarkt.auctions.registerForBidding(player, item);
	}

	private void onClose() {

	}

}
