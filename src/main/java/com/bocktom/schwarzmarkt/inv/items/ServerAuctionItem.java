package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.util.Config;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.ItemUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ServerAuctionItem extends AuctionItem {

	/** Permission this title grants, null for an ordinary item. */
	public final String titlePerm;

	private final boolean titleOwned;

	public ServerAuctionItem(int id) {
		super(id);

		this.titlePerm = null;
		this.titleOwned = false;
	}

	public ServerAuctionItem(int id, ItemStack item, int currentBid, Predicate<String> ownsTitle, Consumer<AuctionItem> clickHandler) {
		super(id, item, currentBid, clickHandler);

		// Read before fillItemLore, which wipes the very line the permission lives in
		this.titlePerm = InvUtil.getTitlePerm(item);
		this.titleOwned = titlePerm != null && ownsTitle.test(titlePerm);

		fillItemLore(item, currentBid);
	}

	public boolean isTitle() {
		return titlePerm != null;
	}

	public boolean isTitleOwned() {
		return titleOwned;
	}

	private void fillItemLore(ItemStack item, int currentBid) {
		ItemMeta meta = item.getItemMeta();

		List<String> lore = ItemUtil.getLore(meta);

		// A title carries its permission in the last lore line, which must never reach the
		// player - its lore is replaced wholesale by the title block from the config.
		if(isTitle()) {
			lore.clear();

			if(titleOwned)
				markOwned(meta);
		}

		List<String> raw = MSG.getList(getLoreKey());
		for (String line : raw) {
			if(line.contains("%meingebot%")) {
				if(currentBid > 0) {
					lore.add(line.replace("%meingebot%", String.valueOf(currentBid)));
					continue;
				}
			} else {
				lore.add(line);
			}
		}

		ItemUtil.setLoreWithoutEvents(meta, lore);
		item.setItemMeta(meta);
	}

	/**
	 * Marks an owned title so it reads at a glance in a full gui, without hovering every
	 * single name tag.
	 * <p>
	 * The item shown in the auction is a display copy, so a texture set here never travels
	 * anywhere - and an owned title cannot be bought, so it never leaves the gui either.
	 * <p>
	 * Both settings are optional. Without a custommodeldata entry only the glint is applied,
	 * which is what happened before this was configurable. The glint is worth keeping even
	 * with a texture in place: a player without the resource pack would otherwise see a
	 * plain name tag. The lore says so in either case.
	 */
	private void markOwned(ItemMeta meta) {
		String modelData = Config.gui.get.getString("auction.item.titleowned.custommodeldata");

		if(modelData != null && !modelData.isBlank()) {
			CustomModelDataComponent component = meta.getCustomModelDataComponent();
			try {
				component.setFloats(List.of(Float.parseFloat(modelData.trim())));
			} catch (NumberFormatException e) {
				// Packs may select on strings instead of numbers
				component.setStrings(List.of(modelData.trim()));
			}
			meta.setCustomModelDataComponent(component);
		}

		if(Config.gui.get.getBoolean("auction.item.titleowned.glint", true))
			meta.setEnchantmentGlintOverride(true);
	}

	private String getLoreKey() {
		if(!isTitle())
			return "auction.item.lore.server";

		return titleOwned ? "auction.item.lore.titleowned" : "auction.item.lore.title";
	}

	@Override
	public boolean isServerAuction() {
		return true;
	}
}
