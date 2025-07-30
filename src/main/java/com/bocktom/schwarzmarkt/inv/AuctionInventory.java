package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.AuctionItem;
import com.bocktom.schwarzmarkt.inv.items.ServerAuctionItem;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

public abstract class AuctionInventory extends ConfigInventory {

	public AuctionInventory(Player player, String config, String configName) {
		super(player, config, configName);
	}

	protected void onBid(AuctionItem item) {
		player.closeInventory();

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
}
