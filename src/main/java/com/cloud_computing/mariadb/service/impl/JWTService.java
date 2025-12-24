package com.cloud_computing.mariadb.service.impl;

import com.cloud_computing.mariadb.dto.UserDTO;
import com.cloud_computing.mariadb.entity.User;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JWTService {
    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGN_KEY;

    public String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .claim("name", user.getName())
                .issuer("mariadb.com")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()))
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwSObject = new JWSObject(header, payload);
        try {
            jwSObject.sign(new MACSigner(SIGN_KEY.getBytes()));
            return jwSObject.serialize();
        } catch (JOSEException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public String generateInvitationToken(Long dbId, String email, String role, String dbName, String inviterName) {
        try {
            // 1. Tạo HMAC signer
            JWSSigner signer = new MACSigner(SIGN_KEY.getBytes());

            // 2. Tạo claims
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject("db-invitation")
                    .claim("dbId", dbId)
                    .claim("email", email)
                    .claim("role", role)
                    .claim("dbName", dbName)
                    .claim("inviterName", inviterName)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 24h
                    .build();

            // 3. Tạo signed JWT
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );

            // 4. Sign
            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (Exception e) {
            throw new RuntimeException("Tạo token thất bại: " + e.getMessage());
        }
    }
    public Map<String, Object> parseInvitationToken(String token) {
        try {
            // 1. Parse signed JWT
            SignedJWT signedJWT = SignedJWT.parse(token);

            // 2. Verify signature
            JWSVerifier verifier = new MACVerifier(SIGN_KEY.getBytes());
            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Token signature không hợp lệ");
            }

            // 3. Get claims
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // 4. Kiểm tra expiration
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime != null && new Date().after(expirationTime)) {
                throw new RuntimeException("Invitation đã hết hạn");
            }

            // 5. Extract data
            Map<String, Object> result = new HashMap<>();
            result.put("dbId", claims.getLongClaim("dbId"));
            result.put("email", claims.getStringClaim("email"));
            result.put("role", claims.getStringClaim("role"));

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Token không hợp lệ: " + e.getMessage());
        }
    }
}

