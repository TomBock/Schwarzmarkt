package com.bocktom.schwarzmarkt.util;

/**
 * Reads and upgrades serialized items as they are stored in the database.
 * <p>
 * Kept behind an interface so the migration can be exercised against a plain SQLite
 * database without a running server - the real data fixer needs the Minecraft runtime.
 */
public interface ItemDataFixer {

	/**
	 * @return the DataVersion the serialized item carries, or {@code null} if it has none
	 * @throws Exception if the item data cannot be parsed
	 */
	Integer readDataVersion(String itemData) throws Exception;

	/**
	 * Upgrades the serialized item from {@code fromVersion} to {@code toVersion}.
	 *
	 * @return the upgraded item data, carrying {@code toVersion} as its DataVersion
	 * @throws Exception if the item data cannot be parsed or fixed
	 */
	String fixUp(String itemData, int fromVersion, int toVersion) throws Exception;
}
