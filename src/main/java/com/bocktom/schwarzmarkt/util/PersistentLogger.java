package com.bocktom.schwarzmarkt.util;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class PersistentLogger {

	private static final String FILE = Path.of(Schwarzmarkt.plugin.getDataPath().toString(), "bids.log").toString();
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

	public static void logBid(int auctionId, Player player, int amount) {
		log("BID | Auction ID: " + auctionId + " | Bid Amount: " + amount + " | Player: " + player.getName() + " (" + player.getUniqueId() + ")");
	}

	public static void logBidRollbackFailed(int auctionId, Player player, int amount) {
		log("ROLLBACK FAILED | Auction ID: " + auctionId + " | Bid Amount: " + amount + " | Player: " + player.getName() + " (" + player.getUniqueId() + ")");
	}

	public static void logAuctionStart(int auctionId, ItemStack item) {
		log("AUCTION START | Auction ID: " + auctionId + " | Item: " + NBT.itemStackToNBT(item).toString());
	}

	public static void logWinningsFailed(int id, UUID highestBidder, ItemStack item) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(highestBidder);
		log("WINNINGS FAILED | Auction ID: " + id + " | Winner: " + player.getName() + " (" + highestBidder + ")" + " | Item: " + NBT.itemStackToNBT(item).toString());
	}

	public static void logAuctionEnd(int auctionId, UUID winner, int amount) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(winner);
		log("AUCTION END | Auction ID: " + auctionId + " | Bid Amount: " + amount + " | Winner: " + player.getName() + " (" + winner + ")");
	}

	public static void logReturnBidFailed(UUID key, Integer value) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(key);
		log("RETURN BID FAILED | Amount: " + value + " | Player: " + player.getName() + " (" + key + ")");
	}

	public static void logDepositFailed(Player player, int amount) {
		log("DEPOSIT FAILED | Amount: " + amount + " | Player: " + player.getName() + " (" + player.getUniqueId() + ")");
	}

	public static void logWithdrawFailed(Player player, int amount) {
		log("WITHDRAW FAILED | Amount: " + amount + " | Player: " + player.getName() + " (" + player.getUniqueId() + ")");
	}

	private static void log(String message) {
		String timestamp = FORMATTER.format(LocalDateTime.now());
		String logEntry = timestamp + " | " + message;

		// write logEntry to FILE
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE, true))) {
			writer.write(logEntry);
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void init() {
		Path path = Paths.get(FILE);
		try {
			if(!Files.exists(path))
				Files.createFile(path);
			cleanup();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void cleanup() {

		// delete logs older than 30 days
		Path path = Paths.get(FILE);
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
