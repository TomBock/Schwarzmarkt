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

import java.util.List;
import java.util.UUID;

public class PlayerListener implements Listener {

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		UUID playerUuid = player.getUniqueId();

		if(!Schwarzmarkt.db.getWinnings(playerUuid).isEmpty()) {
			sendMessage(player, MSG.get("onjoin.won"));
		}

		int returnedBids = Schwarzmarkt.db.getAndClearReturnedBids(playerUuid);
		if(returnedBids > 0) {
			sendMessage(player, MSG.get("onjoin.lost", "%amount%", String.valueOf(returnedBids)));
		}

		List<Integer> earnings = Schwarzmarkt.db.getAndClearEarningsFromSoldItems(playerUuid);
		boolean hasNonSoldItems = false;
		for (Integer earning : earnings) {
			hasEarnings = hasEarnings | earning > 0;
			hasNonSoldItems = hasNonSoldItems | earning == 0;
		}
		if(hasEarnings) {
			sendMessage(player, MSG.get("onjoin.sold", "%amount%", String.valueOf(soldItems)));
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
