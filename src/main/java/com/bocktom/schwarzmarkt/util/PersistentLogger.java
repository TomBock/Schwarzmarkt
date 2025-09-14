package com.bocktom.schwarzmarkt.util;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class PersistentLogger {

	private static final String FILE = Path.of(Schwarzmarkt.plugin.getDataPath().toString(), "bids.log").toString();
	private static final String PLAYER_FILE = Path.of(Schwarzmarkt.plugin.getDataPath().toString(), "player_bids.log").toString();
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

	public static void logBid(boolean isPlayerAuction, int auctionId, Player player, int amount) {
		log(isPlayerAuction, "BID | Auction ID: " + auctionId + " | Bid Amount: " + amount + " | Player: " + player.getName() + " (" + player.getUniqueId() + ")");
	}

	public static void logBidRollbackFailed(boolean isPlayerAuction, int auctionId, Player player, int amount) {
		log(isPlayerAuction, "ROLLBACK FAILED | Auction ID: " + auctionId + " | Bid Amount: " + amount + " | Player: " + player.getName() + " (" + player.getUniqueId() + ")");
	}

	public static void logServerAuctionStart(int auctionId, ItemStack item) {
		log(false, "AUCTION START | Auction ID: " + auctionId + " | Item: " + NBT.itemStackToNBT(item).toString());
	}

	public static void logPlayerAuctionStart(int auctionId, ItemStack item, UUID ownerUuid) {
		OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
		log(true, "AUCTION START | Auction ID: " + auctionId + " | Player: " + owner.getName() + "| Item: " + NBT.itemStackToNBT(item).toString() + " (" + ownerUuid + ")");
	}

	public static void logWinningsFailed(boolean isPlayerAuction, int id, UUID highestBidder, ItemStack item) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(highestBidder);
		log(isPlayerAuction, "WINNINGS FAILED | Auction ID: " + id + " | Winner: " + player.getName() + " (" + highestBidder + ")" + " | Item: " + NBT.itemStackToNBT(item).toString());
	}

	public static void logItemReturnFailed(int id, UUID highestBidder, ItemStack item) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(highestBidder);
		log(true, "ITEM RETURN FAILED | Auction ID: " + id + " | Winner: " + player.getName() + " (" + highestBidder + ")" + " | Item: " + NBT.itemStackToNBT(item).toString());
	}

	public static void logAuctionEnd(boolean isPlayerAuction, int auctionId, UUID winner, int amount) {
		String winnerName = winner == null ? "None" : Bukkit.getOfflinePlayer(winner).getName();
		log(isPlayerAuction, "AUCTION END | Auction ID: " + auctionId + " | Bid Amount: " + amount + " | Winner: " + winnerName + " (" + winner + ")");
	}

	public static void logAuctionEndNotSold(int auctionId, ItemStack item, UUID ownerUuid) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(ownerUuid);
		log(true, "PLAYER ITEM NOT SOLD | Auction ID: " + auctionId + " | Owner: " + player.getName() + "| Item: " + NBT.itemStackToNBT(item).toString() + " (" + ownerUuid + ")");
	}

	public static void logReturnBidFailed(boolean isPlayerAuction, UUID key, Integer value) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(key);
		log(isPlayerAuction, "RETURN BID FAILED | Amount: " + value + " | Player: " + player.getName() + " (" + key + ")");
	}

	public static void logDepositFailed(boolean isPlayerAuction, OfflinePlayer player, int amount) {
		log(isPlayerAuction, "DEPOSIT FAILED | Amount: " + amount + " | Player: " + player.getName() + " (" + player.getUniqueId() + ")");
	}

	public static void logRevenueFailed(boolean isPlayerAuction, OfflinePlayer player, int amount) {
		log(isPlayerAuction, "REVENUE FAILED | Amount: " + amount + " | Player: " + player.getName() + " (" + player.getUniqueId() + ")");
	}

	public static void logWithdrawFailed(boolean isPlayerAuction, Player player, int amount) {
		log(isPlayerAuction, "WITHDRAW FAILED | Amount: " + amount + " | Player: " + player.getName() + " (" + player.getUniqueId() + ")");
	}

	public static void logItemRemovalFailed(boolean isPlayerAuction, int auctionId, ItemStack item, UUID ownerUuid) {
		OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
		log(isPlayerAuction, "ITEM REMOVAL FAILED | Auction ID: " + auctionId + " | Player: " + owner.getName() + "| Item: " + NBT.itemStackToNBT(item).toString() + " (" + ownerUuid + ")");
	}

	public static void logPlayerAuctionItemSetup(Player player, int total, List<DbItem> itemsAdded, Collection<ItemStack> itemsRemoved) {
		String itemsAddedStr = toStringList(itemsAdded.stream().map(dbItem -> dbItem.item));
		String itemsRemovedStr = toStringList(itemsRemoved.stream());
		log(true, "ITEM SETUP | Player: " + player.getName() +
				" | Items Added: " + itemsAdded.size() + (!itemsAdded.isEmpty() ? itemsAddedStr : "") +
				" | Items Removed: " + itemsRemoved.size() + (!itemsRemoved.isEmpty() ? itemsRemovedStr : "") +
				" | Total Return/Cost: " + total + " (" + player.getUniqueId() + ")");
	}

	private static String toStringList(Stream<ItemStack> items) {
		return " (" + items
				.map(item ->
						(item.getItemMeta() != null && item.getItemMeta().hasDisplayName())
						? item.getItemMeta().getDisplayName() + "[" + item.getAmount() + "]"
						: item.getType().name() + "[" + item.getAmount() + "]")
				.reduce((a, b) -> a + ", " + b)
				.orElse("None") + ")";
	}

	private static void log(boolean isPlayerAuction, String message) {
		String timestamp = FORMATTER.format(LocalDateTime.now());
		String logEntry = timestamp + " | " + message;

		// write logEntry to FILE
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(isPlayerAuction ? PLAYER_FILE : FILE, true))) {
			writer.write(logEntry);
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void init() {
		Path path = Paths.get(FILE);
		Path playerPath = Paths.get(PLAYER_FILE);
		try {
			if(!Files.exists(path))
				Files.createFile(path);
			cleanup(FILE);
			if(!Files.exists(playerPath))
				Files.createFile(playerPath);
			cleanup(PLAYER_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void cleanup(String filepath) {

		// delete logs older than 30 days
		Path path = Paths.get(filepath);
		try (Stream<String> lines = Files.lines(path)) {
			List<String> filteredLines = lines
					.filter(line -> {
						try {
							String datePart = line.split(" \\| ")[0]; // Extract date
							LocalDateTime logDate = LocalDateTime.parse(datePart, FORMATTER);
							return logDate.isAfter(LocalDateTime.now().minusMonths(1)); // Keep only recent logs
						} catch (Exception e) {
							return true; // Keep invalid lines to avoid data loss
						}
					})
					.toList();

			Files.write(path, filteredLines, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
