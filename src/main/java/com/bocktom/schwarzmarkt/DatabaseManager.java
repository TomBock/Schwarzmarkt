package com.bocktom.schwarzmarkt;

import com.bocktom.schwarzmarkt.inv.Auction;
import com.bocktom.schwarzmarkt.inv.PlayerAuction;
import com.bocktom.schwarzmarkt.util.*;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.utils.DataFixerUtil;
import org.bukkit.inventory.ItemStack;

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

	private static final int DB_VERSION = 5;
	// 2 = fixed
	// 3 = 1.21.5
	// 4 = added player auctions
	// 5 = not sold items

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
		setDbVersion(4);
		int curVersion = getDbVersion();
		if(curVersion != DB_VERSION) {

			boolean rollbackNeeded = false;
			if(curVersion == 2) {
				rollbackNeeded = upNbtVersion(DataFixerUtil.VERSION1_21_3, DataFixerUtil.VERSION1_21_5);
			}

			if(!rollbackNeeded) {
				setDbVersion(DB_VERSION);
			}
		}

		getLogger().info("Current database version: " + curVersion);

		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			int result = 0;

			// Versioning
			if(curVersion == 0) {
				result += new DBStatementBuilder(con, "sql/create_items.sql").executeUpdate();
				result += new DBStatementBuilder(con, "sql/create_auctions.sql").executeUpdate();
				result += new DBStatementBuilder(con, "sql/create_auction_bids.sql").executeUpdate();
				result += new DBStatementBuilder(con, "sql/create_winnings.sql").executeUpdate();
				result += new DBStatementBuilder(con, "sql/create_return_bids.sql").executeUpdate();
			} else if (curVersion == 1) {
				result += new DBStatementBuilder(con, "sql/v2/migrate_items.sql").executeUpdate();
			} else if (curVersion <= 3) {
				result += new DBStatementBuilder(con, "sql/v4/create_player_auctions.sql").executeUpdate();
				result += new DBStatementBuilder(con, "sql/v4/create_player_items.sql").executeUpdate();
				result += new DBStatementBuilder(con, "sql/v4/create_player_auction_bids.sql").executeUpdate();
				result += new DBStatementBuilder(con, "sql/v4/create_sold_items.sql").executeUpdate();
				result += new DBStatementBuilder(con, "sql/v4/create_item_cooldown.sql").executeUpdate();
			} else if(curVersion <= 4) {
				result += new DBStatementBuilder(con, "sql/v5/create_notsold.sql").executeUpdate();
			} else {
				plugin.getLogger().warning("Unknown database version: " + curVersion);
			}

			con.commit();
			if(result > 0)
				plugin.getLogger().info("Created " + result + " tables");
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to setup database: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private boolean upNbtVersion(int fromVersion, int toVersion) {
		int updated = 0;

		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			// Update Items
			try (ResultSet set = new DBStatementBuilder(con, "sql/select_items.sql").executeQuery()) {
				while (set.next()) {
					int id = set.getInt("id");
					String oldData = set.getString("item_data");

					// 2. Parse NBT
					ReadWriteNBT nbt = NBT.parseNBT(oldData);

					// 3. Fix the NBT using DataFixerUtil
					ReadWriteNBT fixedNbt = DataFixerUtil.fixUpItemData(nbt, fromVersion, toVersion);

					// 4. Update the auction with new NBT
					new DBStatementBuilder(con, "sql/v3/update_items_nbt.sql")
							.setString(1, fixedNbt.toString())
							.setInt(2, id)
							.executeUpdate();

					updated++;
				}
			}

			// Update Auctions
			try (ResultSet set = new DBStatementBuilder(con, "sql/select_auctions.sql").executeQuery()) {
				while (set.next()) {
					int id = set.getInt("id");
					String oldData = set.getString("item_data");

					// 2. Parse NBT
					ReadWriteNBT nbt = NBT.parseNBT(oldData);

					// 3. Fix the NBT using DataFixerUtil
					ReadWriteNBT fixedNbt = DataFixerUtil.fixUpItemData(nbt, fromVersion, toVersion);

					// 4. Update the auction with new NBT
					new DBStatementBuilder(con, "sql/v3/update_auctions_nbt.sql")
							.setString(1, fixedNbt.toString())
							.setInt(2, id)
							.executeUpdate();

					updated++;
				}
			}

			// Update Winnings
			try (ResultSet set = new DBStatementBuilder(con, "sql/v3/select_all_winnings.sql").executeQuery()) {
				while (set.next()) {
					int id = set.getInt("id");
					String oldData = set.getString("item_data");

					// 2. Parse NBT
					ReadWriteNBT nbt = NBT.parseNBT(oldData);

					// 3. Fix the NBT using DataFixerUtil
					ReadWriteNBT fixedNbt = DataFixerUtil.fixUpItemData(nbt, fromVersion, toVersion);

					// 4. Update the auction with new NBT
					new DBStatementBuilder(con, "sql/v3/update_winnings_nbt.sql")
							.setString(1, fixedNbt.toString())
							.setInt(2, id)
							.executeUpdate();
					updated++;

				}
			}
			con.commit();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to create version table: " + e.getMessage());
			e.printStackTrace();
			return true;
		} catch (NoSuchFieldException | IllegalAccessException e) {
			plugin.getLogger().warning("Failed to update NBT: " + e.getMessage());
			e.printStackTrace();
			return true;
		}
		plugin.getLogger().info("Updated " + updated + " items to new NBT version");
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
				try(ResultSet set = new DBStatementBuilder(con, "sql/v4/insert_player_auction.sql")
						.setInt(1, item.id)
						.setString(2, json)
						.setBytes(3, item.ownerUuid.toString().getBytes())
						.setInt(4, item.amount) //minBid
						.setInt(5, item.amount) // deposit
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
					int auctionId = set.getInt("id");

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
					int auctionId = set.getInt("id");

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

	public boolean updateItems(List<DbItem> added, List<DbItem> updated, ArrayList<Integer> removed) {
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
					int deposit = set.getInt("deposit");
					int minBid = set.getInt("min_bid");
					String json = set.getString("item_data");
					ReadWriteNBT nbt = NBT.parseNBT(json);
					items.add(new OwnedDbItem(id, ownerUuid, NBT.itemStackFromNBT(nbt), minBid, deposit));
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
			try(ResultSet set = new DBStatementBuilder(con, "sql/v4/select_player_items_by_player.sql")
					.setBytes(1, playerUuid.toString().getBytes())
					.executeQuery()) {

				while(set.next()) {
					int id = set.getInt("id");
					String json = set.getString("item_data");
					int inAuction = set.getInt("in_auction");
					int deposit = set.getInt("deposit");
					int minBid = set.getInt("min_bid");
					ReadWriteNBT nbt = NBT.parseNBT(json);
					items.add(new PlayerDbItem(id, playerUuid, NBT.itemStackFromNBT(nbt), minBid, deposit, inAuction > 0));
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

	public boolean updatePlayerItems(UUID owner_uuid, List<DbItem> added, List<DbItem> updated, ArrayList<Integer> removed, int deposit) {
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
				new DBStatementBuilder(con, "sql/v4/insert_player_items_by_player.sql")
						.setBytes(1, owner_uuid.toString().getBytes())
						.setString(2, json)
						.setInt(3, item.amount) // minBid
						.setInt(4, deposit)
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
					int highestBid = set.getInt("highest_bid");
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
			int result = new DBStatementBuilder(con, "sql/v5/delete_notsold.sql")
					.setInt(1, id)
					.executeUpdate();
			return result > 0;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to delete not sold item: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}
}
