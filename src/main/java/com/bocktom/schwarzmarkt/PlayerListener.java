package com.bocktom.schwarzmarkt;

import com.bocktom.schwarzmarkt.util.Config;
import com.bocktom.schwarzmarkt.util.MSG;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerListener implements Listener {

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		UUID playerUuid = player.getUniqueId();

		boolean hasWinnings = !Schwarzmarkt.db.getWinnings(playerUuid).isEmpty();
		boolean hasNotSold = !Schwarzmarkt.db.getNotSold(playerUuid).isEmpty();
		if(hasWinnings && hasNotSold) {
			sendMessage(player, MSG.get("onjoin.wonandnotsold"));
		} else if(hasWinnings) {
			sendMessage(player, MSG.get("onjoin.won"));
		} else if(hasNotSold) {
			sendMessage(player, MSG.get("onjoin.notsold"));
		}

		int returnedBids = Schwarzmarkt.db.getAndClearReturnedBids(playerUuid);
		if(returnedBids > 0) {
			sendMessage(player, MSG.get("onjoin.lost", "%amount%", String.valueOf(returnedBids)));
		}

		int earnings = Schwarzmarkt.db.getAndClearEarningsFromSoldItems(playerUuid);
		if(earnings > 0) {
			sendMessage(player, MSG.get("onjoin.sold", "%amount%", String.valueOf(earnings)));
		}
	}

	private void sendMessage(Player player, String msg) {
		int delay = Config.msg.get.getInt("onjoin.delay");
		Bukkit.getScheduler().runTaskLaterAsynchronously(Schwarzmarkt.plugin, () -> {
			if(player != null && player.isOnline())
				player.sendMessage(Component.text(msg));
		}, 20L * delay);
	}
}
