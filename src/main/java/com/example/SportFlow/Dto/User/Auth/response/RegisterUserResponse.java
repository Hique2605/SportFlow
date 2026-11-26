package com.example.SportFlow.Dto.User.Auth.response;

import com.example.SportFlow.Enum.Perfil;

import java.util.UUID;

public record RegisterUserResponse(
        UUID id,
        String name,
        String email,
        Perfil perfil
) {
}
