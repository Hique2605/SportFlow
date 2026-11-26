package com.example.SportFlow.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.Enum.Perfil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class TokenConfig {

    private static final String ISSUER = "SportFlow";
    private static final long VALIDADE_SEGUNDOS = 86400; // 24 horas

    private final Algorithm algorithm;

    public TokenConfig(@Value("${jwt.secret}") String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    /** Gera o token assinado com e-mail (subject), id e perfil do usuário. */
    public String generateToken(Usuario user) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getEmail())
                .withClaim("userId", user.getId().toString())
                .withClaim("perfil", user.getPerfil().name())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(VALIDADE_SEGUNDOS))
                .sign(algorithm);
    }

    /** Valida assinatura, emissor e validade. Token inválido = Optional vazio. */
    public Optional<JWTUserData> validateToken(String token) {
        try {
            DecodedJWT decoded = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);

            UUID userId = UUID.fromString(decoded.getClaim("userId").asString());
            String perfilClaim = decoded.getClaim("perfil").asString();
            // Tokens antigos (gerados antes dos perfis) são tratados como CLIENTE
            Perfil perfil = perfilClaim == null ? Perfil.CLIENTE : Perfil.valueOf(perfilClaim);

            return Optional.of(new JWTUserData(userId, decoded.getSubject(), perfil));

        } catch (JWTVerificationException | IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }
}
