package com.bocktom.schwarzmarkt;

import com.bocktom.schwarzmarkt.inv.Auction;
import com.bocktom.schwarzmarkt.inv.items.AuctionItem;
import com.bocktom.schwarzmarkt.util.Config;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import com.bocktom.schwarzmarkt.util.PersistentLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

public class AuctionManager {
	private static final int BID_TIME = 60 * 2; // 2 Minutes for players to bid via command

	private final Schwarzmarkt plugin;
	private final Map<Player, AuctionItem> biddingPlayers  = new HashMap<>();

	public AuctionManager() {
		this.plugin = Schwarzmarkt.plugin;
	}

	public void startAuctions(@Nullable Player player) {
		List<Auction> auctions = Schwarzmarkt.db.getAuctions();
		if(!auctions.isEmpty()) {
			sendMessage(MSG.get("auction.alreadyrunning"), player);
			return;
		}

		int auctionItems = Config.gui.get.getInt("auction.items");
		List<ItemStack> items = Schwarzmarkt.db.getRandomItems(auctionItems);
		List<Integer> auctionIds = Schwarzmarkt.db.addAuctions(items);

		if(!auctionIds.isEmpty())
			sendMessage(MSG.get("auction.started", "%amount%", String.valueOf(auctionItems)), player);
		else
			sendMessage(MSG.get("auction.error"), player);

		for (int i = 0; i < auctionIds.size(); i++) {
			PersistentLogger.logAuctionStart(auctionIds.get(i), items.get(i));
		}
	}

	public void stopAuctions(@Nullable Player player) {
		// Check if running
		List<Auction> auctions = Schwarzmarkt.db.getAuctions();
		if(auctions.isEmpty()) {
			sendMessage(MSG.get("auction.notrunning"), player);
			return;
		}

		Map<UUID, Integer> returnBids = processAuctionWinners(auctions);
		Map<UUID, Integer> successfulReturns = returnBidsToPlayers(returnBids);

		notifyOnlinePlayers(successfulReturns);
		Schwarzmarkt.db.addReturnedBids(successfulReturns);

		Schwarzmarkt.db.removeAuctions();
		Schwarzmarkt.db.removeBids(auctions.stream().map(auction -> auction.id).toList());

		sendMessage(MSG.get("auction.ended"), player);
	}

	private Map<UUID, Integer> processAuctionWinners(List<Auction> auctions) {
		Map<UUID, Integer> returnBids = new HashMap<>();

		for (Auction auction : auctions) {

			// Grant winnings
			if(auction.highestBidder != null) {
				processWinningBid(auction);
			}

			// Collect bids to return
			Schwarzmarkt.db.getBids(auction.id).forEach((uuid, amount) -> {
				if(!uuid.equals(auction.highestBidder)) {
					returnBids.merge(uuid, amount, Integer::sum);
				}
			});
		}
		return returnBids;
	}

	private Map<UUID, Integer> returnBidsToPlayers(Map<UUID, Integer> returnBids) {
		Map<UUID, Integer> successfulReturns = new HashMap<>(returnBids);

		for (Map.Entry<UUID, Integer> returnBid : returnBids.entrySet()) {
			EconomyResponse response = Schwarzmarkt.economy.depositPlayer(Bukkit.getOfflinePlayer(returnBid.getKey()), returnBid.getValue());
			if(response.type != EconomyResponse.ResponseType.SUCCESS) {
				PersistentLogger.logReturnBidFailed(returnBid.getKey(), returnBid.getValue());
				successfulReturns.remove(returnBid.getKey());
			}
		}
		return successfulReturns;
	}

	private void notifyOnlinePlayers(Map<UUID, Integer> successfulReturns) {
		// Check for online players and message them directly (no need to store in db)
		Iterator<Map.Entry<UUID, Integer>> iterator = successfulReturns.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, Integer> entry = iterator.next();
			Player onlinePlayer = Bukkit.getPlayer(entry.getKey());

			if (onlinePlayer != null) {
				onlinePlayer.sendMessage(MSG.get("onjoin.lost", "%amount%", String.valueOf(entry.getValue())));
				iterator.remove();
			}
		}
	}

	private void processWinningBid(Auction auction) {
		boolean winningsResult = Schwarzmarkt.db.addWinnings(auction.highestBidder, auction.item);
		if(!winningsResult) {
			PersistentLogger.logWinningsFailed(auction.id, auction.highestBidder, auction.item);
			return;
		}
		PersistentLogger.logAuctionEnd(auction.id, auction.highestBidder, auction.highestBid);

		// Inform directly
		Player winner = Bukkit.getPlayer(auction.highestBidder);
		if(winner != null) {
			winner.sendMessage(Component.text(MSG.get("onjoin.won")));
		}
	}

	private void sendMessage(String message, @Nullable Player player) {
		if(player != null) {
			player.sendMessage(message);
		} else {
			plugin.getLogger().info(message);
		}
	}

	public void bid(Player player, int amount) {
		AuctionItem auction = biddingPlayers.get(player);

		if(auction == null) {
			player.sendMessage(MSG.get("bid.noauction"));
			return;
		}

		// Check if player has enough money
		if(!hasEnoughBalance(player, amount)) {
			player.sendMessage(MSG.get("bid.broke"));
			return;
		}

		// Try to place bid in DB
		if(!Schwarzmarkt.db.placeBid(auction.id, player.getUniqueId(), amount)) {
			player.sendMessage(MSG.get("error"));
			return;
		}

		// Attempt to withdraw money
		if(!withdrawMoney(player, auction, amount)) {
			return;
		}

		// Bid successfully placed
		PersistentLogger.logBid(auction.id, player.getUniqueId(), amount);
		player.sendMessage(MSG.get("bid.success",
				"%amount%", String.valueOf(amount),
				"%item%", InvUtil.getName(auction.item)));

		biddingPlayers.remove(player);
	}

	private boolean withdrawMoney(Player player, AuctionItem auction, int amount) {
		EconomyResponse withdrawResponse = Schwarzmarkt.economy.withdrawPlayer(player, amount);
		if(withdrawResponse.type != EconomyResponse.ResponseType.SUCCESS) {
			player.sendMessage(MSG.get("error"));

			// Rollback if withdraw failed
			boolean rollbackResponse = Schwarzmarkt.db.rollbackBid(auction.id, player.getUniqueId(), amount);
			if(!rollbackResponse) {
				PersistentLogger.logBidRollbackFailed(auction.id, player.getUniqueId(), amount);
			}
			return false;
		}
		return true;
	}

	private boolean hasEnoughBalance(Player player, int amount) {
		return Schwarzmarkt.economy.getBalance(player) >= amount;
	}

	/**
	 * Registers a player for bidding via /biete
	 * The player is removed after 2 minutes, where has to select an AuctionItem again
	 */
	public void registerForBidding(Player player, AuctionItem auction) {
		biddingPlayers.put(player, auction);

		Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {

			// Check if player has changed auction
			AuctionItem curAuction = biddingPlayers.getOrDefault(player, new AuctionItem(-1));
			if(auction.id != curAuction.id) return;

			biddingPlayers.remove(player);
		}, 20 * BID_TIME);
	}
}
