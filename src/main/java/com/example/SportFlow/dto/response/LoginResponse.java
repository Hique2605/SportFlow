package com.example.SportFlow.dto.response;

public record LoginResponse(
        String token,
        String tipo,
        long expiraEmSegundos
) {
}
