package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Function;

public class ServerSetupItem extends SetupItem {

	public ServerSetupItem(int id, ItemStack item, int amount, Function<PickableItem, Boolean> onAdded, Function<PickableItem, Boolean> tryRemove) {
		super(id, item, amount, onAdded, tryRemove);
	}

	@Override
	protected int getAddition() {
		return 1;
	}

	@Override
	protected int getSubtraction() {
		return -1;
	}

	@Override
	protected List<String> getRawLore() {
		return MSG.getList("setup.item.lore");
	}

	public static SetupItem empty(Function<PickableItem, Boolean> tryAdd, Function<PickableItem, Boolean> onRemoved) {
		return new ServerSetupItem(-1, new ItemStack(Material.AIR), 0, tryAdd, onRemoved);
	}
}
