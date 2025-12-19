package com.cloud_computing.mariadb.service.impl;

import com.cloud_computing.mariadb.dto.UserDTO;
import com.cloud_computing.mariadb.entity.User;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

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
}

