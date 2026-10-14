package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.dto.request.LoginRequest;
import com.example.SportFlow.dto.request.RegisterRequest;
import com.example.SportFlow.dto.response.LoginResponse;
import com.example.SportFlow.dto.response.UsuarioResponse;
import com.example.SportFlow.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public UsuarioResponse registrar(RegisterRequest request) {
        return usuarioService.registrar(request);
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha())
        );
        Usuario usuario = (Usuario) authentication.getPrincipal();
        return new LoginResponse(
                jwtService.gerarToken(usuario),
                "Bearer",
                jwtService.getExpirationSeconds()
        );
    }
}
