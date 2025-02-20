package com.bocktom.schwarzmarkt;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

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

		if(args.length == 0) {
			Schwarzmarkt.plugin.openWinnings(player);
			return true;
		}

		if(!player.hasPermission("schwarzmarkt.admin")) {
			player.sendMessage("Du hast keine Berechtigung für diesen Befehl");
			return true;
		}

		if(args.length == 1) {
			switch (args[0]) {
				case "setup":
					Schwarzmarkt.plugin.openSetup(player);
					return true;
				case "info":
					Schwarzmarkt.plugin.openInfo(player);
					return true;
				case "start":
					Schwarzmarkt.plugin.startAuction();
					return true;
				case "stop":
					Schwarzmarkt.plugin.stopAuction();
					return true;
			}
		}

		if(args.length == 2 && args[0].equals("show")) {
			Schwarzmarkt.plugin.openAuction(args[1]);
			return true;
		}

		player.sendMessage("Dieser Befehl braucht mindestens 1 Parameter: /schwarzmarkt <setup|info|start|stop|show>");
		return true;
	}

	private boolean onConsoleCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 1) {
			switch (args[0]) {
				case "start":
					Schwarzmarkt.plugin.startAuction();
					return true;
				case "stop":
					Schwarzmarkt.plugin.stopAuction();
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

		sender.sendMessage("Dieser Befehl braucht mindestens 1 Parameter: /schwarzmarkt <start|stop|info>");
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player player))
			return List.of();

		if(!player.hasPermission("schwarzmarkt.admin"))
			return List.of();

		if(args.length == 1) {
			return List.of("setup", "start", "stop", "info", "show");
		}

		if(args.length == 2 && args[0].equals("show")) {
			return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
		}

		return List.of();
	}
}
