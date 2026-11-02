package com.example.SportFlow.doc;

import com.example.SportFlow.Dto.User.Auth.request.LoginRequest;
import com.example.SportFlow.Dto.User.Auth.request.RegisterUserRequest;
import com.example.SportFlow.Dto.User.Auth.response.LoginResponse;
import com.example.SportFlow.Dto.User.Auth.response.RegisterUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(
        name = "Auth",
        description = "Endpoints responsáveis pela autenticação e cadastro de usuários"
)
public interface AuthControllerDoc {

    @Operation(
            summary = "Realiza o login do usuário",
            description = "Autentica o usuário utilizando e-mail e senha e retorna um token JWT."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login realizado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de login inválidos"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "E-mail ou senha incorretos"
            )
    })
    ResponseEntity<LoginResponse> login(LoginRequest loginRequest);


    @Operation(
            summary = "Cadastra um novo usuário",
            description = "Cria um novo usuário no sistema utilizando nome, e-mail e senha."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário cadastrado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegisterUserResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de cadastro inválidos"
            )
    })
    ResponseEntity<RegisterUserResponse> register(RegisterUserRequest request);

}
