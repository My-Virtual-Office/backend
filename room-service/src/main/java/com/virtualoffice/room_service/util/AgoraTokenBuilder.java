package com.virtualoffice.room_service.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.zip.Deflater;

/**
 * Agora AccessToken2 ("007") builder for RTC channels.
 *
 * Wire format, which Agora's gateway parses strictly — every field is little-endian:
 *
 *   signingKey  = HMAC(key=salt,    msg=HMAC(key=issuedAt, msg=appCertificate))
 *   signingInfo = str(appId) | u32(issuedAt) | u32(expireSeconds) | u32(salt) | u16(serviceCount)
 *                 | u16(serviceType) | u16(privCount) | [u16(priv) u32(expiryTs)]...
 *                 | str(channelName) | str(uid)
 *   signature   = HMAC(key=signingKey, msg=signingInfo)
 *   token       = "007" + base64(zlib( str(signature) | signingInfo ))
 *
 * where str(x) = u16(byteLength) followed by the bytes, and u16/u32 are little-endian.
 *
 * The signature comes FIRST in the payload and is length-prefixed; the App ID is a
 * length-prefixed string, not raw bytes. Agora reads the App ID back out of the token, so
 * any drift here surfaces as the very misleading "invalid vendor key, can not find appid"
 * rather than a signature error.
 */
public final class AgoraTokenBuilder {

    private static final String VERSION = "007";
    private static final int SERVICE_TYPE_RTC = 1;

    // RTC privileges. All four are granted for the same window: this service hands the token
    // to a voice client that also needs to publish audio.
    private static final int[] RTC_PRIVILEGES = {
            1, // join channel
            2, // publish audio stream
            3, // publish video stream
            4, // publish data stream
    };

    private static final SecureRandom RANDOM = new SecureRandom();

    private AgoraTokenBuilder() {
    }

    /**
     * Build an Agora RTC token.
     *
     * @param appId          Agora App ID (32-char hex)
     * @param appCertificate Agora App Certificate (32-char hex); blank => App-ID-only mode
     * @param channelName    Agora channel name
     * @param uid            user id; 0 means "any uid may use this token"
     * @param expireSeconds  token lifetime in seconds, counted from now
     * @return a "007…" token, or "" when no certificate is configured
     */
    public static String buildToken(String appId, String appCertificate,
                                    String channelName, int uid, int expireSeconds) {
        if (appCertificate == null || appCertificate.isBlank()) {
            return "";
        }
        try {
            int issuedAt = (int) (System.currentTimeMillis() / 1000);
            int salt = RANDOM.nextInt(99_999_999) + 1;
            int privilegeExpire = issuedAt + expireSeconds;

            byte[] signingInfo = packSigningInfo(appId, issuedAt, expireSeconds, salt,
                    channelName, uid, privilegeExpire);

            // Two-step key derivation: the certificate never signs the payload directly.
            byte[] signingKey = hmac(uint32LE(salt), hmac(uint32LE(issuedAt),
                    appCertificate.getBytes(StandardCharsets.UTF_8)));
            byte[] signature = hmac(signingKey, signingInfo);

            ByteArrayOutputStream content = new ByteArrayOutputStream();
            packString(content, signature);
            content.write(signingInfo);

            return VERSION + Base64.getEncoder().encodeToString(zlibCompress(content.toByteArray()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build Agora token", e);
        }
    }

    private static byte[] packSigningInfo(String appId, int issuedAt, int expireSeconds, int salt,
                                          String channelName, int uid, int privilegeExpire)
            throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        packString(buf, appId.getBytes(StandardCharsets.UTF_8));
        buf.write(uint32LE(issuedAt));
        buf.write(uint32LE(expireSeconds)); // a duration, not an absolute timestamp
        buf.write(uint32LE(salt));
        buf.write(uint16LE(1)); // exactly one service: RTC

        buf.write(uint16LE(SERVICE_TYPE_RTC));
        buf.write(uint16LE(RTC_PRIVILEGES.length));
        for (int privilege : RTC_PRIVILEGES) {
            buf.write(uint16LE(privilege));
            buf.write(uint32LE(privilegeExpire)); // absolute, unlike the token expiry above
        }
        // Privileges precede channel/uid — the reverse order is silently accepted by nothing.
        packString(buf, channelName.getBytes(StandardCharsets.UTF_8));
        packString(buf, uid == 0
                ? new byte[0] // empty uid = wildcard
                : Integer.toUnsignedString(uid).getBytes(StandardCharsets.UTF_8));

        return buf.toByteArray();
    }

    private static void packString(ByteArrayOutputStream buf, byte[] bytes) throws Exception {
        buf.write(uint16LE(bytes.length));
        buf.write(bytes);
    }

    private static byte[] uint32LE(int v) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array();
    }

    private static byte[] uint16LE(int v) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) v).array();
    }

    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] zlibCompress(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        while (!deflater.finished()) {
            out.write(chunk, 0, deflater.deflate(chunk));
        }
        deflater.end();
        return out.toByteArray();
    }
}
