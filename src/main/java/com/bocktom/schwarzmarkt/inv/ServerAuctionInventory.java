package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.ServerAuctionItem;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.item.Item;

import java.util.List;
import java.util.Map;

public class ServerAuctionInventory extends AuctionInventory {

	public ServerAuctionInventory(Player player) {
		super(player, "auction", MSG.get("auction.name"));
	}

	@Override
	protected List<Item> getItems() {
		List<Auction> auctions = Schwarzmarkt.db.getServerAuctions();
		Map<Integer, Integer> bidsPerAuction = Schwarzmarkt.db.getServerAuctionBids(player.getUniqueId());

		return InvUtil.createItems(auctions,
				auction -> new ServerAuctionItem(auction.id, auction.item, bidsPerAuction.getOrDefault(auction.id, 0), this::onBid));
	}

}
