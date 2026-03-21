package com.ttm.push.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class StaticFileHandler implements HttpHandler
{
	private static final Logger log = LoggerFactory.getLogger(StaticFileHandler.class);

	private static final Map<String, String> MIME_TYPES = Map.of(
		".html", "text/html; charset=utf-8",
		".css", "text/css; charset=utf-8",
		".js", "application/javascript; charset=utf-8",
		".json", "application/json; charset=utf-8",
		".png", "image/png",
		".svg", "image/svg+xml",
		".ico", "image/x-icon"
	);

	private final Path baseDir;

	public StaticFileHandler(Path baseDir)
	{
		this.baseDir = baseDir.toAbsolutePath().normalize();
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException
	{
		if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			exchange.sendResponseHeaders(405, -1);
			return;
		}

		String path = exchange.getRequestURI().getPath();

		// Strip /push prefix
		if (path.startsWith("/push"))
		{
			path = path.substring("/push".length());
		}
		if (path.isEmpty() || "/".equals(path))
		{
			path = "/index.html";
		}

		// Directory traversal protection
		Path filePath = baseDir.resolve(path.substring(1)).normalize();
		if (!filePath.startsWith(baseDir))
		{
			log.warn("Directory traversal attempt: {}", exchange.getRequestURI());
			exchange.sendResponseHeaders(403, -1);
			return;
		}

		if (!Files.exists(filePath) || Files.isDirectory(filePath))
		{
			exchange.sendResponseHeaders(404, -1);
			return;
		}

		String fileName = filePath.getFileName().toString();
		String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
		String contentType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");

		exchange.getResponseHeaders().add("Content-Type", contentType);

		// Service-Worker-Allowed header for sw.js
		if ("sw.js".equals(fileName))
		{
			exchange.getResponseHeaders().add("Service-Worker-Allowed", "/");
		}

		byte[] data = Files.readAllBytes(filePath);
		exchange.sendResponseHeaders(200, data.length);
		try (OutputStream os = exchange.getResponseBody())
		{
			os.write(data);
		}
	}
}
