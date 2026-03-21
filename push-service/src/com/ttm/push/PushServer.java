package com.ttm.push;

import com.sun.net.httpserver.HttpServer;
import com.ttm.push.db.Database;
import com.ttm.push.handler.RegistrationHandler;
import com.ttm.push.handler.SendHandler;
import com.ttm.push.handler.StaticFileHandler;
import com.ttm.push.push.WebPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.log4j.PropertyConfigurator;
//import java.util.logging.Level;
//import java.util.logging.Logger;


import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.Executors;

public class PushServer
{
        private static final Logger log = LoggerFactory.getLogger(PushServer.class);

	public static void main(String[] args) throws Exception
	{
                PropertyConfigurator.configure("conf/log4j.properties");
            
                Properties props = loadProperties();

		int port = Integer.parseInt(props.getProperty("server.port", "8080"));
		String apiKey = props.getProperty("push.api.key", "changeme");
		String webDir = props.getProperty("web.directory", "web");

		Database db = new Database(props);
		db.initialize();

		WebPushService pushService = new WebPushService(props);

		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/api/push/register", new RegistrationHandler(db));
		server.createContext("/api/push/send", new SendHandler(db, pushService, apiKey));
		server.createContext("/push", new StaticFileHandler(Path.of(webDir, "push")));
		server.setExecutor(Executors.newFixedThreadPool(10));

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("Shutting down push server...");
 			server.stop(3);
			db.close();
		}));

		server.start();
		log.info("Push server started on port {}", port);
		log.info("PWA available at http://localhost:{}/push/", port);

	}

	private static Properties loadProperties() throws IOException
	{
		Properties props = new Properties();
		Path[] candidates = {
			Path.of("conf", "push-service.properties"),
			Path.of("push-service.properties")
		};
		for (Path p : candidates)
		{
			if (Files.exists(p))
			{
				try (FileInputStream fis = new FileInputStream(p.toFile()))
				{
					props.load(fis);
				}
				log.info("Loaded configuration from {}", p);
				return props;
			}
		}
		log.warn("No configuration file found, using defaults");
		return props;
	}
}
