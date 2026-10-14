package com.example.SportFlow.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.example.SportFlow.Entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class JwtService {

    private static final String ISSUER = "sportflow-api";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expirationSeconds;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
        this.expirationSeconds = expirationSeconds;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(usuario.getEmail())
                .withClaim("usuarioId", usuario.getId().toString())
                .withClaim("perfil", usuario.getPerfil().name())
                .withIssuedAt(agora)
                .withExpiresAt(agora.plusSeconds(expirationSeconds))
                .sign(algorithm);
    }

    public Optional<String> validarEObterEmail(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            return Optional.ofNullable(jwt.getSubject());
        } catch (JWTVerificationException exception) {
            return Optional.empty();
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
