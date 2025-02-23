package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.AuctionItem;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.item.Item;

import java.util.List;
import java.util.Map;

public class AuctionInventory extends ConfigInventory {

	public AuctionInventory(Player player) {
		super(player, "auction", MSG.get("auction.name"));
	}

	@Override
	protected List<Item> getItems() {
		List<Auction> auctions = Schwarzmarkt.db.getAuctions();
		Map<Integer, Integer> bidsPerAuction = Schwarzmarkt.db.getBids(player.getUniqueId());

		return InvUtil.createItems(auctions,
				auction -> new AuctionItem(auction.id, auction.item, bidsPerAuction.getOrDefault(auction.id, 0), this::onBid));
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
}
