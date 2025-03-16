package com.bocktom.schwarzmarkt;

import com.bocktom.schwarzmarkt.inv.Auction;
import com.bocktom.schwarzmarkt.util.DBStatementBuilder;
import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DatabaseManager {

	private static final int DB_VERSION = 1;

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
		int version = getDbVersion();

		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			int result = 0;

			// Versioning
			if(version == 0) {
				createVersionTable();
				version = setDbVersion(DB_VERSION);

				result += new DBStatementBuilder(con, "sql/create_items.sql").executeUpdate();
				result += new DBStatementBuilder(con, "sql/create_auctions.sql").executeUpdate();
				result += new DBStatementBuilder(con, "sql/create_auction_bids.sql").executeUpdate();
				result += new DBStatementBuilder(con, "sql/create_winnings.sql").executeUpdate();
				result += new DBStatementBuilder(con, "sql/create_return_bids.sql").executeUpdate();

				// Migrate from v0 to v1
				result += new DBStatementBuilder(con, "sql/v1/drop_returned_bids.sql").executeUpdate();
			}

			con.commit();
			if(result > 0)
				plugin.getLogger().info("Created " + result + " tables");
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to setup database: " + e.getMessage());
			e.printStackTrace();
		}
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
					return set.getInt("version");
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

	public void removeAuctions() {
		try (Connection con = getConnection()) {
			new DBStatementBuilder(con, "sql/delete_all_auctions.sql")
					.executeUpdate();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to remove auctions: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public List<Auction> getAuctions() {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/select_auctions.sql")
					.executeQuery()) {

				List<Auction> auctions = new ArrayList<>();
				while(set.next()) {
					byte[] highestBidder = set.getBytes("highest_bidder_uuid");
					UUID uuid = highestBidder != null ? UUID.fromString(new String(highestBidder)) : null;
					Auction auction = new Auction(
							set.getInt("id"),
							NBT.itemStackFromNBT(NBT.parseNBT(set.getString("item_data"))),
							set.getInt("highest_bid"),
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

	public Map<ItemStack, Map<UUID, Integer>> getBids() {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/select_bids.sql")
					.executeQuery()) {

				Map<ItemStack, Map<UUID, Integer>> bids = new HashMap<>();
				while(set.next()) {
					String json = set.getString("item_data");
					ItemStack item = NBT.itemStackFromNBT(NBT.parseNBT(json));
					UUID playerUuid = UUID.fromString(new String(set.getBytes("player_uuid")));
					int amount = set.getInt("bid_amount");

					bids.computeIfAbsent(item, k -> new HashMap<>()).put(playerUuid, amount);
				}
				return bids;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get bids: " + e.getMessage());
			e.printStackTrace();
		}
		return Map.of();
	}

	public Map<UUID, Integer> getBids(int auctionId) {
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

	public Map<Integer, Integer> getBids(UUID playerUuid) {
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

	public boolean removeBids(List<Integer> auctionId) {
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);

			for(int id : auctionId) {
				new DBStatementBuilder(con, "sql/delete_auction_bids.sql")
						.setInt(1, id)
						.executeUpdate();
			}
			con.commit();
			return true;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to delete bids: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public boolean updateItems(Collection<ItemStack> added, Collection<Integer> removed) {
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);
			for(ItemStack item : added) {
				String json = NBT.itemStackToNBT(item).toString();
				new DBStatementBuilder(con, "sql/insert_item.sql")
						.setString(1, json)
						.executeUpdate();
			}
			for(int id : removed) {
				new DBStatementBuilder(con, "sql/delete_item.sql")
						.setInt(1, id)
						.executeUpdate();
			}
			con.commit();
			return true;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to update items: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public Map<Integer, ItemStack> getItems() {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/select_items.sql")
					.executeQuery()) {

				Map<Integer, ItemStack> items = new TreeMap<>();
				while(set.next()) {
					int id = set.getInt("id");
					String json = set.getString("item_data");
					ItemStack item = NBT.itemStackFromNBT(NBT.parseNBT(json));
					items.put(id, item);
				}
				return items;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get items: " + e.getMessage());
			e.printStackTrace();
		}
		return Map.of();
	}

	public List<ItemStack> getRandomItems(int amount) {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/select_random_items.sql")
					.setInt(1, amount)
					.executeQuery()) {

				List<ItemStack> items = new ArrayList<>();
				while(set.next()) {
					String json = set.getString("item_data");
					ItemStack item = NBT.itemStackFromNBT(NBT.parseNBT(json));
					items.add(item);
				}
				return items;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get random items: " + e.getMessage());
			e.printStackTrace();
		}
		return List.of();
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

	public void addReturnedBids(Map<UUID, Integer> bids) {
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

}
