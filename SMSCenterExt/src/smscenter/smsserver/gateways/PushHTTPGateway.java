package smscenter.smsserver.gateways;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.smslib.GatewayException;
import org.smslib.OutboundMessage;
import org.smslib.TimeoutException;
import org.smslib.helper.Logger;

/**
 * SMSLib Gateway implementation that sends messages via HTTP POST
 * to the PushServiceProvider for delivery as push notifications.
 *
 * Includes recipient mapping: the SMSServer writes telephone numbers
 * as recipients, but the push-service expects player numbers (plNr).
 * This gateway performs a reverse lookup in smscenter_phones.
 */
public class PushHTTPGateway extends org.smslib.AGateway
{
	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

	private final String serviceUrl;
	private final String apiKey;
	private final OkHttpClient httpClient;
	private final Gson gson;

	private String dbConnectionUrl;
	private Connection dbConnection;

	public PushHTTPGateway(String id, String serviceUrl, String apiKey)
	{
		super(id);
		this.serviceUrl = serviceUrl.endsWith("/") ? serviceUrl.substring(0, serviceUrl.length() - 1) : serviceUrl;
		this.apiKey = apiKey;
		this.httpClient = new OkHttpClient.Builder()
				.connectTimeout(10, TimeUnit.SECONDS)
				.writeTimeout(10, TimeUnit.SECONDS)
				.readTimeout(30, TimeUnit.SECONDS)
				.build();
		this.gson = new Gson();
	}

	/**
	 * Configure database access for recipient mapping (phone → plNr).
	 * Called by PushGateway.create() with the SMSServer properties.
	 */
	public void configureDatabase(Properties props, String interfaceId)
	{
		String dbName = props.getProperty(interfaceId + ".database", "");
		String server = props.getProperty(interfaceId + ".server", "");
		boolean windowsAuth = "yes".equals(props.getProperty(interfaceId + ".windowsAuth", "no"));
		String user = props.getProperty(interfaceId + ".user", "");
		String pwd = props.getProperty(interfaceId + ".password", "");

		if (dbName.isEmpty() || server.isEmpty())
		{
			Logger.getInstance().logWarn("PushHTTPGateway: No database configured for recipient mapping. " +
				"Phone numbers will be used as playerId (may not match push registrations).", null, getGatewayId());
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("jdbc:sqlserver://");
		if (server.equalsIgnoreCase("(local)"))
			sb.append("localhost");
		else
			sb.append(server);
		sb.append(";");

		String[] database = dbName.split("\\\\");
		sb.append("databaseName=").append(database[0]).append(";");
		if (database.length > 1)
			sb.append("instanceName=").append(database[1]).append(";");

		if (windowsAuth)
			sb.append("integratedSecurity=true;trustServerCertificate=true;encrypt=true;");
		else
			sb.append("user=").append(user).append(";password=").append(pwd)
				.append(";integratedSecurity=false;trustServerCertificate=true;encrypt=true;");

		this.dbConnectionUrl = sb.toString();
		Logger.getInstance().logInfo("PushHTTPGateway: Database configured for recipient mapping.", null, getGatewayId());
	}

	@Override
	public void startGateway() throws TimeoutException, GatewayException, IOException, InterruptedException
	{
		super.startGateway();
		Logger.getInstance().logInfo("PushHTTPGateway started: " + serviceUrl, null, getGatewayId());
	}

	@Override
	public void stopGateway() throws TimeoutException, GatewayException, IOException, InterruptedException
	{
		super.stopGateway();
		httpClient.dispatcher().executorService().shutdown();
		httpClient.connectionPool().evictAll();
		closeDbConnection();
		Logger.getInstance().logInfo("PushHTTPGateway stopped.", null, getGatewayId());
	}

	@Override
	public boolean sendMessage(OutboundMessage msg) throws TimeoutException, GatewayException, IOException, InterruptedException
	{
		String recipient = msg.getRecipient();
		String playerId = resolvePlayerId(recipient);
		String messageText = msg.getText();

		JsonObject requestJson = new JsonObject();
		requestJson.addProperty("playerId", playerId);
		requestJson.addProperty("message", messageText);

		RequestBody body = RequestBody.create(gson.toJson(requestJson), JSON);
		Request request = new Request.Builder()
				.url(serviceUrl + "/api/push/send")
				.addHeader("Authorization", "Bearer " + apiKey)
				.addHeader("Content-Type", "application/json")
				.post(body)
				.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			String responseBody = response.body() != null ? response.body().string() : "";

			if (response.isSuccessful())
			{
				JsonObject result = JsonParser.parseString(responseBody).getAsJsonObject();
				if (result.has("success") && result.get("success").getAsBoolean())
				{
					if (result.has("timestamp"))
					{
						msg.setRefNo(result.get("timestamp").getAsString());
					}
					msg.setMessageStatus(OutboundMessage.MessageStatuses.SENT);
					Logger.getInstance().logInfo("Push sent to player " + playerId +
						" (recipient: " + recipient + "), sent: " + result.get("sent").getAsInt(), null, getGatewayId());
					return true;
				}
			}

			msg.setMessageStatus(OutboundMessage.MessageStatuses.FAILED);
			Logger.getInstance().logError("Push send failed for player " + playerId + ": HTTP " + response.code() + " - " + responseBody, null, getGatewayId());
			return false;
		}
		catch (Exception e)
		{
			msg.setMessageStatus(OutboundMessage.MessageStatuses.FAILED);
			Logger.getInstance().logError("Push send exception for player " + playerId + ": " + e.getMessage(), e, getGatewayId());
			return false;
		}
	}

	/**
	 * Resolve a telephone number to a player number (plNr) by looking up
	 * smscenter_phones. If no match is found, the original recipient is returned.
	 */
	private String resolvePlayerId(String phoneNumber)
	{
		if (dbConnectionUrl == null)
		{
			return phoneNumber;
		}

		// Normalize: try with and without leading "+"
		String normalized = phoneNumber.trim();
		String withPlus = normalized.startsWith("+") ? normalized : "+" + normalized;
		String withoutPlus = normalized.startsWith("+") ? normalized.substring(1) : normalized;

		try
		{
			Connection con = getDbConnection();
			String sql = "SELECT DISTINCT plNr FROM smscenter_phones WHERE phone LIKE ? OR phone LIKE ?";
			try (PreparedStatement ps = con.prepareStatement(sql))
			{
				ps.setString(1, "%" + withPlus);
				ps.setString(2, "%" + withoutPlus);
				try (ResultSet rs = ps.executeQuery())
				{
					if (rs.next())
					{
						String plNr = rs.getString("plNr").trim();
						Logger.getInstance().logInfo("PushHTTPGateway: Resolved phone " + phoneNumber +
							" to player " + plNr, null, getGatewayId());
						return plNr;
					}
				}
			}
		}
		catch (SQLException e)
		{
			Logger.getInstance().logError("PushHTTPGateway: DB lookup failed for " + phoneNumber +
				", using phone as fallback: " + e.getMessage(), e, getGatewayId());
			closeDbConnection();
		}

		Logger.getInstance().logWarn("PushHTTPGateway: No player found for phone " + phoneNumber +
			", using phone number as playerId", null, getGatewayId());
		return phoneNumber;
	}

	private synchronized Connection getDbConnection() throws SQLException
	{
		if (dbConnection == null || dbConnection.isClosed())
		{
			dbConnection = DriverManager.getConnection(dbConnectionUrl);
			dbConnection.setAutoCommit(false);
		}
		return dbConnection;
	}

	private void closeDbConnection()
	{
		try
		{
			if (dbConnection != null && !dbConnection.isClosed())
			{
				dbConnection.close();
			}
		}
		catch (SQLException e)
		{
			// ignore
		}
		dbConnection = null;
	}

	@Override
	public int getQueueSchedulingInterval()
	{
		return 500;
	}
}
