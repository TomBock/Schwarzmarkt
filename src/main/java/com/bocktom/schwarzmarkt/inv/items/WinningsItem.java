package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.ItemUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.Click;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class WinningsItem extends PickableItem {

	private boolean isTitle;
	private boolean isTitleAssigned;

	private String titlePerm;
	private String title;

	public WinningsItem(int id, ItemStack item, Function<PickableItem, Boolean> onAdded, Function<PickableItem, Boolean> tryRemove) {
		super(id, item, onAdded, tryRemove);

		isTitle = InvUtil.isTitleItem(item);
		if(isTitle) {
			ItemMeta meta = item.getItemMeta();
			List<String> lore = ItemUtil.getLore(meta);
			title = meta.getDisplayName();
			titlePerm = ItemUtil.removeLegacyColorCodes(lore.getLast());
			lore.clear();
			lore.add(MSG.get("winnings.title.lore"));
			ItemUtil.setLoreWithoutEvents(meta, lore);
			item.setItemMeta(meta);
		}
	}

	@Override
	protected boolean handlePickup(@NotNull Player player, @NotNull ClickType clickType) {
		if(isTitle) {
			return handleTitle(player, clickType);
		} else {
			return super.handlePickup(player, clickType);
		}
	}

	private boolean handleTitle(@NotNull Player player, @NotNull ClickType clickType) {
		// InvUI 2 cancels item clicks itself, so the explicit setCancelled calls are gone

		if(isTitleAssigned) {
			player.sendMessage(MSG.get("winnings.title.assigned"));
			return false;
		}

		grantPermission(player).thenAccept(result -> {
			if(!result) {
				player.sendMessage(MSG.get("error"));
				return;
			}

			// false: der Titel wird gegen die Permission eingetauscht, das Nametag
			// selbst darf der Spieler nicht behalten
			if(!super.handlePickup(player, clickType, false))
				return;

			player.sendMessage(MSG.get("winnings.title.onclick", "%titel%", title));
			isTitleAssigned = true;
		});
		return true;
	}

	private CompletableFuture<Boolean> grantPermission(Player player) {
		User permUser = Schwarzmarkt.perms.getUserManager().getUser(player.getUniqueId());
		if(permUser == null)
			return CompletableFuture.completedFuture(false);

		permUser.data().add(Node.builder(titlePerm).value(true).build());
		CompletableFuture<Void> future = Schwarzmarkt.perms.getUserManager().saveUser(permUser);
		return future.thenApply(v -> true);
	}

	@Override
	protected boolean isPartialClickAllowed() {
		return false;
	}
}
