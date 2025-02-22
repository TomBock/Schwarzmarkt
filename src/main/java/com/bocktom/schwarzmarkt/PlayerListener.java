package com.bocktom.schwarzmarkt;

import com.bocktom.schwarzmarkt.util.MSG;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if(!Schwarzmarkt.db.getWinnings(player.getUniqueId()).isEmpty()) {
			player.sendMessage(Component.text(MSG.get("onjoin.won")).clickEvent(ClickEvent.runCommand("/schwarzmarkt gewinne")));
		}

		int returnedBids = Schwarzmarkt.db.getAndClearReturnedBids(player.getUniqueId());
		if(returnedBids > 0) {
			player.sendMessage(MSG.get("onjoin.returned", "%amount%", String.valueOf(returnedBids)));
		}
	}
}
