package com.bocktom.schwarzmarkt;

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
				case "gewinne":
					Schwarzmarkt.plugin.openWinnings(player);
					return true;
				case "bieten":
					player.sendMessage(MSG.get("bid.noamount"));
					return true;
			}

			// ADMIN
			if(isAdmin) {
				switch (args[0]) {
					case "setup":
						Schwarzmarkt.plugin.openSetup(player);
						return true;
					case "info":
						Schwarzmarkt.plugin.openInfo(player);
						return true;
					case "start":
						Schwarzmarkt.auctions.startAuctions(player);
						return true;
					case "stop":
						Schwarzmarkt.auctions.stopAuctions(player);
						return true;
				}
			}
		}

		if(args.length == 2) {

			// ADMIN
			if(args[0].equals("show") && isAdmin) {
				Schwarzmarkt.plugin.openAuction(args[1]);
				return true;
			}

			// USER
			if(args[0].equals("bieten")) {
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
			if(isAdmin && args[0].equals("titel")) {
				ItemStack item = InvUtil.createTitleItem(args[1], args[2]);
				PlayerUtil.give(player, item);
				return true;
			}
		}

		if(isAdmin)
			player.sendMessage("Dieser Befehl braucht mindestens 1 Parameter: /schwarzmarkt <setup|start|stop|info|gewinne|bieten|titel> [args]");
		else
			player.sendMessage("Dieser Befehl braucht mindestens 1 Parameter: /schwarzmarkt <gewinne|bieten> [betrag]");
		return true;
	}

	private boolean onConsoleCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 1) {
			switch (args[0]) {
				case "start":
					Schwarzmarkt.auctions.startAuctions(null);
					return true;
				case "stop":
					Schwarzmarkt.auctions.stopAuctions(null);
					return true;
				case "info":
					Schwarzmarkt.plugin.openInfo(sender);
					return true;
			}
		}

		if(args.length == 2 && args[0].equals("show")) {
			Schwarzmarkt.plugin.openAuction(args[1]);
			return true;
		}

		sender.sendMessage("Dieser Befehl braucht mindestens 1 Parameter: /schwarzmarkt <start|stop|info|show> [args]");
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
				completions.add("start");
				completions.add("stop");
				completions.add("info");
				completions.add("show");
				completions.add("titel");
			}
		} else if(args.length == 2 && isAdmin) {
			if(args[0].equals("show")) {
				Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(completions::add);
			}
		}

		completions.sort(String::compareToIgnoreCase);
		return completions;
	}
}
