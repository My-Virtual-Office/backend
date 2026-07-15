/*
 * Copyright (c) 2025 My Virtual Office
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */
package com.virtualoffice.room_service.util;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.Inflater;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Decodes what buildToken emits and re-derives the signature independently, so the token
 * is checked against the AccessToken2 wire format rather than against itself.
 *
 * This exists because a malformed token is not a loud failure: Agora reads the App ID out
 * of the token body, so a layout slip is reported as "invalid vendor key, can not find
 * appid" — which reads like a bad App ID and sends you debugging the wrong thing entirely.
 */
class AgoraTokenBuilderTest {

    private static final String APP_ID = "5db80389fc284a3a8c166979882f118d";
    private static final String APP_CERT = "a6790438be834910b050de22cfcf2555";

    @Test
    void blankCertificateYieldsNoToken() {
        assertThat(AgoraTokenBuilder.buildToken(APP_ID, "", "room-1", 64, 3600)).isEmpty();
        assertThat(AgoraTokenBuilder.buildToken(APP_ID, null, "room-1", 64, 3600)).isEmpty();
    }

    @Test
    void tokenCarriesTheAppIdChannelAndUidAgoraWillReadBack() {
        String token = AgoraTokenBuilder.buildToken(APP_ID, APP_CERT, "room-abc", 64, 3600);

        assertThat(token).startsWith("007");
        Decoded d = decode(token);

        assertThat(d.appId).isEqualTo(APP_ID);
        assertThat(d.channel).isEqualTo("room-abc");
        assertThat(d.uid).isEqualTo("64");
        assertThat(d.expire).isEqualTo(3600); // a duration, not an absolute timestamp
        assertThat(d.serviceCount).isEqualTo(1);
        assertThat(d.serviceType).isEqualTo(1); // RTC
        assertThat(d.privileges).containsOnlyKeys(1, 2, 3, 4);
        assertThat(d.privileges.values()).allMatch(v -> v == d.issuedAt + 3600);
        assertThat(d.trailingBytes).isZero(); // every byte accounted for
    }

    @Test
    void uidZeroIsPackedAsTheWildcard() {
        assertThat(decode(AgoraTokenBuilder.buildToken(APP_ID, APP_CERT, "room-abc", 0, 3600)).uid)
                .isEmpty();
    }

    @Test
    void signatureMatchesAnIndependentDerivation() throws Exception {
        Decoded d = decode(AgoraTokenBuilder.buildToken(APP_ID, APP_CERT, "room-abc", 64, 3600));

        // signingKey = HMAC(salt, HMAC(issuedAt, certificate)) — the certificate never signs
        // the payload directly.
        byte[] step1 = hmac(le32(d.issuedAt), APP_CERT.getBytes(StandardCharsets.UTF_8));
        byte[] signingKey = hmac(le32(d.salt), step1);
        assertThat(d.signature).isEqualTo(hmac(signingKey, d.signingInfo));
    }

    @Test
    void everyTokenGetsAFreshSalt() {
        String a = AgoraTokenBuilder.buildToken(APP_ID, APP_CERT, "room-abc", 64, 3600);
        String b = AgoraTokenBuilder.buildToken(APP_ID, APP_CERT, "room-abc", 64, 3600);
        assertThat(a).isNotEqualTo(b);
    }

    // ---- minimal AccessToken2 reader -------------------------------------------------

    private record Decoded(String appId, int issuedAt, int expire, int salt, int serviceCount,
                           int serviceType, Map<Integer, Integer> privileges, String channel,
                           String uid, byte[] signature, byte[] signingInfo, int trailingBytes) {
    }

    private static Decoded decode(String token) {
        byte[] raw = inflate(Base64.getDecoder().decode(token.substring(3)));
        ByteBuffer buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);

        byte[] signature = readBytes(buf);
        // Whatever follows the signature is exactly the payload that was signed.
        byte[] signingInfo = new byte[buf.remaining()];
        buf.duplicate().get(signingInfo);

        String appId = new String(readBytes(buf), StandardCharsets.UTF_8);
        int issuedAt = buf.getInt();
        int expire = buf.getInt();
        int salt = buf.getInt();
        int serviceCount = Short.toUnsignedInt(buf.getShort());
        int serviceType = Short.toUnsignedInt(buf.getShort());

        int privCount = Short.toUnsignedInt(buf.getShort());
        Map<Integer, Integer> privileges = new LinkedHashMap<>();
        for (int i = 0; i < privCount; i++) {
            privileges.put(Short.toUnsignedInt(buf.getShort()), buf.getInt());
        }
        String channel = new String(readBytes(buf), StandardCharsets.UTF_8);
        String uid = new String(readBytes(buf), StandardCharsets.UTF_8);

        return new Decoded(appId, issuedAt, expire, salt, serviceCount, serviceType, privileges,
                channel, uid, signature, signingInfo, buf.remaining());
    }

    private static byte[] readBytes(ByteBuffer buf) {
        byte[] out = new byte[Short.toUnsignedInt(buf.getShort())];
        buf.get(out);
        return out;
    }

    private static byte[] le32(int v) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array();
    }

    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] inflate(byte[] data) {
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(data);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            while (!inflater.finished()) {
                out.write(chunk, 0, inflater.inflate(chunk));
            }
            inflater.end();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("token payload is not valid zlib", e);
        }
    }
}
