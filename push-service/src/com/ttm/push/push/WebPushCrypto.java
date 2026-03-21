package com.ttm.push.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Base64;

/**
 * Web Push encryption and VAPID implementation using only JDK crypto.
 * Implements RFC 8291 (Message Encryption for Web Push), RFC 8292 (VAPID),
 * and RFC 8188 (aes128gcm Content-Coding).
 */
public class WebPushCrypto
{
	private static final Logger log = LoggerFactory.getLogger(WebPushCrypto.class);
	private static final String CURVE = "secp256r1";
	private static final int KEY_LENGTH = 16; // AES-128
	private static final int NONCE_LENGTH = 12;
	private static final int TAG_LENGTH = 128; // bits

	/**
	 * Generate a new VAPID key pair (P-256/prime256v1).
	 */
	public static KeyPair generateKeyPair() throws Exception
	{
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
		kpg.initialize(new ECGenParameterSpec(CURVE));
		return kpg.generateKeyPair();
	}

	/**
	 * Encode an EC public key as uncompressed point (65 bytes: 0x04 || x || y).
	 */
	public static byte[] encodePublicKey(ECPublicKey key)
	{
		byte[] x = toUnsignedBytes(key.getW().getAffineX(), 32);
		byte[] y = toUnsignedBytes(key.getW().getAffineY(), 32);
		byte[] result = new byte[65];
		result[0] = 0x04;
		System.arraycopy(x, 0, result, 1, 32);
		System.arraycopy(y, 0, result, 33, 32);
		return result;
	}

	/**
	 * Decode an uncompressed EC public key point (65 bytes) to ECPublicKey.
	 */
	public static ECPublicKey decodePublicKey(byte[] encoded) throws Exception
	{
		if (encoded.length != 65 || encoded[0] != 0x04)
			throw new IllegalArgumentException("Invalid uncompressed public key");

		byte[] x = Arrays.copyOfRange(encoded, 1, 33);
		byte[] y = Arrays.copyOfRange(encoded, 33, 65);

		ECPoint point = new ECPoint(new BigInteger(1, x), new BigInteger(1, y));
		AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
		params.init(new ECGenParameterSpec(CURVE));
		ECParameterSpec spec = params.getParameterSpec(ECParameterSpec.class);

		KeyFactory kf = KeyFactory.getInstance("EC");
		return (ECPublicKey) kf.generatePublic(new ECPublicKeySpec(point, spec));
	}

	/**
	 * Decode a VAPID private key from raw 32-byte format.
	 */
	public static ECPrivateKey decodePrivateKey(byte[] raw) throws Exception
	{
		AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
		params.init(new ECGenParameterSpec(CURVE));
		ECParameterSpec spec = params.getParameterSpec(ECParameterSpec.class);

		KeyFactory kf = KeyFactory.getInstance("EC");
		return (ECPrivateKey) kf.generatePrivate(new ECPrivateKeySpec(new BigInteger(1, raw), spec));
	}

	/**
	 * Encrypt a message for Web Push (RFC 8291 / aes128gcm).
	 *
	 * @param payload       the plaintext message
	 * @param p256dhBase64  browser's P-256 public key (base64url)
	 * @param authBase64    browser's auth secret (base64url)
	 * @return encrypted payload in aes128gcm format
	 */
	public static byte[] encrypt(byte[] payload, String p256dhBase64, String authBase64) throws Exception
	{
		Base64.Decoder decoder = Base64.getUrlDecoder();
		byte[] clientPublicKeyBytes = decoder.decode(p256dhBase64);
		byte[] authSecret = decoder.decode(authBase64);

		ECPublicKey clientPublicKey = decodePublicKey(clientPublicKeyBytes);

		// Generate ephemeral key pair for this message
		KeyPair ephemeral = generateKeyPair();
		ECPublicKey ephemeralPublic = (ECPublicKey) ephemeral.getPublic();
		byte[] ephemeralPublicBytes = encodePublicKey(ephemeralPublic);

		// ECDH shared secret
		KeyAgreement ka = KeyAgreement.getInstance("ECDH");
		ka.init(ephemeral.getPrivate());
		ka.doPhase(clientPublicKey, true);
		byte[] sharedSecret = ka.generateSecret();

		// IKM = HKDF-Extract(auth_secret, ecdh_secret)
		// Then derive PRK using "WebPush: info" context
		byte[] ikm = hkdfExtract(authSecret, sharedSecret);

		// info = "WebPush: info" || 0x00 || client_public || server_public || 0x00
		byte[] info = buildInfo("WebPush: info", clientPublicKeyBytes, ephemeralPublicBytes);
		byte[] prk = hkdfExpand(ikm, info, 32);

		// Derive content encryption key
		byte[] cekInfo = buildContentInfo("Content-Encoding: aes128gcm", new byte[0]);
		byte[] cek = hkdfExpand(prk, cekInfo, KEY_LENGTH);

		// Derive nonce
		byte[] nonceInfo = buildContentInfo("Content-Encoding: nonce", new byte[0]);
		byte[] nonce = hkdfExpand(prk, nonceInfo, NONCE_LENGTH);

		// Pad plaintext: payload || 0x02 (delimiter for final record)
		byte[] padded = new byte[payload.length + 1];
		System.arraycopy(payload, 0, padded, 0, payload.length);
		padded[payload.length] = 0x02; // final record delimiter

		// AES-128-GCM encrypt
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE,
			new SecretKeySpec(cek, "AES"),
			new GCMParameterSpec(TAG_LENGTH, nonce));
		byte[] encrypted = cipher.doFinal(padded);

		// Build aes128gcm header: salt(16) || rs(4) || idlen(1) || keyid(65)
		byte[] salt = new byte[16];
		SecureRandom.getInstanceStrong().nextBytes(salt);

		// Re-derive with actual salt
		ikm = hkdfExtract(authSecret, sharedSecret);
		prk = hkdfExpand(ikm, info, 32);
		cek = hkdfExpand(prk, cekInfo, KEY_LENGTH);
		nonce = hkdfExpand(prk, nonceInfo, NONCE_LENGTH);

		// Actually use the salt in the key derivation
		// RFC 8291: PRK = HKDF-Extract(auth, ecdh_secret), then
		// IKM for content = HKDF-Expand(PRK, info, 32)
		// Then salt is used: extract(salt, IKM) -> PRK_content
		// Then expand for CEK and nonce
		byte[] prkContent = hkdfExtract(salt, prk);
		cek = hkdfExpand(prkContent, cekInfo, KEY_LENGTH);
		nonce = hkdfExpand(prkContent, nonceInfo, NONCE_LENGTH);

		cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE,
			new SecretKeySpec(cek, "AES"),
			new GCMParameterSpec(TAG_LENGTH, nonce));
		encrypted = cipher.doFinal(padded);

		int recordSize = encrypted.length + 86; // header size

		// aes128gcm header
		ByteBuffer header = ByteBuffer.allocate(86 + encrypted.length);
		header.put(salt);                         // 16 bytes salt
		header.putInt(recordSize);                // 4 bytes record size
		header.put((byte) 65);                    // 1 byte keyid length
		header.put(ephemeralPublicBytes);          // 65 bytes ephemeral public key
		header.put(encrypted);                    // encrypted content

		return header.array();
	}

	/**
	 * Create VAPID Authorization header value.
	 */
	public static String createVapidAuthHeader(ECPrivateKey privateKey, ECPublicKey publicKey,
		String audience, String subject) throws Exception
	{
		long now = System.currentTimeMillis() / 1000;
		long exp = now + 12 * 3600; // 12 hours

		// JWT Header: {"typ":"JWT","alg":"ES256"}
		String headerJson = "{\"typ\":\"JWT\",\"alg\":\"ES256\"}";
		String payloadJson = "{\"aud\":\"" + audience + "\",\"exp\":" + exp +
			",\"sub\":\"" + subject + "\"}";

		Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
		String headerB64 = b64.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
		String payloadB64 = b64.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

		String signingInput = headerB64 + "." + payloadB64;

		// Sign with ES256
		Signature sig = Signature.getInstance("SHA256withECDSA");
		sig.initSign(privateKey);
		sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
		byte[] derSignature = sig.sign();

		// Convert DER to raw R||S (64 bytes)
		byte[] rawSignature = derToRaw(derSignature);
		String signatureB64 = b64.encodeToString(rawSignature);

		String jwt = signingInput + "." + signatureB64;
		String publicKeyB64 = b64.encodeToString(encodePublicKey(publicKey));

		return "vapid t=" + jwt + ",k=" + publicKeyB64;
	}

	// --- HKDF (RFC 5869) ---

	static byte[] hkdfExtract(byte[] salt, byte[] ikm) throws Exception
	{
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(salt.length > 0 ? salt : new byte[32], "HmacSHA256"));
		return mac.doFinal(ikm);
	}

	static byte[] hkdfExpand(byte[] prk, byte[] info, int length) throws Exception
	{
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(prk, "HmacSHA256"));

		byte[] result = new byte[length];
		byte[] t = new byte[0];
		int offset = 0;
		byte counter = 1;

		while (offset < length)
		{
			mac.reset();
			mac.update(t);
			mac.update(info);
			mac.update(counter);
			t = mac.doFinal();
			int toCopy = Math.min(t.length, length - offset);
			System.arraycopy(t, 0, result, offset, toCopy);
			offset += toCopy;
			counter++;
		}
		return result;
	}

	// --- Helper methods ---

	private static byte[] buildInfo(String type, byte[] clientPublicKey, byte[] serverPublicKey)
	{
		byte[] typeBytes = type.getBytes(StandardCharsets.UTF_8);
		ByteBuffer buf = ByteBuffer.allocate(typeBytes.length + 1 + clientPublicKey.length + serverPublicKey.length);
		buf.put(typeBytes);
		buf.put((byte) 0x00);
		buf.put(clientPublicKey);
		buf.put(serverPublicKey);
		return buf.array();
	}

	private static byte[] buildContentInfo(String type, byte[] context)
	{
		byte[] typeBytes = type.getBytes(StandardCharsets.UTF_8);
		ByteBuffer buf = ByteBuffer.allocate(typeBytes.length + 1 + context.length);
		buf.put(typeBytes);
		buf.put((byte) 0x00);
		buf.put(context);
		return buf.array();
	}

	/**
	 * Convert DER-encoded ECDSA signature to raw R||S format (64 bytes).
	 * JDK produces DER, but JWT needs raw.
	 */
	static byte[] derToRaw(byte[] der)
	{
		// DER: 0x30 <len> 0x02 <rLen> <r> 0x02 <sLen> <s>
		int offset = 2; // skip 0x30 and length
		if (der[1] == (byte) 0x81) offset = 3; // long form length

		// R
		offset++; // skip 0x02
		int rLen = der[offset++] & 0xFF;
		byte[] r = Arrays.copyOfRange(der, offset, offset + rLen);
		offset += rLen;

		// S
		offset++; // skip 0x02
		int sLen = der[offset++] & 0xFF;
		byte[] s = Arrays.copyOfRange(der, offset, offset + sLen);

		byte[] raw = new byte[64];
		// Copy R (right-aligned to 32 bytes, strip leading zero if present)
		int rStart = r.length > 32 ? r.length - 32 : 0;
		int rDest = r.length < 32 ? 32 - r.length : 0;
		System.arraycopy(r, rStart, raw, rDest, Math.min(r.length, 32));

		// Copy S (right-aligned to 32 bytes)
		int sStart = s.length > 32 ? s.length - 32 : 0;
		int sDest = s.length < 32 ? 32 - s.length : 0;
		System.arraycopy(s, sStart, raw, 32 + sDest, Math.min(s.length, 32));

		return raw;
	}

	/**
	 * Convert BigInteger to unsigned byte array of fixed length.
	 */
	private static byte[] toUnsignedBytes(BigInteger value, int length)
	{
		byte[] bytes = value.toByteArray();
		if (bytes.length == length)
		{
			return bytes;
		}
		byte[] result = new byte[length];
		if (bytes.length > length)
		{
			// Strip leading zero byte
			System.arraycopy(bytes, bytes.length - length, result, 0, length);
		}
		else
		{
			// Pad with leading zeros
			System.arraycopy(bytes, 0, result, length - bytes.length, bytes.length);
		}
		return result;
	}

	/**
	 * CLI tool to generate VAPID keys.
	 */
	public static void main(String[] args)
	{
		if (args.length > 0 && "generate-keys".equals(args[0]))
		{
			try
			{
				KeyPair kp = generateKeyPair();
				ECPublicKey pub = (ECPublicKey) kp.getPublic();
				ECPrivateKey priv = (ECPrivateKey) kp.getPrivate();

				Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
				String publicKeyB64 = b64.encodeToString(encodePublicKey(pub));
				String privateKeyB64 = b64.encodeToString(toUnsignedBytes(priv.getS(), 32));

				System.out.println("=== VAPID Keys Generated ===");
				System.out.println();
				System.out.println("Public Key (for conf/push-service.properties AND app.js):");
				System.out.println(publicKeyB64);
				System.out.println();
				System.out.println("Private Key (for conf/push-service.properties ONLY):");
				System.out.println(privateKeyB64);
				System.out.println();
				System.out.println("Add to conf/push-service.properties:");
				System.out.println("vapid.public.key=" + publicKeyB64);
				System.out.println("vapid.private.key=" + privateKeyB64);
				System.out.println();
				System.out.println("Add to web/push/app.js:");
				System.out.println("const VAPID_PUBLIC_KEY = '" + publicKeyB64 + "';");
			}
			catch (Exception e)
			{
				System.err.println("Error generating keys: " + e.getMessage());
				e.printStackTrace();
				System.exit(1);
			}
		}
		else
		{
			System.out.println("Usage: WebPushCrypto generate-keys");
		}
	}
}
