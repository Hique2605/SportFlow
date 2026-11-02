package com.example.SportFlow.Dto.User.Auth.request;

import jakarta.validation.constraints.NotEmpty;

public record RegisterUserRequest(
        @NotEmpty(message = "Campo nome é obrigatório")
        String name,

        @NotEmpty(message = "Campo e-mail é obrigatório")
        String email,

        @NotEmpty(message = "Campo senha é obrigatório")
        String password
) {
}
