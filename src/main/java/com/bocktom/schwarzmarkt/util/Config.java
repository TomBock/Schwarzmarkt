package com.bocktom.schwarzmarkt.util;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

public class Config {

	public static InternalConfig msg;
	public static InternalConfig gui;

	private Schwarzmarkt plugin;

	public Config() {
		this.plugin = Schwarzmarkt.plugin;
		loadConfig();
	}

	public static Optional<InternalConfig> getInv(String arg) {
		switch (arg) {
			case "auctioninv":
				return Optional.of(gui);
			default:
				return Optional.empty();
		}
	}

	private void loadConfig() {
		// Check if directory exists
		if(!plugin.getDataFolder().exists()) {
			plugin.getDataFolder().mkdir();
		}

		msg = new InternalConfig(plugin, "msg.yml");
		gui = new InternalConfig(plugin, "gui.yml");
	}

	public static class InternalConfig {

		public final FileConfiguration get;
		private final File file;
		private final JavaPlugin plugin;

		public InternalConfig(JavaPlugin plugin, String configFile) {
			this.plugin = plugin;
			this.file = new File(plugin.getDataPath() + File.separator + configFile);

			if (!Files.exists(file.toPath()) || !file.isFile()) {
				plugin.getDataFolder().mkdirs();
				copyDefaultConfig(configFile);
			}

			this.get = YamlConfiguration.loadConfiguration(file);
			applyDefaults(configFile);
		}

		/**
		 * Backs the config on disk with the one bundled in the jar.
		 * <p>
		 * The default file is only ever copied when none exists, so a server that has been
		 * running for a while never sees keys added in a later version - a lookup would come
		 * back empty and the feature behind it would silently do nothing. Registering the
		 * bundled config as defaults makes those keys resolve without touching the file on
		 * disk, which keeps the admin's edits and comments intact.
		 */
		private void applyDefaults(String configName) {
			try (InputStream inputStream = plugin.getResource(configName)) {
				if (inputStream == null)
					return;

				get.setDefaults(YamlConfiguration.loadConfiguration(
						new InputStreamReader(inputStream, StandardCharsets.UTF_8)));
			} catch (IOException e) {
				plugin.getLogger().warning("Could not read default config file: " + configName);
			}
		}

		private void copyDefaultConfig(String configName) {
			try (InputStream inputStream = plugin.getResource(configName)) {
				if (inputStream == null) {
					plugin.getLogger().warning("Could not find default config file: " + configName);
					return;
				}

				Files.copy(inputStream, new File(plugin.getDataPath().toString(), configName).toPath());
			} catch (IOException e) {
				plugin.getLogger().warning("Could not copy default config file: " + configName);
				e.printStackTrace();
			}

		}

		public void save() {
			try {
				get.save(file);
			} catch (IOException e) {
				plugin.getLogger().warning("Could not save config files");
				e.printStackTrace();
			}
		}
	}

}
