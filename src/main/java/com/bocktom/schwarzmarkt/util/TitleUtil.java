package com.bocktom.schwarzmarkt.util;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Answers whether a player already owns a title.
 * <p>
 * A title counts as owned once its permission has been granted. A player who won one but
 * has not fetched it from the winnings gui yet does not hold the permission, so those are
 * counted too - otherwise the same title could be bought a second time in the window
 * between winning it and collecting it.
 */
public class TitleUtil {

	private TitleUtil() {}

	/**
	 * Whether the permission has actually been granted to the player.
	 * <p>
	 * Deliberately not {@code Player#hasPermission}: an operator, or anyone holding a
	 * wildcard, passes that check for every permission there is and would find themselves
	 * locked out of bidding on every title. LuckPerms reports TRUE only for a node that was
	 * really assigned.
	 */
	public static boolean hasTitlePermission(Player player, String perm) {
		if(perm == null || perm.isBlank())
			return false;

		User user = Schwarzmarkt.perms.getUserManager().getUser(player.getUniqueId());
		if(user == null)
			return false;

		return user.getCachedData().getPermissionData().checkPermission(perm) == Tristate.TRUE;
	}

	/**
	 * Permissions of the titles sitting in the player's winnings, waiting to be collected.
	 */
	public static Set<String> getUnclaimedTitlePerms(UUID uuid) {
		Set<String> perms = new HashSet<>();

		for (ItemStack item : Schwarzmarkt.db.getWinnings(uuid).values()) {
			String perm = InvUtil.getTitlePerm(item);
			if(perm != null)
				perms.add(perm);
		}
		return perms;
	}

	/**
	 * Checks a single title. Hits the winnings table, so use
	 * {@link #getUnclaimedTitlePerms} once instead when a whole gui full of items is built.
	 */
	public static boolean owns(Player player, String perm) {
		if(perm == null)
			return false;

		return hasTitlePermission(player, perm)
				|| getUnclaimedTitlePerms(player.getUniqueId()).contains(perm);
	}
}
