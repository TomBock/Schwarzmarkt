package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Function;

public class PlayerSetupItem extends SetupItem {

	public PlayerSetupItem(int id, ItemStack item, int amount, Function<PickableItem, Boolean> onAdded, Function<PickableItem, Boolean> tryRemove) {
		super(id, item, amount, onAdded, tryRemove);
	}

	@Override
	protected List<String> getRawLore() {
		return MSG.getList("setup.item.lore.player");
	}

	@Override
	protected int getAddition() {
		return 100;
	}

	@Override
	protected int getSubtraction() {
		return -100;
	}

}
