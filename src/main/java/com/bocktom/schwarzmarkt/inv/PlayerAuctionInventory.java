package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.PlayerAuctionItem;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.item.Item;

import java.util.List;
import java.util.Map;

public class PlayerAuctionInventory extends AuctionInventory {

	public PlayerAuctionInventory(Player player) {
		super(player, "playerauction", MSG.get("playerauction.name"));
	}


	@Override
	protected List<Item> getItems() {
		List<PlayerAuction> auctions = Schwarzmarkt.db.getPlayerAuctions();
		Map<Integer, Integer> bidsPerAuction = Schwarzmarkt.db.getPlayerAuctionBids(player.getUniqueId());

		return InvUtil.createItems(auctions,
				auction -> new PlayerAuctionItem(
						auction.id,
						auction.item,
						auction.ownerId,
						auction.minBid,
						auction.deposit,
						bidsPerAuction.getOrDefault(auction.id, 0),
						auction.highestBid,
						this::onBid));
	}
}
