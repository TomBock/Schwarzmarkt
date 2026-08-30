package com.bocktom.schwarzmarkt.util;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
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
	 * Whether the title's permission node is actually assigned to the player.
	 * <p>
	 * Neither {@code Player#hasPermission} nor LuckPerms' {@code checkPermission} answers
	 * that question. Both report what a permission <i>resolves to</i>: an operator or the
	 * holder of a wildcard comes back positive for every title there is, and the only way
	 * to read as "not owned" is an explicit deny. What is wanted here is whether the node
	 * was handed out, so the player's nodes are inspected directly.
	 * <p>
	 * Inherited nodes are included, so a title granted through a group counts. A wildcard
	 * does not, because it is a node in its own right and never equals a concrete title
	 * permission.
	 */
	public static boolean hasTitlePermission(Player player, String perm) {
		if(perm == null || perm.isBlank())
			return false;

		User user = Schwarzmarkt.perms.getUserManager().getUser(player.getUniqueId());
		if(user == null)
			return false;

		return user.resolveDistinctInheritedNodes(user.getQueryOptions()).stream()
				.filter(NodeType.PERMISSION::matches)
				.map(NodeType.PERMISSION::cast)
				.anyMatch(node -> node.getValue() && node.getPermission().equalsIgnoreCase(perm));
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
