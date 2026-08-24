package com.bocktom.schwarzmarkt;

import com.bocktom.schwarzmarkt.inv.Auction;
import com.bocktom.schwarzmarkt.inv.PlayerAuction;
import com.bocktom.schwarzmarkt.util.*;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.utils.DataFixerUtil;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.bukkit.Bukkit.getLogger;

public class DatabaseManager {

	private static final int DB_VERSION = 8;
	// 2 = fixed
	// 3 = 1.21.5
	// 4 = added player auctions
	// 5 = not sold items
	// 6 = try again
	// 7 = name for player auctions
	// 8 = 26.2 item format

	/**
	 * Tables holding serialized items, as (select, update) sql resource pairs.
	 * <p>
	 * The 1.21.5 step only knew items, auctions and winnings - the player auction and
	 * notsold tables did not exist yet - so that step keeps its original, smaller scope.
	 */
	/** Create scripts per schema version. All of them are CREATE TABLE IF NOT EXISTS. */
	private static final String[] SCHEMA_V1 = {
			"sql/create_items.sql",
			"sql/create_auctions.sql",
			"sql/create_auction_bids.sql",
			"sql/create_winnings.sql",
			"sql/create_return_bids.sql",
	};

	private static final String[] SCHEMA_V4 = {
			"sql/v4/create_player_auctions.sql",
			"sql/v4/create_player_items.sql",
			"sql/v4/create_player_auction_bids.sql",
			"sql/v4/create_sold_items.sql",
			"sql/v4/create_item_cooldown.sql",
	};

	private static final String[] SCHEMA_V5 = {
			"sql/v5/create_notsold.sql",
	};

	private static final String[][] NBT_TABLES_V3 = {
			{"sql/select_items.sql", "sql/v3/update_items_nbt.sql"},
			{"sql/select_auctions.sql", "sql/v3/update_auctions_nbt.sql"},
			{"sql/v3/select_all_winnings.sql", "sql/v3/update_winnings_nbt.sql"},
	};

	private static final String[][] NBT_TABLES_V8 = {
			{"sql/select_items.sql", "sql/v3/update_items_nbt.sql"},
			{"sql/select_auctions.sql", "sql/v3/update_auctions_nbt.sql"},
			{"sql/v3/select_all_winnings.sql", "sql/v3/update_winnings_nbt.sql"},
			{"sql/v8/select_all_player_items.sql", "sql/v8/update_player_items_nbt.sql"},
			{"sql/v8/select_all_player_auctions.sql", "sql/v8/update_player_auctions_nbt.sql"},
			{"sql/v8/select_all_notsold.sql", "sql/v8/update_notsold_nbt.sql"},
			{"sql/v8/select_all_item_cooldown.sql", "sql/v8/update_item_cooldown_nbt.sql"},
	};

	private final Schwarzmarkt plugin;
	private String dbUrl;

	public DatabaseManager() {
		this.plugin = Schwarzmarkt.plugin;
		setupDatabase("schwarzmarkt.db");
	}

	private void setupDatabase(String fileName) {
		File dbFile = new File(plugin.getDataFolder(), fileName);
		if (!plugin.getDataFolder().exists())
			plugin.getDataFolder().mkdirs();
		this.dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

		// Versioning
		createVersionTable();
		int curVersion = getDbVersion();

		getLogger().info("Current database version: " + curVersion);

		if(curVersion == DB_VERSION) {
			plugin.getLogger().info("Database version is up to date");
			ensureSchema();
			return;
		}
		if(curVersion > DB_VERSION) {
			plugin.getLogger().warning("Unknown database version: " + curVersion);
			return;
		}

		// Every outstanding step is applied in order - a fresh database walks the whole
		// chain from 1 to DB_VERSION. The stored version is only raised after a step
		// actually went through, so a failed migration is retried on the next start
		// instead of being silently skipped.
		for (int version = curVersion + 1; version <= DB_VERSION; version++) {
			if(!migrateTo(version)) {
				plugin.getLogger().severe("Database migration to version " + version + " failed - aborting at version " + (version - 1));
				return;
			}
			setDbVersion(version);
			plugin.getLogger().info("Database migrated to version " + version);
		}

		ensureSchema();
	}

	/**
	 * Applies the single step that lifts the database from {@code version - 1} to
	 * {@code version}.
	 *
	 * @return whether the step succeeded
	 */
	private boolean migrateTo(int version) {

		// Item format steps rewrite row data and manage their own transaction
		if(version == 3) {
			return !upNbtVersion(DataFixerUtil.VERSION1_21_3, DataFixerUtil.VERSION1_21_5, NBT_TABLES_V3);
		}
		if(version == 8) {
			return !upNbtVersion(DataFixerUtil.VERSION1_21R7, DataFixerUtil.VERSION_26_2, NBT_TABLES_V8);
		}

		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			int result = 0;

			switch (version) {
				case 1 -> result += createTables(con, SCHEMA_V1);
				case 2 -> {
					// create_items.sql declares amount itself nowadays, so on a database
					// that walked the full chain this ALTER would hit "duplicate column name"
					if(!hasColumn(con, "items", "amount"))
						result += new DBStatementBuilder(con, "sql/v2/migrate_items.sql").executeUpdate();
				}
				case 4 -> {
					result += createTables(con, SCHEMA_V4);
					result += new DBStatementBuilder(con, "sql/v4/seed_player_auction_ids.sql").executeUpdate();
				}
				case 5 -> result += createTables(con, SCHEMA_V5);
				case 6 -> {
					// No schema change - this version was only bumped to re-run version 5,
					// which never applied because the old migration chain skipped it.
				}
				case 7 -> {
					// The v4 create scripts already declare owner_name nowadays, so on a
					// database that walked the full chain the column is present and these
					// ALTERs would fail with "duplicate column name".
					if(!hasColumn(con, "player_auctions", "owner_name"))
						result += new DBStatementBuilder(con, "sql/v6/alter_player_auctions.sql").executeUpdate();
					if(!hasColumn(con, "player_items", "owner_name"))
						result += new DBStatementBuilder(con, "sql/v6/alter_player_items.sql").executeUpdate();
				}
				default -> {
					plugin.getLogger().warning("No migration defined for database version: " + version);
					return false;
				}
			}

			con.commit();
			if(result > 0)
				plugin.getLogger().info("Applied " + result + " statements for database version " + version);
			return true;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to migrate database to version " + version + ": " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	private int createTables(Connection con, String[] scripts) throws SQLException, IOException {
		int result = 0;
		for (String script : scripts) {
			result += new DBStatementBuilder(con, script).executeUpdate();
		}
		return result;
	}

	/**
	 * Re-runs every create script once the version chain is done.
	 * <p>
	 * Databases that were set up while the old migration chain was in place can sit at a
	 * high version number without ever having received the tables of the steps in between,
	 * and the version chain alone would never revisit them. Every script here is a
	 * CREATE TABLE IF NOT EXISTS, so this is a no-op on a healthy database.
	 */
	private void ensureSchema() {
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);
			createTables(con, SCHEMA_V1);
			createTables(con, SCHEMA_V4);
			createTables(con, SCHEMA_V5);
			con.commit();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to verify schema: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Whether the given table already has the given column. Used to keep ALTER steps
	 * idempotent instead of relying on the create scripts and the ALTERs never overlapping.
	 */
	private boolean hasColumn(Connection con, String table, String column) throws SQLException {
		try (ResultSet set = con.getMetaData().getColumns(null, null, table, column)) {
			return set.next();
		}
	}

	/**
	 * Runs the NBT data fixer over every table that stores serialized items.
	 *
	 * @param fromVersion fallback source version for rows whose nbt carries no DataVersion
	 * @param tables (select, update) sql resource pairs; the select must expose {@code id}
	 *               and {@code item_data}, the update takes (item_data, id)
	 * @return whether a rollback is needed, i.e. whether the step failed
	 */
	private boolean upNbtVersion(int fromVersion, int toVersion, String[][] tables) {
		int updated = 0;
		int skipped = 0;

		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			for (String[] table : tables) {
				String selectSql = table[0];
				String updateSql = table[1];

				try (ResultSet set = new DBStatementBuilder(con, selectSql).executeQuery()) {
					while (set.next()) {
						int id = set.getInt("id");
						String oldData = set.getString("item_data");
						if(oldData == null)
							continue;

						ReadWriteNBT nbt = NBT.parseNBT(oldData);

						// Rows are not all written by the same server version: the last item
						// format step was 1.21.5, everything stored since then carries the
						// version that was running at the time. Trusting a single source
						// version would skip the fixes an older row still needs, so each row
						// is fixed from the DataVersion it carries.
						Integer storedVersion = nbt.hasTag("DataVersion") ? nbt.getInteger("DataVersion") : null;
						int itemVersion = storedVersion != null ? storedVersion : fromVersion;

						if(itemVersion >= toVersion) {
							skipped++;
							continue;
						}

						ReadWriteNBT fixedNbt = DataFixerUtil.fixUpItemData(nbt, itemVersion, toVersion);

						new DBStatementBuilder(con, updateSql)
								.setString(1, fixedNbt.toString())
								.setInt(2, id)
								.executeUpdate();

						updated++;
					}
				}
			}

			con.commit();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to update NBT: " + e.getMessage());
			e.printStackTrace();
			return true;
		} catch (NoSuchFieldException | IllegalAccessException e) {
			plugin.getLogger().warning("Failed to update NBT: " + e.getMessage());
			e.printStackTrace();
			return true;
		}
		plugin.getLogger().info("Updated " + updated + " items to NBT version " + toVersion
				+ (skipped > 0 ? " (" + skipped + " already current or newer)" : ""));
		return false;
	}

	private void createVersionTable() {
		try (Connection con = getConnection()) {
			new DBStatementBuilder(con, "sql/v1/create_schema_version.sql")
					.executeUpdate();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to create version table: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private int setDbVersion(int version) {
		try (Connection con = getConnection()) {
			int result = new DBStatementBuilder(con, "sql/v1/insert_or_update_schema_version.sql")
					.setInt(1, version)
					.executeUpdate();
			return result;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to set db version: " + e.getMessage());
			e.printStackTrace();
		}
		return 0;
	}

	private int getDbVersion() {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/v1/select_schema_version.sql")
					.executeQuery()) {

				if(set.next()) {
					return set.getInt("latest_version");
				}
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get db version: " + e.getMessage());
			e.printStackTrace();
		}
		return 0;
	}

	public List<Integer> addAuctions(Collection<ItemStack> items) {
		List<Integer> auctionIds = new ArrayList<>();
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);
			for(ItemStack item : items) {
				String json = NBT.itemStackToNBT(item).toString();
				try(ResultSet set = new DBStatementBuilder(con, "sql/insert_auction.sql")
						.setString(1, json)
						.executeQuery()) {

					if(set.next()) {
						int id = set.getInt(1);
						auctionIds.add(id);
					}
				}
			}
			con.commit();
			plugin.getLogger().info("Added " + items.size() + " auctions");
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to add auction: " + e.getMessage());
			e.printStackTrace();
		}
		return auctionIds;
	}

	public List<Integer> addPlayerAuctions(Collection<OwnedDbItem> items) {
		List<Integer> auctionIds = new ArrayList<>();

		try (Connection con = getConnection()) {
			con.setAutoCommit(false);
			for(OwnedDbItem item : items) {
				String json = NBT.itemStackToNBT(item.item).toString();
				try(ResultSet set = new DBStatementBuilder(con, "sql/v6/insert_player_auction.sql")
						.setInt(1, item.id)
						.setString(2, json)
						.setBytes(3, item.ownerUuid.toString().getBytes())
						.setString(4, item.ownerName)
						.setInt(5, item.amount) //minBid
						.setInt(6, item.amount) // deposit
						.executeQuery()) {

					if(set.next()) {
						int id = set.getInt(1);
						auctionIds.add(id);
					}
				}
			}
			con.commit();
			plugin.getLogger().info("Added " + items.size() + " auctions");
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to add auction: " + e.getMessage());
			e.printStackTrace();
		}
		return auctionIds;
	}

	public void removeAuctions() {
		try (Connection con = getConnection()) {
			new DBStatementBuilder(con, "sql/delete_all_auctions.sql")
					.executeUpdate();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to remove auctions: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void removePlayerAuctions() {
		try (Connection con = getConnection()) {
			new DBStatementBuilder(con, "sql/v4/delete_all_player_auctions.sql")
					.executeUpdate();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to remove auctions: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public List<Auction> getServerAuctions() {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/select_auctions.sql")
					.executeQuery()) {

				List<Auction> auctions = new ArrayList<>();
				while(set.next()) {
					byte[] highestBidder = set.getBytes("highest_bidder_uuid");
					UUID uuid = highestBidder != null ? UUID.fromString(new String(highestBidder)) : null;
					int id = set.getInt("id");
					ReadWriteNBT nbt = NBT.parseNBT(set.getString("item_data"));
					ItemStack itemStack = NBT.itemStackFromNBT(nbt);

					int highestBid = set.getInt("highest_bid");

					Auction auction = new Auction(
							id,
							itemStack,
							highestBid,
							uuid);
					auctions.add(auction);
				}
				return auctions;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get auctions: " + e.getMessage());
			e.printStackTrace();
		}
		return List.of();
	}


	public List<PlayerAuction> getPlayerAuctions() {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/v4/select_player_auctions.sql")
					.executeQuery()) {

				List<PlayerAuction> auctions = new ArrayList<>();
				while(set.next()) {
					int id = set.getInt("id");

					byte[] highestBidder = set.getBytes("highest_bidder_uuid");
					UUID highestBidderUuid = highestBidder != null ? UUID.fromString(new String(highestBidder)) : null;
					byte[] owner = set.getBytes("owner_uuid");
					UUID ownerUuid = owner != null ? UUID.fromString(new String(owner)) : null;

					String ownerName = set.getString("owner_name");

					ReadWriteNBT nbt = NBT.parseNBT(set.getString("item_data"));
					ItemStack itemStack = NBT.itemStackFromNBT(nbt);

					int highestBid = set.getInt("highest_bid");
					int minBid = set.getInt("min_bid");
					int deposit = set.getInt("deposit");
					int itemId = set.getInt("item_id");

					PlayerAuction auction = new PlayerAuction(
							id,
							itemId,
							itemStack,
							ownerUuid,
							ownerName,
							minBid,
							deposit,
							highestBid,
							highestBidderUuid);
					auctions.add(auction);
				}
				return auctions;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get auctions: " + e.getMessage());
			e.printStackTrace();
		}
		return List.of();
	}

	@Deprecated
	public int getBid(int auctionId, UUID playerUuid) {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/select_bid.sql")
					.setInt(1, auctionId)
					.setBytes(2, playerUuid.toString().getBytes())
					.executeQuery()) {

				if(set.next()) {
					return set.getInt("bid_amount");
				}
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get bid: " + e.getMessage());
			e.printStackTrace();
		}
		return 0;
	}

	public boolean placeBid(int auctionId, UUID playerUuid, int amount) {
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			byte[] uuid = playerUuid.toString().getBytes();
			int result = new DBStatementBuilder(con, "sql/insert_or_update_bid.sql")
					.setInt(1, auctionId)
					.setBytes(2, uuid)
					.setInt(3, amount)
					.executeUpdate();

			int totalBid = 0;
			try(ResultSet set = new DBStatementBuilder(con, "sql/select_bid.sql")
					.setInt(1, auctionId)
					.setBytes(2, playerUuid.toString().getBytes())
					.executeQuery()) {

				if(set.next()) {
					totalBid = set.getInt("bid_amount");
				}
			}

			result += new DBStatementBuilder(con, "sql/update_highest_bid.sql")
					.setInt(1, totalBid)
					.setBytes(2, uuid)
					.setInt(3, auctionId)
					.setInt(4, totalBid)
					.executeUpdate();

			con.commit();
			return result > 0;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to place bid: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public boolean placePlayerBid(int auctionId, UUID playerUuid, int amount) {
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			byte[] uuid = playerUuid.toString().getBytes();
			int result = new DBStatementBuilder(con, "sql/v4/insert_or_update_player_bid.sql")
					.setInt(1, auctionId)
					.setBytes(2, uuid)
					.setInt(3, amount)
					.executeUpdate();

			int totalBid = 0;
			try(ResultSet set = new DBStatementBuilder(con, "sql/v4/select_player_bid.sql")
					.setInt(1, auctionId)
					.setBytes(2, playerUuid.toString().getBytes())
					.executeQuery()) {

				if(set.next()) {
					totalBid = set.getInt("bid_amount");
				}
			}

			result += new DBStatementBuilder(con, "sql/v4/update_highest_player_bid.sql")
					.setInt(1, totalBid)
					.setBytes(2, uuid)
					.setInt(3, auctionId)
					.setInt(4, totalBid)
					.executeUpdate();

			con.commit();
			return result > 0;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to place bid: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public boolean rollbackBid(int auctionId, UUID playerUuid, int amount) {
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			byte[] uuid = playerUuid.toString().getBytes();
			int result = new DBStatementBuilder(con, "sql/rollback_bid.sql")
					.setInt(1, amount)
					.setInt(2, auctionId)
					.setBytes(3, uuid)
					.setInt(4, auctionId)
					.setBytes(5, uuid)
					.executeUpdate();

			con.commit();
			return result > 0;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to rollback bid: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public Map<Integer, Bids> getServerAuctionBids() {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/select_bids.sql")
					.executeQuery()) {

				Map<Integer, Bids> bids = new HashMap<>();
				while(set.next()) {
					UUID playerUuid = UUID.fromString(new String(set.getBytes("player_uuid")));
					int amount = set.getInt("bid_amount");
					int auctionId = set.getInt("auction_id");

					bids.computeIfAbsent(auctionId, k -> new Bids()).put(playerUuid, amount);
				}
				return bids;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get bids: " + e.getMessage());
			e.printStackTrace();
		}
		return Map.of();
	}

	public Map<Integer, Bids> getPlayerAuctionBids() {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/v4/select_player_auction_bids.sql")
					.executeQuery()) {

				Map<Integer, Bids> bids = new HashMap<>();
				while(set.next()) {
					UUID playerUuid = UUID.fromString(new String(set.getBytes("player_uuid")));
					int amount = set.getInt("bid_amount");
					int auctionId = set.getInt("player_auction_id");

					bids.computeIfAbsent(auctionId, k -> new Bids()).put(playerUuid, amount);
				}
				return bids;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get bids: " + e.getMessage());
			e.printStackTrace();
		}
		return Map.of();
	}

	public Map<UUID, Integer> getServerAuctionBids(int auctionId) {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/select_bids_by_auction.sql")
					.setInt(1, auctionId)
					.executeQuery()) {

				Map<UUID, Integer> bids = new HashMap<>();
				while(set.next()) {
					UUID playerUuid = UUID.fromString(new String(set.getBytes("player_uuid")));
					int amount = set.getInt("bid_amount");
					bids.put(playerUuid, amount);
				}
				return bids;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get bids: " + e.getMessage());
			e.printStackTrace();
		}
		return Map.of();
	}

	public Map<Integer, Integer> getServerAuctionBids(UUID playerUuid) {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/select_bids_by_player.sql")
					.setBytes(1, playerUuid.toString().getBytes())
					.executeQuery()) {

				Map<Integer, Integer> bids = new HashMap<>();
				while(set.next()) {
					int auctionId = set.getInt("auction_id");
					int amount = set.getInt("bid_amount");
					bids.put(auctionId, amount);
				}
				return bids;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get bids: " + e.getMessage());
			e.printStackTrace();
		}
		return Map.of();
	}


	public Map<Integer, Integer> getPlayerAuctionBids(UUID playerUuid) {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/v4/select_player_bids_by_player.sql")
					.setBytes(1, playerUuid.toString().getBytes())
					.executeQuery()) {

				Map<Integer, Integer> bids = new HashMap<>();
				while(set.next()) {
					int auctionId = set.getInt("player_auction_id");
					int amount = set.getInt("bid_amount");
					bids.put(auctionId, amount);
				}
				return bids;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get bids: " + e.getMessage());
			e.printStackTrace();
		}
		return Map.of();
	}

	public boolean removeBids() {
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			new DBStatementBuilder(con, "sql/delete_auction_bids.sql")
					.executeUpdate();
			con.commit();
			return true;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to delete bids: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public boolean removePlayerBids() {
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			new DBStatementBuilder(con, "sql/v4/delete_player_auction_bids.sql")
					.executeUpdate();
			con.commit();
			return true;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to delete bids: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public boolean updateItems(List<DbItem> added, List<DbItem> updated, Set<Integer> removed) {
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			for(int id : removed) {
				new DBStatementBuilder(con, "sql/delete_item.sql")
						.setInt(1, id)
						.executeUpdate();
			}

			for(DbItem item : added) {
				String json = NBT.itemStackToNBT(item.item).toString();
				new DBStatementBuilder(con, "sql/insert_item.sql")
						.setString(1, json)
						.setInt(2, item.amount)
						.executeUpdate();
			}

			for(DbItem item : updated) {
				String json = NBT.itemStackToNBT(item.item).toString();
				new DBStatementBuilder(con, "sql/v1/update_item.sql")
						.setString(1, json)
						.setInt(2, item.amount)
						.setInt(3, item.id)
						.executeUpdate();
			}


			con.commit();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to update items: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public List<DbItem> getItems() {
		List<DbItem> items = new ArrayList<>();

		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/select_items.sql")
					.executeQuery()) {

				while(set.next()) {
					int id = set.getInt("id");
					int amount = set.getInt("amount");
					String json = set.getString("item_data");
					ReadWriteNBT nbt = NBT.parseNBT(json);
					items.add(new DbItem(id, NBT.itemStackFromNBT(nbt), amount));
				}
				return items;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get items: " + e.getMessage());
			e.printStackTrace();
		}
		return List.of();
	}

	public List<OwnedDbItem> getPlayerItems() {
		List<OwnedDbItem> items = new ArrayList<>();

		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/v4/select_player_items.sql")
					.executeQuery()) {

				while(set.next()) {
					int id = set.getInt("id");
					UUID ownerUuid = UUID.fromString(new String(set.getBytes("owner_uuid")));
					String ownerName = set.getString("owner_name");
					int deposit = set.getInt("deposit");
					int minBid = set.getInt("min_bid");
					String json = set.getString("item_data");
					ReadWriteNBT nbt = NBT.parseNBT(json);
					items.add(new OwnedDbItem(id, ownerUuid, ownerName, NBT.itemStackFromNBT(nbt), minBid, deposit));
				}
				return items;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get items: " + e.getMessage());
			e.printStackTrace();
		}
		return List.of();
	}

	public List<ItemStack> getRandomItems(int amount) {
		List<DbItem> items = getItems();
		return InvUtil.getWeighedRandomSelection(items, amount, dbItem -> dbItem.item);
	}

	public List<OwnedDbItem> getRandomPlayerItems(int auctionItems) {
		List<OwnedDbItem> items = getPlayerItems();
		return InvUtil.getRandomSelection(items, auctionItems);
	}

	public boolean addWinnings(UUID uuid, ItemStack item) {
		try (Connection con = getConnection()) {
			int result = new DBStatementBuilder(con, "sql/insert_winnings.sql")
					.setBytes(1, uuid.toString().getBytes())
					.setString(2, NBT.itemStackToNBT(item).toString())
					.executeUpdate();
			return result > 0;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to add winnings: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public boolean removeWinnings(int winningsId) {
		try (Connection con = getConnection()) {
			int result = new DBStatementBuilder(con, "sql/delete_winnings.sql")
					.setInt(1, winningsId)
					.executeUpdate();
			return result > 0;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to remove winnings: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public Map<Integer, ItemStack> getWinnings(UUID uuid) {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/select_winnings.sql")
					.setBytes(1, uuid.toString().getBytes())
					.executeQuery()) {

				Map<Integer, ItemStack> winnings = new TreeMap<>();
				while(set.next()) {
					int id = set.getInt("id");
					String json = set.getString("item_data");
					ItemStack item = NBT.itemStackFromNBT(NBT.parseNBT(json));
					winnings.put(id, item);
				}
				return winnings;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get winnings: " + e.getMessage());
			e.printStackTrace();
		}
		return Map.of();
	}

	public void addBidsToNotifyLater(Map<UUID, Integer> bids) {
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);
			for (Map.Entry<UUID, Integer> entry : bids.entrySet()) {
				new DBStatementBuilder(con, "sql/insert_or_update_return_bids.sql")
						.setBytes(1, entry.getKey().toString().getBytes())
						.setInt(2, entry.getValue())
						.executeUpdate();
			}
			con.commit();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to add return bid: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public int getAndClearReturnedBids(UUID playerUuid) {
		int amount = 0;
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			try(ResultSet set = new DBStatementBuilder(con, "sql/select_return_bids.sql")
					.setBytes(1, playerUuid.toString().getBytes())
					.executeQuery()) {

				if(set.next()) {
					 amount = set.getInt("amount");
				}
			}

			new DBStatementBuilder(con, "sql/delete_return_bids.sql")
					.setBytes(1, playerUuid.toString().getBytes())
					.executeUpdate();

			con.commit();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get returned bids: " + e.getMessage());
			e.printStackTrace();
		}
		return amount;
	}

	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection(this.dbUrl);
	}

	public List<PlayerDbItem> getPlayerItems(UUID playerUuid) {
		List<PlayerDbItem> items = new ArrayList<>();

		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/v6/select_player_items_by_player.sql")
					.setBytes(1, playerUuid.toString().getBytes())
					.executeQuery()) {

				while(set.next()) {
					int id = set.getInt("id");
					String playerName = set.getString("owner_name");
					String json = set.getString("item_data");
					int inAuction = set.getInt("in_auction");
					int deposit = set.getInt("deposit");
					int minBid = set.getInt("min_bid");
					ReadWriteNBT nbt = NBT.parseNBT(json);
					items.add(new PlayerDbItem(id, playerUuid, playerName, NBT.itemStackFromNBT(nbt), minBid, deposit, inAuction > 0));
				}
				return items;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get items: " + e.getMessage());
			e.printStackTrace();
		}
		return List.of();
	}

	public boolean removePlayerItem(UUID owner_uuid, int itemId) {
		try (Connection con = getConnection()) {
			int result = new DBStatementBuilder(con, "sql/v4/delete_player_item.sql")
					.setBytes(1, owner_uuid.toString().getBytes())
					.setInt(2, itemId)
					.executeUpdate();
			return result > 0;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to delete player item: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public boolean updatePlayerItems(UUID owner_uuid, String ownerName, List<DbItem> added, List<DbItem> updated, Set<Integer> removed, int deposit) {
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			for(int id : removed) {
				new DBStatementBuilder(con, "sql/v4/delete_player_items_by_player.sql")
						.setBytes(1, owner_uuid.toString().getBytes())
						.setInt(2, id)
						.executeUpdate();
			}

			for(DbItem item : added) {
				String json = NBT.itemStackToNBT(item.item).toString();
				new DBStatementBuilder(con, "sql/v6/insert_player_items_by_player.sql")
						.setBytes(1, owner_uuid.toString().getBytes())
						.setString(2, ownerName)
						.setString(3, json)
						.setInt(4, item.amount) // minBid
						.setInt(5, deposit)
						.executeUpdate();
			}

			for(DbItem item : updated) {
				String json = NBT.itemStackToNBT(item.item).toString();
				new DBStatementBuilder(con, "sql/v4/update_player_items_by_player.sql")
						.setString(1, json)
						.setInt(2, item.amount) // minBid
						.setInt(3, item.id)
						.executeUpdate();
			}


			con.commit();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to update items: " + e.getMessage());
			e.printStackTrace();
		}

		return false;
	}

	public void addSoldItem(UUID ownerUuid, int highestBid, UUID highestBidder) {
		try (Connection con = getConnection()) {
			new DBStatementBuilder(con, "sql/v4/insert_sold_item.sql")
					.setBytes(1, ownerUuid.toString().getBytes())
					.setInt(2, highestBid)
					.setBytes(3, highestBidder.toString().getBytes())
					.executeUpdate();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to add sold item: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public int getAndClearEarningsFromSoldItems(UUID ownerUuid) {
		int earnings = 0;
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/v4/select_sold_item_by_player.sql")
					.setBytes(1, ownerUuid.toString().getBytes())
					.executeQuery()) {

				while(set.next()) {
					int highestBid = set.getInt("price");
					earnings += highestBid;
				}
			}

			new DBStatementBuilder(con, "sql/v4/delete_sold_item.sql")
					.setBytes(1, ownerUuid.toString().getBytes())
					.executeUpdate();

		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get sold items: " + e.getMessage());
			e.printStackTrace();
		}
		return earnings;
	}

	public void insertItemCooldown(List<ItemStack> items, long cooldownDays) {
		Instant cooldown = Instant.now().plus(cooldownDays, ChronoUnit.DAYS);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String cooldownString = dateFormat.format(Date.from(cooldown));


		try (Connection con = getConnection()) {
			for (ItemStack item : items) {
				String json = NBT.itemStackToNBT(item).toString();
				new DBStatementBuilder(con, "sql/v4/insert_item_cooldown.sql")
						.setString(1, json)
						.setString(2, cooldownString)
						.executeUpdate();
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to insert item cooldown: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void cleanItemCooldowns() {
		try (Connection con = getConnection()) {
			new DBStatementBuilder(con, "sql/v4/delete_item_cooldown.sql")
					.executeUpdate();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to remove item cooldown: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public boolean hasItemCooldown(ItemStack item) {
		try (Connection con = getConnection()) {
			String json = NBT.itemStackToNBT(item).toString();
			try(ResultSet set = new DBStatementBuilder(con, "sql/v4/select_item_cooldown.sql")
					.setString(1, json)
					.executeQuery()) {

				return set.next();
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to check item cooldown: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public boolean isServerAuctionRunning(int auctionId) {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/v4/select_server_auction_by_id.sql")
					.setInt(1, auctionId)
					.executeQuery()) {

				return set.next();
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to check if auction is running: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public boolean isPlayerAuctionRunning(int auctionId) {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/v4/select_player_auction_by_id.sql")
					.setInt(1, auctionId)
					.executeQuery()) {

				return set.next();
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to check if auction is running: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public boolean addNotsold(UUID playerUuid, ItemStack item) {
		try (Connection con = getConnection()) {
			int result = new DBStatementBuilder(con, "sql/v5/insert_notsold.sql")
					.setBytes(1, playerUuid.toString().getBytes())
					.setString(2, NBT.itemStackToNBT(item).toString())
					.executeUpdate();
			return result > 0;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to add not sold item: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public Map<Integer, ItemStack> getNotSold(UUID uuid) {
		Map<Integer, ItemStack> notSoldItems = new HashMap<>();
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/v5/select_notsold_by_player.sql")
					.setBytes(1, uuid.toString().getBytes())
					.executeQuery()) {

				while(set.next()) {
					int id = set.getInt("id");
					String json = set.getString("item_data");
					ItemStack item = NBT.itemStackFromNBT(NBT.parseNBT(json));
					notSoldItems.put(id, item);
				}
				return notSoldItems;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get not sold item: " + e.getMessage());
			e.printStackTrace();
		}
		return Map.of();
	}

	public boolean removeNotSold(int id) {
		try (Connection con = getConnection()) {
			new DBStatementBuilder(con, "sql/v5/delete_notsold.sql")
					.setInt(1, id)
					.executeUpdate();
			return true;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to delete not sold item: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public boolean hasPlacedBidInPlayerAuction(int playerAuctionId, @NotNull UUID uniqueId) {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/v5/select_player_bid_by_player.sql")
					.setInt(1, playerAuctionId)
					.setBytes(2, uniqueId.toString().getBytes())
					.executeQuery()) {

				return set.next();
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to insert player auction bid: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}
}
