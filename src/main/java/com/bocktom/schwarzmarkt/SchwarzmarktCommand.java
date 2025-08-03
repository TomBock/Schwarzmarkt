package com.bocktom.schwarzmarkt;

import com.bocktom.schwarzmarkt.util.Config;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import com.bocktom.schwarzmarkt.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SchwarzmarktCommand implements CommandExecutor, TabCompleter {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player player)
			return onPlayerCommand(player, command, label, args);
		else
			return onConsoleCommand(sender, command, label, args);
	}

	private boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
		if(!player.hasPermission("schwarzmarkt.use")) {
			player.sendMessage("Du hast keine Berechtigung für diesen Befehl");
			return true;
		}

		boolean isAdmin = player.hasPermission("schwarzmarkt.admin");

		if(args.length == 1) {

			// USER
			switch (args[0]) {
				case "bieten":
					player.sendMessage(MSG.get("bid.noamount"));
					return true;
			}

			// ADMIN
			if(isAdmin) {
				switch (args[0]) {
					case "info":
						Schwarzmarkt.plugin.openInfo(player);
						return true;
					case "setup":
						Schwarzmarkt.plugin.openSetup(player);
						return true;

					//Deprecated
					case "start":
						Schwarzmarkt.auctions.startAuctions(player, true);
						return true;
					//Deprecated
					case "stop":
						Schwarzmarkt.auctions.stopAuctions(player, true);
						return true;
				}
			}
		}

		if(args.length == 2) {

			// ADMIN
			if(isAdmin) {
				switch(args[0]) {

					case "setinvitem":
						setInvItem(player, args[1]);
						return true;
					case "gewinne":
						Schwarzmarkt.plugin.openWinnings(args[1]);
						return true;

					case "start":
						Schwarzmarkt.auctions.startAuctions(player, args[1].equals("server"));
						return true;
					case "stop":
						Schwarzmarkt.auctions.stopAuctions(player, args[1].equals("server"));
						return true;


					case "setup":
						Schwarzmarkt.plugin.openPlayerSetup(args[1]);
						return true;
					case "showsetup":
						Schwarzmarkt.plugin.openPlayerSetup(player, args[1]);
						return true;

					case "show":
						Schwarzmarkt.plugin.openAuction(args[1]);
						return true;
				}
			}

			// USER
			switch (args[0]) {
				case "bieten":
					int amount = 0;
					try {
						amount = Integer.parseInt(args[1]);

					} catch(NumberFormatException e) {
						player.sendMessage(MSG.get("bid.noamount"));
						return true;
					}
					if(amount <= 0) {
						player.sendMessage(MSG.get("bid.invalidamount"));
						return true;
					}

					Schwarzmarkt.auctions.bid(player, amount);
					return true;
			}
		}

		if(args.length == 3) {

			// ADMIN
			if(isAdmin) {

				switch (args[0]) {
					case "titel":
						ItemStack item = InvUtil.createTitleItem(args[1], args[2]);
						PlayerUtil.give(player, item);
						return true;
				}

			}
		}

		if(isAdmin)
			player.sendMessage("Dieser Befehl braucht mindestens 1 Parameter: /schwarzmarkt (setup | setup [player] | start | stop | info | gewinne [player] | bieten [amount] | show [player] | showsetup [player] | titel [displayname] [permission] | setinvitem [slotkey])");
		else
			player.sendMessage("Dieser Befehl braucht mindestens 1 Parameter: /schwarzmarkt bieten [betrag]");
		return true;
	}

	private void setInvItem(Player player, String key) {
		Config.InternalConfig config = Config.gui;

		ItemStack item = player.getItemInHand();
		if(item == null) {
			player.sendMessage("No item in hand");
			return;
		}

		config.get.set("slots." + key, item);
		config.save();
		player.sendMessage("Item " + key + " set");
	}

	private boolean onConsoleCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 1) {
			switch (args[0]) {

				// Deprecated
				case "start":
					Schwarzmarkt.auctions.startAuctions(null, true);
					return true;
				// Deprecated
				case "stop":
					Schwarzmarkt.auctions.stopAuctions(null, true);
					return true;

				case "info":
					Schwarzmarkt.plugin.openInfo(sender);
					return true;
			}
		}

		if(args.length == 2) {
			switch (args[0]) {
				// Deprecated
				case "show":
					Schwarzmarkt.plugin.openAuction(args[1]);
					return true;

				case "start":
					Schwarzmarkt.auctions.startAuctions(null, args[1].equals("server"));
					return true;
				case "stop":
					Schwarzmarkt.auctions.stopAuctions(null, args[1].equals("server"));
					return true;
			}
		}

		sender.sendMessage("Dieser Befehl braucht mindestens 1 Parameter: /schwarzmarkt (start | stop | info | show [player])");
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player player))
			return List.of();
		boolean isAdmin = player.hasPermission("schwarzmarkt.admin");

		List<String> completions = new ArrayList<>();


		if(args.length == 1) {
			completions.add("gewinne");
			completions.add("bieten");

			if(isAdmin) {
				completions.add("setup");
				completions.add("showsetup");
				completions.add("start");
				completions.add("stop");
				completions.add("info");
				completions.add("show");
				completions.add("titel");
				completions.add("setinvitem");
			}
		} else if(args.length == 2 && isAdmin) {

			switch (args[0]) {
				case "show", "showsetup":
					Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(completions::add);
					break;
				case "start", "stop":
					completions.add("server");
					completions.add("spieler");
					break;
			}
		}

		completions.sort(String::compareToIgnoreCase);
		return completions;
	}
}
