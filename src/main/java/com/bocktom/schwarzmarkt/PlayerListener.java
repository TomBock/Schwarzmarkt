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

public class PlayerListener implements Listener {

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if(!Schwarzmarkt.db.getWinnings(player.getUniqueId()).isEmpty()) {
			sendMessage(player, MSG.get("onjoin.won"));
		}

		int returnedBids = Schwarzmarkt.db.getAndClearReturnedBids(player.getUniqueId());
		if(returnedBids > 0) {
			sendMessage(player, MSG.get("onjoin.returned", "%amount%", String.valueOf(returnedBids)));
		}
	}

	private void sendMessage(Player player, String msg) {
		int delay = Config.msg.get.getInt("onjoin.delay");
		Bukkit.getScheduler().runTaskLaterAsynchronously(Schwarzmarkt.plugin, () -> player.sendMessage(Component.text(msg)), 20L * delay);
	}
}
