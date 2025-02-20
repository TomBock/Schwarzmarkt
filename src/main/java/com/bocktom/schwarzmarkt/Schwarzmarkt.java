package com.bocktom.schwarzmarkt;

import com.bocktom.schwarzmarkt.inv.SetupInventory;
import com.bocktom.schwarzmarkt.inv.WinningsInventory;
import com.bocktom.schwarzmarkt.util.Config;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Schwarzmarkt extends JavaPlugin {

	public static Schwarzmarkt plugin;
	public static Config config;

	@Override
	public void onEnable() {
		plugin = this;
		config = new Config();

		//noinspection DataFlowIssue
		getCommand("schwarzmarkt").setExecutor(new SchwarzmarktCommand());
	}

	public void openAuction(String playerName) {

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
