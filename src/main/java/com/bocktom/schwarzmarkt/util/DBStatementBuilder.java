package com.bocktom.schwarzmarkt.util;

import com.bocktom.schwarzmarkt.Schwarzmarkt;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBStatementBuilder {

	private final PreparedStatement statement;

	public DBStatementBuilder(Connection con, String sqlFile) throws SQLException, IOException {

		InputStream input = Schwarzmarkt.plugin.getResource(sqlFile);
		if(input == null)
			throw new IOException("Resource not found: " + sqlFile);
		String sql = new String(input.readAllBytes());

		statement = con.prepareStatement(sql);
	}

	public DBStatementBuilder setInt(int parameterIndex, int value) throws SQLException {
		statement.setInt(parameterIndex, value);
		return this;
	}

	public DBStatementBuilder setBytes(int parameterIndex, byte[] value) throws SQLException {
		statement.setBytes(parameterIndex, value);
		return this;
	}

	public DBStatementBuilder setLong(int parameterIndex, long amount) throws SQLException {
		statement.setLong(parameterIndex, amount);
		return this;
	}


	public DBStatementBuilder setString(int parameterIndex, String text) throws SQLException {
		statement.setString(parameterIndex, text);
		return this;
	}

	public int executeUpdate() throws SQLException {
		return statement.executeUpdate();
	}

	public ResultSet executeQuery() throws SQLException {
		return statement.executeQuery();
	}
}
