package com.bocktom.schwarzmarkt;

import com.bocktom.schwarzmarkt.inv.items.AuctionItem;
import com.bocktom.schwarzmarkt.util.Bids;
import com.bocktom.schwarzmarkt.util.MSG;
import com.bocktom.schwarzmarkt.util.PersistentLogger;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getServer;

public class Economy {

	public static net.milkbowl.vault.economy.Economy economy;

	public boolean setup() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			getLogger().severe("Vault not found! Disabling plugin...");
			return false;
		}
		RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (rsp == null) {
			getLogger().severe("No Economy Service found. Disabling plugin...");
			return false;
		}
		economy = rsp.getProvider();
		return true;
	}

	public Bids returnBidsToPlayers(Bids bidsToReturn, boolean isPlayerAuction) {
		Bids successfulReturns = new Bids(bidsToReturn);

		for (Map.Entry<UUID, Integer> returnBid : bidsToReturn.entrySet()) {
			EconomyResponse response = economy.depositPlayer(Bukkit.getOfflinePlayer(returnBid.getKey()), returnBid.getValue());
			if(response.type != EconomyResponse.ResponseType.SUCCESS) {
				PersistentLogger.logReturnBidFailed(isPlayerAuction, returnBid.getKey(), returnBid.getValue());
				successfulReturns.remove(returnBid.getKey());
			}
		}
		return successfulReturns;
	}


	public boolean withdrawBidMoney(Player player, AuctionItem auction, int amount) {
		EconomyResponse withdrawResponse = economy.withdrawPlayer(player, amount);
		if(withdrawResponse.type != EconomyResponse.ResponseType.SUCCESS) {
			player.sendMessage(MSG.get("error"));

			// Rollback if withdraw failed
			boolean rollbackResponse = Schwarzmarkt.db.rollbackBid(auction.id, player.getUniqueId(), amount);
			if(!rollbackResponse) {
				PersistentLogger.logBidRollbackFailed(auction.isPlayerAuction(), auction.id, player, amount);
			}
			return false;
		}
		return true;
	}

	public boolean hasEnoughBalance(Player player, int amount) {
		return economy.getBalance(player) >= amount;
	}

	public boolean depositMoney(OfflinePlayer player, int amount, boolean isPlayerAuction) {
		EconomyResponse withdrawResponse = economy.depositPlayer(player, amount);
		if(withdrawResponse.type != EconomyResponse.ResponseType.SUCCESS) {
			PersistentLogger.logDepositFailed(isPlayerAuction, player, amount);
			return false;
		}
		return true;
	}

	public boolean withdrawMoney(Player player, int amount, boolean isPlayerAuction) {
		EconomyResponse withdrawResponse = economy.withdrawPlayer(player, amount);
		if(withdrawResponse.type != EconomyResponse.ResponseType.SUCCESS) {
			PersistentLogger.logWithdrawFailed(isPlayerAuction, player, amount);
			return false;
		}
		return true;
	}
}
