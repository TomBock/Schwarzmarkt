package com.bocktom.schwarzmarkt;

import com.bocktom.schwarzmarkt.inv.Auction;
import com.bocktom.schwarzmarkt.inv.AuctionInventory;
import com.bocktom.schwarzmarkt.inv.SetupInventory;
import com.bocktom.schwarzmarkt.inv.WinningsInventory;
import com.bocktom.schwarzmarkt.inv.items.AuctionItem;
import com.bocktom.schwarzmarkt.util.Config;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import com.bocktom.schwarzmarkt.util.PersistentLogger;
import de.tr7zw.changeme.nbtapi.NBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Schwarzmarkt extends JavaPlugin {

	private static final int BID_TIME = 60 * 2; // 2 Minutes for players to bid via command
	private static final int AUCTION_ITEMS = 3; // Remember to update inventory if changed

	public static Schwarzmarkt plugin;

	public static DatabaseManager db;
	public static Config config;
	private Economy economy;

	private final Map<Player, AuctionItem> biddingPlayers  = new HashMap<>();

	@Override
	public void onEnable() {
		plugin = this;
		config = new Config();
		db = new DatabaseManager();

		//noinspection DataFlowIssue
		getCommand("schwarzmarkt").setExecutor(new SchwarzmarktCommand());
		getServer().getPluginManager().registerEvents(new PlayerListener(), this);

		NBT.preloadApi();
		PersistentLogger.init();

		if(!setupEconomy()) {
			getServer().getPluginManager().disablePlugin(this);
		}
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			getLogger().severe("Vault not found! Disabling plugin...");
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			getLogger().severe("No Economy Service found. Disabling plugin...");
			return false;
		}
		economy = rsp.getProvider();
		return true;
	}

	public void openAuction(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		if(player != null) {
			new AuctionInventory(player);
		}
	}

	public void openSetup(Player player) {
		new SetupInventory(player);
	}

	public void openInfo(CommandSender sender) {
		List<Auction> auctions = db.getAuctions();

		sender.sendMessage("§aAuctions active: " + auctions.size());
		for (Auction auction : auctions) {
			Map<UUID, Integer> bids = db.getBids(auction.id);
			StringBuilder msg = new StringBuilder("Auction " + auction.id + " for " + InvUtil.getName(auction.item));

			for (Map.Entry<UUID, Integer> bidEntry : bids.entrySet()) {
				msg.append("\n  §8").append(Bukkit.getOfflinePlayer(bidEntry.getKey()).getName()).append(": §e").append(bidEntry.getValue());
			}
			sender.sendMessage(msg.toString());
		}
	}

	public void openWinnings(Player player) {
		new WinningsInventory(player);
	}

	public void startAuction(@Nullable Player player) {
		// Check if running
		List<Auction> auctions = db.getAuctions();
		if(!auctions.isEmpty()) {
			if(player != null) {
				player.sendMessage("§cAuction already running");
			} else {
				getLogger().warning("Auction already running");
			}
			return;
		}

		// Start Auction
		List<ItemStack> items = db.getRandomItems(AUCTION_ITEMS);
		List<Integer> auctionIds = db.addAuctions(items);
		if(player != null) {
			if(!auctionIds.isEmpty())
				player.sendMessage("§aAuctions started (" + auctionIds.size() + "/" + AUCTION_ITEMS + ")");
			else
				player.sendMessage("§cFailed to start any auction. See console for errors");
		}
		for (int i = 0; i < auctionIds.size(); i++) {
			PersistentLogger.logAuctionStart(auctionIds.get(i), items.get(i));
		}
	}

	public void stopAuction(@Nullable Player player) {
		// Check if running
		List<Auction> auctions = db.getAuctions();
		if(auctions.isEmpty()) {
			if(player != null) {
				player.sendMessage("§cAuction not running");
			} else {
				getLogger().warning("Auction not running");
			}
			return;
		}

		Map<UUID, Integer> returnBids = new HashMap<>();
		for (Auction auction : auctions) {

			// Grant item to winner
			if(auction.highestBidder != null) {
				boolean winningsResult = db.addWinnings(auction.highestBidder, auction.item);
				if(!winningsResult)
				{
					PersistentLogger.logWinningsFailed(auction.id, auction.highestBidder, auction.item);
				} else {
					PersistentLogger.logAuctionEnd(auction.id, auction.highestBidder, auction.highestBid);

					// Inform directly
					Player winner = Bukkit.getPlayer(auction.highestBidder);
					if(winner != null) {
						winner.sendMessage(Component.text(MSG.get("onjoin.won")).clickEvent(ClickEvent.runCommand("/schwarzmarkt gewinne")));
					}
				}
			}

			// Collect bids to return
			Map<UUID, Integer> loosers = db.getBids(auction.id);
			for (Map.Entry<UUID, Integer> looser : loosers.entrySet()) {
				if(looser.getKey().equals(auction.highestBidder)) continue;
				returnBids.put(looser.getKey(), returnBids.getOrDefault(looser.getKey(), 0) + looser.getValue());
			}
		}

		// Return bids
		Map<UUID, Integer> returnBidsSuccessful = new HashMap<>(returnBids);
		for (Map.Entry<UUID, Integer> returnBid : returnBids.entrySet()) {
			EconomyResponse response = economy.depositPlayer(Bukkit.getOfflinePlayer(returnBid.getKey()), returnBid.getValue());
			if(response.type == EconomyResponse.ResponseType.FAILURE) {
				PersistentLogger.logReturnBidFailed(returnBid.getKey(), returnBid.getValue());
				returnBidsSuccessful.remove(returnBid.getKey());
			}
		}

		// Check for online players and message them directly (no need to store in db)
		Map<UUID, Integer> returnBidsSuccessful2 = new HashMap<>(returnBidsSuccessful);
		for (Map.Entry<UUID, Integer> entry : returnBidsSuccessful.entrySet()) {
			Player onlinePlayer = Bukkit.getPlayer(entry.getKey());
			if(onlinePlayer != null) {
				returnBidsSuccessful2.remove(entry.getKey());
				onlinePlayer.sendMessage(MSG.get("onjoin.lost", "%amount%", String.valueOf(entry.getValue())));
			}
		}

		// Add to db for message
		db.addReturnedBids(returnBidsSuccessful2);

		// Remove auctions
		db.removeAuctions();
		db.removeBids(auctions.stream().map(auction -> auction.id).toList());

		if(player != null)
			player.sendMessage("§aAuctions stopped");
		else
			getLogger().info("Auctions stopped");
	}

	public void bid(Player player, int amount) {
		// Check if the player selected an Auction
		if(!biddingPlayers.containsKey(player)) {
			player.sendMessage(MSG.get("bid.noauction"));
			return;
		}
		AuctionItem auction = biddingPlayers.get(player);

		// Check Economy
		double balance = economy.getBalance(player);

		// Check if player has enough money
		if(balance < amount) {
			player.sendMessage(MSG.get("bid.broke"));
			return;
		}

		// Update db
		boolean dbResponse = db.placeBid(auction.id, player.getUniqueId(), amount);
		if(!dbResponse) {
			player.sendMessage(MSG.get("error"));
			return;
		}

		// Withdraw Money
		EconomyResponse withdrawResponse = economy.withdrawPlayer(player, amount);
		if(withdrawResponse.type == EconomyResponse.ResponseType.FAILURE) {
			player.sendMessage(MSG.get("error"));

			// Rollback if withdraw failed
			boolean rollbackResponse = db.rollbackBid(auction.id, player.getUniqueId(), amount);
			if(!rollbackResponse) {
				PersistentLogger.logBidRollbackFailed(auction.id, player.getUniqueId(), amount);
			}
			return;
		}
		// Worked fine
		PersistentLogger.logBid(auction.id, player.getUniqueId(), amount);
		player.sendMessage(MSG.get("bid.success",
				"%amount%", String.valueOf(amount),
				"%item%", InvUtil.getName(auction.item)));

		biddingPlayers.remove(player);
	}

	/**
	 * Registers a player for bidding via /biete
	 * The player is removed after 2 minutes, where has to select an AuctionItem again
	 */
	public void registerForBidding(Player player, AuctionItem auction) {
		biddingPlayers.put(player, auction);

		Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {

			// Check if player has changed auction
			AuctionItem curAuction = biddingPlayers.getOrDefault(player, new AuctionItem(-1));
			if(auction.id != curAuction.id) return;

			biddingPlayers.remove(player);
		}, 20 * BID_TIME);
	}
}
