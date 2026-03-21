package com.ttm.push.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.ttm.push.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class StatusHandler implements HttpHandler
{
	private static final Logger log = LoggerFactory.getLogger(StatusHandler.class);
	private final Database db;
	private final Gson gson = new Gson();

	public StatusHandler(Database db)
	{
		this.db = db;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException
	{
		// CORS headers
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
		exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

		if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			exchange.sendResponseHeaders(204, -1);
			return;
		}

		if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
			return;
		}

		try
		{
			Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
			String playerId = params.get("playerId");
			String endpoint = params.get("endpoint");

			if (playerId == null || endpoint == null)
			{
				sendResponse(exchange, 400, "{\"error\":\"Missing playerId or endpoint parameter\"}");
				return;
			}

			boolean registered = db.isDeviceRegistered(playerId, endpoint);

			JsonObject response = new JsonObject();
			response.addProperty("registered", registered);
			sendResponse(exchange, 200, gson.toJson(response));
		}
		catch (Exception e)
		{
			log.error("Status check failed", e);
			sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
		}
	}

	private Map<String, String> parseQuery(String query)
	{
		Map<String, String> params = new LinkedHashMap<>();
		if (query == null || query.isEmpty())
		{
			return params;
		}
		for (String pair : query.split("&"))
		{
			int idx = pair.indexOf('=');
			if (idx > 0)
			{
				String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
				String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
				params.put(key, value);
			}
		}
		return params;
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
