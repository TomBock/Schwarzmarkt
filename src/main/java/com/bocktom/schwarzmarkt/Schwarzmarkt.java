package com.bocktom.schwarzmarkt;

import com.bocktom.schwarzmarkt.inv.Auction;
import com.bocktom.schwarzmarkt.inv.AuctionInventory;
import com.bocktom.schwarzmarkt.inv.SetupInventory;
import com.bocktom.schwarzmarkt.inv.WinningsInventory;
import com.bocktom.schwarzmarkt.util.Config;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.PersistentLogger;
import de.tr7zw.changeme.nbtapi.NBT;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Schwarzmarkt extends JavaPlugin {

	public static Schwarzmarkt plugin;

	public static AuctionManager auctions;
	public static DatabaseManager db;
	public static Economy economy;
	public static Config config;
	public static LuckPerms perms;

	@Override
	public void onEnable() {
		plugin = this;
		config = new Config();
		db = new DatabaseManager();
		auctions = new AuctionManager();

		//noinspection DataFlowIssue
		getCommand("schwarzmarkt").setExecutor(new SchwarzmarktCommand());
		getServer().getPluginManager().registerEvents(new PlayerListener(), this);

		NBT.preloadApi();
		PersistentLogger.init();

		if(!setupEconomy()) {
			getServer().getPluginManager().disablePlugin(this);
		}

		if(!setupLuckPerms()) {
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

	private boolean setupLuckPerms() {
		RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
		if (provider == null) {
			getLogger().severe("LuckPerms not found! Disabling plugin...");
			return false;
		}
		perms = provider.getProvider();
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

	public void openWinnings(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		if(player != null) {
			new WinningsInventory(player);
		}
	}

}
