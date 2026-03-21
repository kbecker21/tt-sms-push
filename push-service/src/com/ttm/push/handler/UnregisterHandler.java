package com.ttm.push.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.ttm.push.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class UnregisterHandler implements HttpHandler
{
	private static final Logger log = LoggerFactory.getLogger(UnregisterHandler.class);
	private final Database db;
	private final Gson gson = new Gson();

	public UnregisterHandler(Database db)
	{
		this.db = db;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException
	{
		// CORS headers
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
		exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

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

		try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
		{
			JsonObject body = JsonParser.parseReader(reader).getAsJsonObject();
			String playerId = body.get("playerId").getAsString();
			JsonObject subscription = body.getAsJsonObject("subscription");
			String endpoint = subscription.get("endpoint").getAsString();

			db.deleteDeviceByPlayerAndEndpoint(playerId, endpoint);

			JsonObject response = new JsonObject();
			response.addProperty("success", true);
			response.addProperty("message", "Player " + playerId + " unregistered");
			sendResponse(exchange, 200, gson.toJson(response));
			log.info("Player {} unregistered", playerId);
		}
		catch (Exception e)
		{
			log.error("Unregistration failed", e);
			sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
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
