package com.bocktom.schwarzmarkt.util;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class Config {

	private Schwarzmarkt plugin;
	public FileConfiguration items;
	public FileConfiguration auction;

	private final String ITEMS_CFG = "items.yml";
	private final String AUCTION_CFG = "auction.yml";

	public Config() {
		this.plugin = Schwarzmarkt.plugin;
		loadConfig();
	}

	private void loadConfig() {
		File itemsConfig = new File(plugin.getDataPath() + File.separator + ITEMS_CFG);
		File auctionConfig = new File(plugin.getDataPath() + File.separator + AUCTION_CFG);

		if(!Files.exists(itemsConfig.toPath()) || !itemsConfig.isFile()) {
			plugin.getDataFolder().mkdirs();
			copyDefaultConfig(ITEMS_CFG);
		}
		if(!Files.exists(auctionConfig.toPath()) || !auctionConfig.isFile()) {
			plugin.getDataFolder().mkdirs();
			copyDefaultConfig(AUCTION_CFG);
		}

		items = YamlConfiguration.loadConfiguration(itemsConfig);
		auction = YamlConfiguration.loadConfiguration(auctionConfig);
	}

	private void copyDefaultConfig(String configName) {
		try (InputStream inputStream = plugin.getResource(configName)) {
			if(inputStream == null) {
				plugin.getLogger().warning("Could not find default config file: " + configName);
				return;
			}

			Files.copy(inputStream, new File(plugin.getDataPath().toString(), configName).toPath());
		} catch (Exception e) {
			plugin.getLogger().warning("Could not copy default config file: " + configName);
			e.printStackTrace();
		}
	}

	public void save() {
		try {
			items.save(new File(plugin.getDataPath().toString(), ITEMS_CFG));
			auction.save(new File(plugin.getDataPath().toString(), AUCTION_CFG));
		} catch (Exception e) {
			plugin.getLogger().warning("Could not save config files");
			e.printStackTrace();
		}
	}
}
