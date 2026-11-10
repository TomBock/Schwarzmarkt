package com.bocktom.schwarzmarkt.util;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.utils.DataFixerUtil;

/**
 * {@link ItemDataFixer} backed by NBT-API and the server's own data fixer.
 */
public class NbtApiItemDataFixer implements ItemDataFixer {

	@Override
	public Integer readDataVersion(String itemData) {
		ReadWriteNBT nbt = NBT.parseNBT(itemData);
		return nbt.hasTag("DataVersion") ? nbt.getInteger("DataVersion") : null;
	}

	@Override
	public String fixUp(String itemData, int fromVersion, int toVersion) throws Exception {
		// fixUpItemData also stamps toVersion as the new DataVersion
		return DataFixerUtil.fixUpItemData(NBT.parseNBT(itemData), fromVersion, toVersion).toString();
	}
}
