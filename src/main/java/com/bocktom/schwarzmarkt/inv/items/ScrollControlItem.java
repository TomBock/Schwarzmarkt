package com.bocktom.schwarzmarkt.inv.items;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.ScrollGui;
import xyz.xenondevs.invui.item.AbstractScrollGuiBoundItem;

/**
 * Local replacement for InvUI's {@code item.impl.controlitem.ScrollItem}, which was
 * removed in InvUI 2. Scrolls the bound {@link ScrollGui} by a fixed amount of lines.
 * <p>
 * InvUI 2 also dropped {@code ScrollGui#canScroll(int)}, so the bounds check is done
 * here against the gui's current and maximum line.
 */
public abstract class ScrollControlItem extends AbstractScrollGuiBoundItem {

	private final int scroll;

	public ScrollControlItem(int scroll) {
		this.scroll = scroll;
	}

	protected int getScroll() {
		return scroll;
	}

	/**
	 * Whether the bound gui can still be scrolled by the given amount of lines.
	 */
	protected boolean canScroll(int lines) {
		ScrollGui<?> gui = getGui();
		int target = gui.getLine() + lines;
		return target >= 0 && target <= gui.getMaxLine();
	}

	@Override
	public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
		if(!canScroll(scroll))
			return;
		getGui().setLine(getGui().getLine() + scroll);
	}
}
