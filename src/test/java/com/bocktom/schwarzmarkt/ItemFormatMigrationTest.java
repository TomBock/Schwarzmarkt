package com.bocktom.schwarzmarkt;

import com.bocktom.schwarzmarkt.util.DBStatementBuilder;
import com.bocktom.schwarzmarkt.util.ItemDataFixer;
import com.bocktom.schwarzmarkt.util.ItemDataMigration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the 26.2 -> 26.3 item format step against a real SQLite database built from the
 * plugin's own create scripts.
 * <p>
 * The Minecraft data fixer needs a running server, so these tests swap it for
 * {@link FakeFixer}, which only re-stamps the DataVersion. What is covered here is the
 * database side: which rows are touched, from which version, the transaction and the
 * schema version - not the conversion of the item data itself.
 */
class ItemFormatMigrationTest {

	private static final int V26_2 = 4903;
	private static final int V26_3 = 5023;
	private static final int V1_21_11 = 4671;

	/** The seven tables holding serialized items, in the order the step visits them. */
	private static final String[] ITEM_TABLES = {
			"items", "auctions", "winnings", "player_items", "player_auctions", "notsold", "item_cooldown"
	};

	private static final String[] SCHEMA = {
			"sql/v1/create_schema_version.sql",
			"sql/create_items.sql",
			"sql/create_auctions.sql",
			"sql/create_auction_bids.sql",
			"sql/create_winnings.sql",
			"sql/create_return_bids.sql",
			"sql/v4/create_player_auctions.sql",
			"sql/v4/create_player_items.sql",
			"sql/v4/create_player_auction_bids.sql",
			"sql/v4/create_sold_items.sql",
			"sql/v4/create_item_cooldown.sql",
			"sql/v5/create_notsold.sql",
	};

	/** Item data as a 26.2 server writes it, one per kind of item the market handles. */
	private static final Map<String, String> ITEMS_26_2 = new LinkedHashMap<>();
	static {
		ITEMS_26_2.put("enchanted", "{DataVersion:4903,count:1,id:\"minecraft:diamond_sword\",components:{\"minecraft:enchantments\":{\"minecraft:sharpness\":5,\"minecraft:unbreaking\":3}}}");
		ITEMS_26_2.put("damaged", "{DataVersion:4903,count:1,id:\"minecraft:iron_pickaxe\",components:{\"minecraft:damage\":187}}");
		ITEMS_26_2.put("custom_model_data", "{DataVersion:4903,count:1,id:\"minecraft:paper\",components:{\"minecraft:custom_model_data\":{floats:[1001.0f],strings:[\"schwarzmarkt:coin\"]}}}");
		ITEMS_26_2.put("lore", "{DataVersion:4903,count:3,id:\"minecraft:emerald\",components:{\"minecraft:custom_name\":{text:\"Schmuggelware\",color:\"dark_green\",italic:0b},\"minecraft:lore\":[{text:\"Zeile 1\",italic:0b},{text:\"Zeile 2 mit Umlauten: äöü\",color:\"gray\"}]}}");
		ITEMS_26_2.put("player_head", "{DataVersion:4903,count:1,id:\"minecraft:player_head\",components:{\"minecraft:profile\":{name:\"Thillo\",id:[I;1715421354,-1186643411,-1519449213,1043349862],properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWJjIn19fQ==\"}]}}}");
		ITEMS_26_2.put("bundle", "{DataVersion:4903,count:1,id:\"minecraft:bundle\",components:{\"minecraft:bundle_contents\":[{count:16,id:\"minecraft:arrow\"},{count:1,id:\"minecraft:golden_apple\"}]}}");
		ITEMS_26_2.put("shulker", "{DataVersion:4903,count:1,id:\"minecraft:shulker_box\",components:{\"minecraft:container\":[{slot:0,item:{count:64,id:\"minecraft:diamond\"}},{slot:13,item:{count:1,id:\"minecraft:netherite_sword\",components:{\"minecraft:enchantments\":{\"minecraft:mending\":1}}}}]}}");
		ITEMS_26_2.put("title", "{DataVersion:4903,count:1,id:\"minecraft:name_tag\",components:{\"minecraft:custom_name\":{text:\"Schattenhändler\",color:\"#8A2BE2\"},\"minecraft:lore\":[\"§7phoenix.titel.schattenhaendler\"]}}");
	}

	private static final String CORRUPT = "{DataVersion:4903,count:1,id:\"minecraft:stone\",components:{\"minecraft:lore\":[";

	@TempDir
	Path tempDir;

	private String url;

	@BeforeEach
	void createDatabase() throws Exception {
		url = "jdbc:sqlite:" + tempDir.resolve("schwarzmarkt.db").toAbsolutePath();
		try (Connection con = connect()) {
			for (String script : SCHEMA)
				new DBStatementBuilder(con, script).executeUpdate();
			setSchemaVersion(con, 8);
		}
	}

	@Test
	void eachKindOfItemRoundTripsThroughEveryItemTable() throws Exception {
		Map<String, Map<Integer, String>> written = new LinkedHashMap<>();
		try (Connection con = connect()) {
			for (String table : ITEM_TABLES)
				written.put(table, insertAll(con, table, new ArrayList<>(ITEMS_26_2.values())));
		}

		// Read back through the same select scripts the migration uses
		try (Connection con = connect()) {
			for (int i = 0; i < ITEM_TABLES.length; i++) {
				Map<Integer, String> read = readItemData(con, DatabaseManager.NBT_TABLES_V9[i][0]);
				assertEquals(written.get(ITEM_TABLES[i]), read, ITEM_TABLES[i]);
			}
		}
	}

	@Test
	void migratesAllSevenTablesFrom4903To5023() throws Exception {
		try (Connection con = connect()) {
			for (String table : ITEM_TABLES)
				insertAll(con, table, new ArrayList<>(ITEMS_26_2.values()));
		}

		FakeFixer fixer = new FakeFixer();
		ItemDataMigration.Result result;
		try (Connection con = connect()) {
			result = newMigration(fixer).run(con, DatabaseManager.DB_VERSION);
		}

		int rows = ITEM_TABLES.length * ITEMS_26_2.size();
		assertEquals(rows, result.updated());
		assertEquals(0, result.skipped());
		assertEquals(rows, fixer.calls.size());
		assertTrue(fixer.calls.stream().allMatch(c -> c.from == V26_2 && c.to == V26_3), fixer.calls.toString());

		try (Connection con = connect()) {
			assertEquals(9, schemaVersion(con));
			for (String table : ITEM_TABLES) {
				List<String> data = new ArrayList<>(column(con, table, "item_data").values());
				List<String> expected = ITEMS_26_2.values().stream().map(s -> s.replace("DataVersion:4903", "DataVersion:5023")).toList();
				assertEquals(expected, data, table);
			}
		}
	}

	@Test
	void eachRowIsFixedFromTheDataVersionItCarries() throws Exception {
		String old = ITEMS_26_2.get("enchanted").replace("DataVersion:4903", "DataVersion:" + V1_21_11);
		String unversioned = ITEMS_26_2.get("damaged").replace("DataVersion:4903,", "");
		String current = ITEMS_26_2.get("lore").replace("DataVersion:4903", "DataVersion:" + V26_3);
		String newer = ITEMS_26_2.get("title").replace("DataVersion:4903", "DataVersion:5100");

		try (Connection con = connect()) {
			insertAll(con, "winnings", List.of(old, unversioned, current, newer));
		}

		FakeFixer fixer = new FakeFixer();
		ItemDataMigration.Result result;
		try (Connection con = connect()) {
			result = newMigration(fixer).run(con, 9);
		}

		assertEquals(2, result.updated());
		assertEquals(2, result.skipped());
		assertEquals(List.of(new Call(V1_21_11, V26_3), new Call(V26_2, V26_3)), fixer.calls);

		try (Connection con = connect()) {
			List<String> data = new ArrayList<>(column(con, "winnings", "item_data").values());
			assertEquals(current, data.get(2), "already current row must stay untouched");
			assertEquals(newer, data.get(3), "newer row must stay untouched");
		}
	}

	@Test
	void runningTheStepAgainOnAMigratedDatabaseChangesNothing() throws Exception {
		try (Connection con = connect()) {
			for (String table : ITEM_TABLES)
				insertAll(con, table, new ArrayList<>(ITEMS_26_2.values()));
		}
		try (Connection con = connect()) {
			newMigration(new FakeFixer()).run(con, 9);
		}
		Map<String, Map<Integer, List<String>>> afterFirst = snapshot();

		FakeFixer second = new FakeFixer();
		ItemDataMigration.Result result;
		try (Connection con = connect()) {
			result = newMigration(second).run(con, 9);
		}

		assertEquals(0, result.updated());
		assertEquals(ITEM_TABLES.length * ITEMS_26_2.size(), result.skipped());
		assertTrue(second.calls.isEmpty());
		assertEquals(afterFirst, snapshot());
		try (Connection con = connect()) {
			assertEquals(9, schemaVersion(con));
		}
	}

	@Test
	void aCorruptRowAbortsTheStepAndLeavesTheDatabaseUnchanged() throws Exception {
		try (Connection con = connect()) {
			for (String table : ITEM_TABLES)
				insertAll(con, table, new ArrayList<>(ITEMS_26_2.values()));
			// Last table, so the rows of all other tables have already been rewritten
			// inside the transaction by the time the step hits it
			insertAll(con, "item_cooldown", List.of(CORRUPT));
		}
		Map<String, Map<Integer, List<String>>> before = snapshot();

		FakeFixer fixer = new FakeFixer();
		ItemDataMigration.MigrationException e;
		try (Connection con = connect()) {
			e = assertThrows(ItemDataMigration.MigrationException.class, () -> newMigration(fixer).run(con, 9));
		}

		assertTrue(e.getMessage().contains("item_cooldown"), e.getMessage());
		assertEquals(ITEM_TABLES.length * ITEMS_26_2.size(), fixer.calls.size(),
				"every row before the corrupt one was converted inside the transaction");
		assertEquals(before, snapshot());
		try (Connection con = connect()) {
			assertEquals(8, schemaVersion(con));
		}
	}

	@Test
	void stepCoversTheSevenItemTablesAtTheNbtApiVersions() throws Exception {
		// The step in DatabaseManager is wired to these NBT-API constants
		assertEquals(V26_2, de.tr7zw.changeme.nbtapi.utils.DataFixerUtil.VERSION_26_2);
		assertEquals(V26_3, de.tr7zw.changeme.nbtapi.utils.DataFixerUtil.VERSION_26_3);
		assertEquals(9, DatabaseManager.DB_VERSION);
		assertEquals(ITEM_TABLES.length, DatabaseManager.NBT_TABLES_V9.length);
		for (int i = 0; i < ITEM_TABLES.length; i++) {
			String select = resource(DatabaseManager.NBT_TABLES_V9[i][0]);
			String update = resource(DatabaseManager.NBT_TABLES_V9[i][1]);
			assertTrue(select.contains("FROM " + ITEM_TABLES[i] + ";"), select);
			assertTrue(update.startsWith("UPDATE " + ITEM_TABLES[i] + " SET item_data = ? WHERE id = ?"), update);
		}
	}

	private static String resource(String path) throws java.io.IOException {
		try (var in = ItemFormatMigrationTest.class.getClassLoader().getResourceAsStream(path)) {
			assertNotNull(in, path);
			return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
		}
	}

	// --- helpers ---

	private ItemDataMigration newMigration(ItemDataFixer fixer) {
		return new ItemDataMigration(DatabaseManager.NBT_TABLES_V9, V26_2, V26_3, fixer);
	}

	private Connection connect() throws SQLException {
		return DriverManager.getConnection(url);
	}

	private static void setSchemaVersion(Connection con, int version) throws Exception {
		new DBStatementBuilder(con, "sql/v1/insert_or_update_schema_version.sql").setInt(1, version).executeUpdate();
	}

	private static int schemaVersion(Connection con) throws Exception {
		try (ResultSet set = new DBStatementBuilder(con, "sql/v1/select_schema_version.sql").executeQuery()) {
			assertTrue(set.next());
			return set.getInt("latest_version");
		}
	}

	private static Map<Integer, String> insertAll(Connection con, String table, List<String> itemData) throws SQLException {
		String sql = switch (table) {
			case "items" -> "INSERT INTO items (item_data, amount) VALUES (?, 1)";
			case "auctions" -> "INSERT INTO auctions (item_data, highest_bid) VALUES (?, 0)";
			case "winnings" -> "INSERT INTO winnings (player_uuid, item_data) VALUES (X'01', ?)";
			case "player_items" -> "INSERT INTO player_items (owner_uuid, owner_name, item_data) VALUES (X'01', 'Thillo', ?)";
			case "player_auctions" -> "INSERT INTO player_auctions (item_id, item_data, owner_uuid, owner_name) VALUES (1, ?, X'01', 'Thillo')";
			case "notsold" -> "INSERT INTO notsold (owner_uuid, item_data) VALUES (X'01', ?)";
			case "item_cooldown" -> "INSERT INTO item_cooldown (item_data, cooldown_end) VALUES (?, '2026-12-01 00:00:00')";
			default -> throw new IllegalArgumentException(table);
		};
		Map<Integer, String> ids = new LinkedHashMap<>();
		try (PreparedStatement statement = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
			for (String data : itemData) {
				statement.setString(1, data);
				statement.executeUpdate();
				try (ResultSet keys = statement.getGeneratedKeys()) {
					keys.next();
					ids.put(keys.getInt(1), data);
				}
			}
		}
		return ids;
	}

	private static Map<Integer, String> readItemData(Connection con, String selectScript) throws Exception {
		Map<Integer, String> data = new LinkedHashMap<>();
		try (ResultSet set = new DBStatementBuilder(con, selectScript).executeQuery()) {
			while (set.next())
				data.put(set.getInt("id"), set.getString("item_data"));
		}
		return data;
	}

	private static Map<Integer, String> column(Connection con, String table, String column) throws SQLException {
		Map<Integer, String> values = new LinkedHashMap<>();
		try (PreparedStatement statement = con.prepareStatement("SELECT id, " + column + " FROM " + table + " ORDER BY id");
			 ResultSet set = statement.executeQuery()) {
			while (set.next())
				values.put(set.getInt(1), set.getString(2));
		}
		return values;
	}

	/** Every column of every item table plus schema_version, row by row. */
	private Map<String, Map<Integer, List<String>>> snapshot() throws SQLException {
		Map<String, Map<Integer, List<String>>> tables = new LinkedHashMap<>();
		try (Connection con = connect()) {
			List<String> names = new ArrayList<>(List.of(ITEM_TABLES));
			names.add("schema_version");
			for (String table : names) {
				Map<Integer, List<String>> rows = new LinkedHashMap<>();
				try (PreparedStatement statement = con.prepareStatement("SELECT rowid, * FROM " + table + " ORDER BY rowid");
					 ResultSet set = statement.executeQuery()) {
					ResultSetMetaData meta = set.getMetaData();
					while (set.next()) {
						List<String> row = new ArrayList<>();
						for (int c = 2; c <= meta.getColumnCount(); c++)
							row.add(set.getString(c));
						rows.put(set.getInt(1), row);
					}
				}
				tables.put(table, rows);
			}
		}
		return tables;
	}

	private record Call(int from, int to) {}

	/**
	 * Stands in for the server's data fixer: rejects data that is not a complete compound,
	 * otherwise only re-stamps the DataVersion, as {@code DataFixerUtil.fixUpItemData} does.
	 */
	private static class FakeFixer implements ItemDataFixer {

		private static final Pattern DATA_VERSION = Pattern.compile("DataVersion:(\\d+)");

		final List<Call> calls = new ArrayList<>();

		@Override
		public Integer readDataVersion(String itemData) {
			requireCompound(itemData);
			Matcher matcher = DATA_VERSION.matcher(itemData);
			return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
		}

		@Override
		public String fixUp(String itemData, int fromVersion, int toVersion) {
			requireCompound(itemData);
			calls.add(new Call(fromVersion, toVersion));
			Matcher matcher = DATA_VERSION.matcher(itemData);
			return matcher.find()
					? matcher.replaceFirst("DataVersion:" + toVersion)
					: "{DataVersion:" + toVersion + "," + itemData.substring(1);
		}

		private static void requireCompound(String data) {
			int depth = 0;
			boolean inString = false;
			for (int i = 0; i < data.length(); i++) {
				char c = data.charAt(i);
				if(c == '"' && (i == 0 || data.charAt(i - 1) != '\\'))
					inString = !inString;
				if(inString)
					continue;
				if(c == '{' || c == '[') depth++;
				if(c == '}' || c == ']') depth--;
			}
			if(!data.startsWith("{") || depth != 0 || inString)
				throw new IllegalArgumentException("Malformed item data");
		}
	}
}
