package com.bocktom.schwarzmarkt.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * One item format step: runs the data fixer over every table that stores serialized items.
 * <p>
 * The whole step is a single transaction. The schema version is written inside that same
 * transaction, after the last row went through, so a database either ends up fully migrated
 * and at the new version, or untouched and at the old one - a failing row, a crash or a
 * killed server never leaves a half-migrated database behind.
 */
public class ItemDataMigration {

	public record Result(int updated, int skipped) {}

	/** Raised when a step cannot complete. The transaction has been rolled back by then. */
	public static class MigrationException extends Exception {
		public MigrationException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private record Row(int id, String itemData) {}

	private final String[][] tables;
	private final int fallbackVersion;
	private final int targetVersion;
	private final ItemDataFixer fixer;

	/**
	 * @param tables (select, update) sql resource pairs; the select must expose {@code id}
	 *               and {@code item_data}, the update takes (item_data, id)
	 * @param fallbackVersion source version for rows whose item data carries no DataVersion
	 * @param targetVersion DataVersion every row is lifted to
	 */
	public ItemDataMigration(String[][] tables, int fallbackVersion, int targetVersion, ItemDataFixer fixer) {
		this.tables = tables;
		this.fallbackVersion = fallbackVersion;
		this.targetVersion = targetVersion;
		this.fixer = fixer;
	}

	/**
	 * Runs the step in its own transaction on {@code con} and records {@code schemaVersion}
	 * before committing. On any failure everything is rolled back.
	 */
	public Result run(Connection con, int schemaVersion) throws MigrationException {
		try {
			con.setAutoCommit(false);
			Result result = migrateRows(con);
			new DBStatementBuilder(con, "sql/v1/insert_or_update_schema_version.sql")
					.setInt(1, schemaVersion)
					.executeUpdate();
			con.commit();
			return result;
		} catch (MigrationException e) {
			rollback(con, e);
			throw e;
		} catch (SQLException | IOException | RuntimeException e) {
			rollback(con, e);
			throw new MigrationException("Item format step to schema version " + schemaVersion + " failed: " + e.getMessage(), e);
		}
	}

	private Result migrateRows(Connection con) throws SQLException, IOException, MigrationException {
		int updated = 0;
		int skipped = 0;

		for (String[] table : tables) {
			String selectSql = table[0];
			String updateSql = table[1];

			// Read the table completely before writing to it, instead of updating rows
			// underneath an open cursor on the same table.
			List<Row> rows = new ArrayList<>();
			try (ResultSet set = new DBStatementBuilder(con, selectSql).executeQuery()) {
				while (set.next()) {
					rows.add(new Row(set.getInt("id"), set.getString("item_data")));
				}
			}

			for (Row row : rows) {
				if(row.itemData() == null)
					continue;

				String fixedData;
				try {
					// Rows are not all written by the same server version: everything stored
					// since the last item format step carries the version that was running at
					// the time. Trusting a single source version would skip the fixes an older
					// row still needs, so each row is fixed from the DataVersion it carries.
					Integer storedVersion = fixer.readDataVersion(row.itemData());
					int itemVersion = storedVersion != null ? storedVersion : fallbackVersion;

					if(itemVersion >= targetVersion) {
						skipped++;
						continue;
					}

					fixedData = fixer.fixUp(row.itemData(), itemVersion, targetVersion);
				} catch (Exception e) {
					throw new MigrationException("Cannot migrate row " + row.id() + " (" + selectSql + "): " + e.getMessage(), e);
				}

				new DBStatementBuilder(con, updateSql)
						.setString(1, fixedData)
						.setInt(2, row.id())
						.executeUpdate();
				updated++;
			}
		}
		return new Result(updated, skipped);
	}

	private static void rollback(Connection con, Exception cause) {
		try {
			con.rollback();
		} catch (SQLException e) {
			cause.addSuppressed(e);
		}
	}
}
