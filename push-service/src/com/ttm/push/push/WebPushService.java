package com.ttm.push.push;

import com.ttm.push.db.Database.DeviceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

/**
 * Sends push notifications via the Web Push Protocol (RFC 8030).
 * Uses JDK HttpClient and WebPushCrypto for encryption/VAPID.
 */
public class WebPushService
{
	private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

	private final ECPublicKey vapidPublicKey;
	private final ECPrivateKey vapidPrivateKey;
	private final String vapidSubject;
	private final HttpClient httpClient;

	public WebPushService(Properties props) throws Exception
	{
		String publicKeyB64 = props.getProperty("vapid.public.key", "");
		String privateKeyB64 = props.getProperty("vapid.private.key", "");
		this.vapidSubject = props.getProperty("vapid.subject", "mailto:admin@ttm.co.at");

		if (publicKeyB64.isEmpty() || "HIER_EINTRAGEN".equals(publicKeyB64))
		{
			throw new IllegalStateException(
				"VAPID keys not configured! Run 'ant generate-vapid-keys' and update conf/push-service.properties");
		}

		Base64.Decoder decoder = Base64.getUrlDecoder();
		this.vapidPublicKey = WebPushCrypto.decodePublicKey(decoder.decode(publicKeyB64));
		this.vapidPrivateKey = WebPushCrypto.decodePrivateKey(decoder.decode(privateKeyB64));

		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

		log.info("WebPushService initialized with VAPID subject: {}", vapidSubject);
	}

	/**
	 * Send a push notification to a registered device.
	 *
	 * @param device  the device registration with endpoint and keys
	 * @param message the message text to send
	 * @return HTTP status code from the push endpoint
	 */
	public int sendPush(DeviceRegistration device, String message) throws Exception
	{
		// Encrypt the payload
		byte[] payload = message.getBytes(StandardCharsets.UTF_8);
		byte[] encrypted = WebPushCrypto.encrypt(payload, device.p256dh, device.authKey);

		// Extract audience from endpoint URL
		URI endpointUri = URI.create(device.endpoint);
		String audience = endpointUri.getScheme() + "://" + endpointUri.getHost();

		// Create VAPID auth header
		String authHeader = WebPushCrypto.createVapidAuthHeader(
			vapidPrivateKey, vapidPublicKey, audience, vapidSubject);

		// Send HTTP POST to push endpoint
		HttpRequest request = HttpRequest.newBuilder()
			.uri(endpointUri)
			.header("Content-Type", "application/octet-stream")
			.header("Content-Encoding", "aes128gcm")
			.header("Authorization", authHeader)
			.header("TTL", "86400")
			.timeout(Duration.ofSeconds(30))
			.POST(HttpRequest.BodyPublishers.ofByteArray(encrypted))
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		int statusCode = response.statusCode();
		if (statusCode == 201 || statusCode == 202)
		{
			log.debug("Push sent successfully to {}", device.endpoint.substring(0, Math.min(80, device.endpoint.length())));
		}
		else
		{
			log.warn("Push endpoint returned HTTP {}: {}", statusCode, response.body());
		}

		return statusCode;
	}
}
