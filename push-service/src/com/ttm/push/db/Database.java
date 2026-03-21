package com.ttm.push.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Database
{
	private static final Logger log = LoggerFactory.getLogger(Database.class);

	private final String dbUrl;
	private final String dbUser;
	private final String dbPassword;
	private Connection connection;

	public Database(Properties props)
	{
		this.dbUrl = props.getProperty("db.url", "jdbc:h2:file:./data/pushdb");
		this.dbUser = props.getProperty("db.username", "sa");
		this.dbPassword = props.getProperty("db.password", "");
	}

	public void initialize() throws SQLException
	{
		getConnection();
		createTable();
		log.info("Database initialized: {}", dbUrl);
	}

	private synchronized Connection getConnection() throws SQLException
	{
		if (connection == null || connection.isClosed())
		{
			connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
			connection.setAutoCommit(true);
		}
		return connection;
	}

	private void createTable() throws SQLException
	{
		Connection conn = getConnection();
		boolean isMsSql = dbUrl.contains("sqlserver");

		String sql;
		if (isMsSql)
		{
                        // Use MSSQL Server
                        sql = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'device_registrations') " +
                                "CREATE TABLE device_registrations (" +
                                "id BIGINT IDENTITY(1,1) PRIMARY KEY, " +
                                "player_id VARCHAR(50) NOT NULL, " +
                                "endpoint VARCHAR(2000) NOT NULL, " +
                                "p256dh VARCHAR(500) NOT NULL, " +
                                "auth_key VARCHAR(200) NOT NULL, " +
                                "created_at DATETIME2 DEFAULT GETDATE()" +
                                ")";
		}
		else
		{
			// Use H2 database alternatively
                        sql = "CREATE TABLE IF NOT EXISTS device_registrations (" +
				"id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
				"player_id VARCHAR(50) NOT NULL, " +
				"endpoint VARCHAR(2000) NOT NULL, " +
				"p256dh VARCHAR(500) NOT NULL, " +
				"auth_key VARCHAR(200) NOT NULL, " +
				"created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
				")";
		}

		try (Statement stmt = conn.createStatement())
		{
			stmt.execute(sql);
		}
	}

	public synchronized void registerDevice(String playerId, String endpoint, String p256dh, String authKey)
		throws SQLException
	{
		Connection conn = getConnection();

		// Upsert: check if combination exists
		String selectSql = "SELECT id FROM device_registrations WHERE player_id = ? AND endpoint = ?";
		try (PreparedStatement ps = conn.prepareStatement(selectSql))
		{
			ps.setString(1, playerId);
			ps.setString(2, endpoint);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					// Update existing
					long id = rs.getLong("id");
					String updateSql = "UPDATE device_registrations SET p256dh = ?, auth_key = ? WHERE id = ?";
					try (PreparedStatement ups = conn.prepareStatement(updateSql))
					{
						ups.setString(1, p256dh);
						ups.setString(2, authKey);
						ups.setLong(3, id);
						ups.executeUpdate();
					}
					log.info("Updated registration for player {} (endpoint existing)", playerId);
					return;
				}
			}
		}

		// Insert new
		String insertSql = "INSERT INTO device_registrations (player_id, endpoint, p256dh, auth_key) VALUES (?, ?, ?, ?)";
		try (PreparedStatement ps = conn.prepareStatement(insertSql))
		{
			ps.setString(1, playerId);
			ps.setString(2, endpoint);
			ps.setString(3, p256dh);
			ps.setString(4, authKey);
			ps.executeUpdate();
		}
		log.info("Registered new device for player {}", playerId);
	}

	public synchronized List<DeviceRegistration> getDevicesForPlayer(String playerId) throws SQLException
	{
		List<DeviceRegistration> devices = new ArrayList<>();
		Connection conn = getConnection();

		String sql = "SELECT id, endpoint, p256dh, auth_key FROM device_registrations WHERE player_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql))
		{
			ps.setString(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					devices.add(new DeviceRegistration(
						rs.getLong("id"),
						playerId,
						rs.getString("endpoint"),
						rs.getString("p256dh"),
						rs.getString("auth_key")
					));
				}
			}
		}
		return devices;
	}

	public synchronized void deleteDevice(long id) throws SQLException
	{
		Connection conn = getConnection();
		String sql = "DELETE FROM device_registrations WHERE id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql))
		{
			ps.setLong(1, id);
			ps.executeUpdate();
		}
		log.info("Deleted expired device registration id={}", id);
	}

	public void close()
	{
		try
		{
			if (connection != null && !connection.isClosed())
			{
				connection.close();
			}
		}
		catch (SQLException e)
		{
			log.error("Error closing database connection", e);
		}
	}

	public static class DeviceRegistration
	{
		public final long id;
		public final String playerId;
		public final String endpoint;
		public final String p256dh;
		public final String authKey;

		public DeviceRegistration(long id, String playerId, String endpoint, String p256dh, String authKey)
		{
			this.id = id;
			this.playerId = playerId;
			this.endpoint = endpoint;
			this.p256dh = p256dh;
			this.authKey = authKey;
		}
	}
}
