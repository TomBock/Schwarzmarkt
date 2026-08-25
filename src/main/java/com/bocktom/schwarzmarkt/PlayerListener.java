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
import java.util.concurrent.TimeUnit;

public class PlayerListener implements Listener {

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		UUID playerUuid = player.getUniqueId();

		// Four database reads per join. The notifications go out delayed anyway, so none
		// of this has to hold up the tick thread while the player is connecting.
		Bukkit.getAsyncScheduler().runNow(Schwarzmarkt.plugin,
				task -> notifyOnJoin(player, playerUuid));
	}

	private void notifyOnJoin(Player player, UUID playerUuid) {
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

		// The delay is configured in seconds; the async scheduler takes a TimeUnit
		// directly, so there is no tick conversion any more. It also rejects a delay of
		// zero, hence the branch for an unset or disabled onjoin.delay.
		Runnable send = () -> {
			if(player != null && player.isOnline())
				player.sendMessage(Component.text(msg));
		};

		if(delay <= 0) {
			Bukkit.getAsyncScheduler().runNow(Schwarzmarkt.plugin, task -> send.run());
		} else {
			Bukkit.getAsyncScheduler().runDelayed(Schwarzmarkt.plugin, task -> send.run(),
					delay, TimeUnit.SECONDS);
		}
	}
}
