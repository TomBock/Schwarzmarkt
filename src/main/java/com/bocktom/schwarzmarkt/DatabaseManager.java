package com.bocktom.schwarzmarkt;

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

		try (Connection con = getConnection()) {
			int result = new DBStatementBuilder(con, "sql/create_tables.sql")
					.executeUpdate();

			if(result > 0)
				plugin.getLogger().info("Created tables");
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to setup database: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void addAuctions(Collection<ItemStack> items) {
		try (Connection con = getConnection()) {
			con.setAutoCommit(false);
			for(ItemStack item : items) {
				String json = NBT.itemStackToNBT(item).toString();
				new DBStatementBuilder(con, "sql/insert_auction.sql")
						.setString(1, json)
						.executeUpdate();
			}
			con.commit();
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to add auction: " + e.getMessage());
			e.printStackTrace();
		}
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

	public Map<Integer, ItemStack> getAuctions() {
		try (Connection con = getConnection()) {
			try(ResultSet set = new DBStatementBuilder(con, "sql/select_auctions.sql")
					.executeQuery()) {

				Map<Integer, ItemStack> auctions = new TreeMap<>();
				while(set.next()) {
					int id = set.getInt("id");
					String json = set.getString("item_data");
					ItemStack item = NBT.itemStackFromNBT(NBT.parseNBT(json));
					auctions.put(id, item);
				}
				return auctions;
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get auctions: " + e.getMessage());
			e.printStackTrace();
		}
		return Map.of();
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

			result += new DBStatementBuilder(con, "sql/update_highest_bid.sql")
					.setInt(1, amount)
					.setBytes(2, uuid)
					.setInt(3, auctionId)
					.setInt(4, amount)
					.executeUpdate();

			con.commit();
			return result > 0;
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to place bid: " + e.getMessage());
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
			}
		} catch (SQLException | IOException e) {
			plugin.getLogger().warning("Failed to get winnings: " + e.getMessage());
			e.printStackTrace();
		}
		return Map.of();
	}

	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection(this.dbUrl);
	}
}
