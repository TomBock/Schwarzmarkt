package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.AuctionItem;
import com.bocktom.schwarzmarkt.inv.items.PlayerAuctionItem;
import com.bocktom.schwarzmarkt.inv.items.ServerAuctionItem;
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
		super(player, player.getUniqueId(), "auction", MSG.get("auction.name"));
	}

	protected void onBid(AuctionItem item) {
		player.closeInventory();

		if(item.isPlayerAuction()) {
			if(((PlayerAuctionItem) item).ownerUuid.equals(player.getUniqueId())) {
				player.sendMessage(MSG.get("bid.ownauction"));
				return;
			}
		}

		TextComponent msg;
		if(item.currentBid == 0) {
			msg = MSG.get("bid.info",
							Component.text("%item%"), InvUtil.getName(item.item))
					.clickEvent(ClickEvent.suggestCommand("/schwarzmarkt bieten "));
		} else {
			msg = MSG.get("bid.infowithbid",
							Component.text("%item%"), InvUtil.getName(item.item),
							Component.text("%amount%"), Component.text(item.currentBid))
					.clickEvent(ClickEvent.suggestCommand("/schwarzmarkt bieten "));
		}

		player.sendMessage(msg);
		Schwarzmarkt.auctions.registerForBidding(player, item);
	}

	@Override
	protected List<Item> getItems() {
		List<Auction> auctions = Schwarzmarkt.db.getServerAuctions();
		Map<Integer, Integer> bidsPerAuction = Schwarzmarkt.db.getServerAuctionBids(player.getUniqueId());

		return InvUtil.createItems(auctions,
				auction -> new ServerAuctionItem(auction.id, auction.item, bidsPerAuction.getOrDefault(auction.id, 0), this::onBid));
	}

	@Override
	protected List<Item> getItems2() {
		List<PlayerAuction> auctions = Schwarzmarkt.db.getPlayerAuctions();
		Map<Integer, Integer> bidsPerAuction = Schwarzmarkt.db.getPlayerAuctionBids(player.getUniqueId());

		return InvUtil.createItems(auctions,
				auction -> new PlayerAuctionItem(
						auction.id,
						auction.item,
						auction.ownerId,
						auction.ownerName,
						auction.minBid,
						auction.deposit,
						bidsPerAuction.getOrDefault(auction.id, 0),
						auction.highestBid,
						this::onBid));
	}
}
