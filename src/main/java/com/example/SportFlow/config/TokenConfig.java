package com.example.SportFlow.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.SportFlow.Entity.Usuario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class TokenConfig {

    private final Algorithm algorithm;

    public TokenConfig(@Value("${jwt.secret}") String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String generateToken(Usuario user) {
        return JWT.create()
                .withIssuer("SportFlow")
                .withSubject(user.getEmail())
                .withClaim("userId", user.getId().toString())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(86400))
                .sign(algorithm);
    }

    public Optional<JWTUserData> validateToken(String token) {
        try {
            DecodedJWT decoded = JWT.require(algorithm)
                    .withIssuer("SportFlow")
                    .build()
                    .verify(token);

            UUID userId = UUID.fromString(
                    decoded.getClaim("userId").asString()
            );

            return Optional.of(new JWTUserData(
                    userId,
                    decoded.getSubject()
            ));

        } catch (JWTVerificationException |
                 IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}