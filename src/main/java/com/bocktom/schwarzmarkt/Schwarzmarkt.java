package com.bocktom.schwarzmarkt;

import com.bocktom.schwarzmarkt.inv.AuctionInventory;
import com.bocktom.schwarzmarkt.inv.SetupInventory;
import com.bocktom.schwarzmarkt.inv.WinningsInventory;
import com.bocktom.schwarzmarkt.util.Config;
import de.tr7zw.changeme.nbtapi.NBT;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Schwarzmarkt extends JavaPlugin {

	public static Schwarzmarkt plugin;

	public static DatabaseManager db;
	public static Config config;
	public static Economy economy;

	@Override
	public void onEnable() {
		plugin = this;
		config = new Config();
		db = new DatabaseManager();

		//noinspection DataFlowIssue
		getCommand("schwarzmarkt").setExecutor(new SchwarzmarktCommand());

		NBT.preloadApi();
		if(!setupEconomy()) {
			getLogger().severe("Vault not found! Disabling plugin...");
			getServer().getPluginManager().disablePlugin(this);
		}
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
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

	}

	public void openWinnings(Player player) {
		new WinningsInventory(player);
	}

	public void startAuction() {

	}

	public void stopAuction() {

	}
}
