package com.salygin.ssubench.security;

import com.salygin.ssubench.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final JwtProperties config;
    private final ObjectMapper mapper;

    public String issueToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(config.getExpiration());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", config.getIssuer());
        payload.put("sub", user.getId().toString());
        payload.put("email", user.getEmail());
        payload.put("role", user.getRole());
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        try {
            String encodedHeader = encodeBase64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = encodeBase64Url(mapper.writeValueAsBytes(payload));

            String unsignedToken = encodedHeader + "." + encodedPayload;
            String signature = encodeBase64Url(createSignature(unsignedToken));

            return unsignedToken + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Token generation failed", e);
        }
    }

    public Optional<JwtPrincipal> decode(String token) {
        try {
            String[] segments = token.split("\\.");
            if (segments.length != 3) {
                return Optional.empty();
            }

            String headerSegment = segments[0];
            String payloadSegment = segments[1];
            String signatureSegment = segments[2];

            if (!signatureMatches(headerSegment, payloadSegment, signatureSegment)) {
                return Optional.empty();
            }

            JsonNode headerJson = parseSegment(headerSegment);
            if (!"HS256".equals(headerJson.path("alg").asText(null))) {
                return Optional.empty();
            }

            JsonNode body = parseSegment(payloadSegment);

            if (!config.getIssuer().equals(body.path("iss").asText(null))) {
                return Optional.empty();
            }

            long expiration = body.path("exp").asLong(0L);
            if (expiration <= 0 || Instant.now().isAfter(Instant.ofEpochSecond(expiration))) {
                return Optional.empty();
            }

            String subject = body.path("sub").asText(null);
            String mail = body.path("email").asText(null);
            String role = body.path("role").asText(null);

            if (subject == null || subject.isBlank()
                    || mail == null || mail.isBlank()
                    || role == null || role.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new JwtPrincipal(UUID.fromString(subject), mail, role));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private boolean signatureMatches(String header, String payload, String signature) throws Exception {
        String data = header + "." + payload;
        byte[] expected = createSignature(data);
        byte[] provided = Base64.getUrlDecoder().decode(signature);
        return MessageDigest.isEqual(expected, provided);
    }

    private byte[] createSignature(String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec key = new SecretKeySpec(
                config.getSecret().getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256
        );
        mac.init(key);
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode parseSegment(String segment) throws Exception {
        byte[] decoded = Base64.getUrlDecoder().decode(segment);
        return mapper.readTree(decoded);
    }

    private String encodeBase64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
