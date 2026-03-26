package com.ttm.push.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.ttm.push.db.Database;
import com.ttm.push.db.Database.DeviceRegistration;
import com.ttm.push.push.WebPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class SendHandler implements HttpHandler
{
	private static final Logger log = LoggerFactory.getLogger(SendHandler.class);
	private final Database db;
	private final WebPushService pushService;
	private final String apiKey;
	private final Gson gson = new Gson();

	public SendHandler(Database db, WebPushService pushService, String apiKey)
	{
		this.db = db;
		this.pushService = pushService;
		this.apiKey = apiKey;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException
	{
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
		exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

		if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			exchange.sendResponseHeaders(204, -1);
			return;
		}

		if (!"POST".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
			return;
		}

		// Auth check
		String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
		if (authHeader == null || !authHeader.equals("Bearer " + apiKey))
		{
			sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
			return;
		}

		try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
		{
			JsonObject body = JsonParser.parseReader(reader).getAsJsonObject();
			String playerId = body.get("playerId").getAsString();
			String message = body.get("message").getAsString();

			List<DeviceRegistration> devices = db.getDevicesForPlayer(playerId);
			if (devices.isEmpty())
			{
				log.warn("No devices registered for player {}", playerId);
				JsonObject response = new JsonObject();
				response.addProperty("success", true);
				response.addProperty("sent", 0);
				response.addProperty("message", "No devices registered for player " + playerId);
				sendResponse(exchange, 200, gson.toJson(response));
				return;
			}

			int sent = 0;
			for (DeviceRegistration device : devices)
			{
				try
				{
					// Include playerId in payload so SW can route messages per player
				JsonObject payload = new JsonObject();
				payload.addProperty("playerId", playerId);
				payload.addProperty("message", message);
				int statusCode = pushService.sendPush(device, gson.toJson(payload));
					if (statusCode == 201 || statusCode == 202)
					{
						sent++;
					}
					else if (statusCode == 410 || statusCode == 404)
					{
						log.info("Subscription expired for player {}, removing", playerId);
						db.deleteDevice(device.id);
					}
					else
					{
						log.warn("Push send returned HTTP {} for player {}", statusCode, playerId);
					}
				}
				catch (Exception e)
				{
					log.error("Failed to send push to device {} for player {}", device.id, playerId, e);
				}
			}

			String timestamp = ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.ENGLISH));

			JsonObject response = new JsonObject();
			response.addProperty("success", true);
			response.addProperty("sent", sent);
			response.addProperty("timestamp", timestamp);
			sendResponse(exchange, 200, gson.toJson(response));
			log.info("Push sent to player {}: {}/{} devices", playerId, sent, devices.size());
		}
		catch (Exception e)
		{
			log.error("Send failed", e);
			sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
		}
	}

	private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException
	{
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(statusCode, bytes.length);
		try (OutputStream os = exchange.getResponseBody())
		{
			os.write(bytes);
		}
	}
}
