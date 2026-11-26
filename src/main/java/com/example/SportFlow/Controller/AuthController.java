package com.example.SportFlow.Controller;

import com.example.SportFlow.config.TokenConfig;
import com.example.SportFlow.doc.AuthControllerDoc;
import com.example.SportFlow.Dto.User.Auth.request.LoginRequest;
import com.example.SportFlow.Dto.User.Auth.request.RegisterUserRequest;
import com.example.SportFlow.Dto.User.Auth.response.LoginResponse;
import com.example.SportFlow.Dto.User.Auth.response.RegisterUserResponse;
import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.Enum.Perfil;
import com.example.SportFlow.Repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController implements AuthControllerDoc {

    private final UsuarioRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenConfig tokenConfig;

    public AuthController(UsuarioRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          TokenConfig tokenConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenConfig = tokenConfig;
    }

    /**
     * 1) AuthenticationManager busca o usuário pelo e-mail (AuthConfig) e compara a senha com BCrypt.
     * 2) Se estiver certo, gera o JWT. Se estiver errado, lança BadCredentialsException -> 401.
     */
    @Override
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        String email = loginRequest.email().trim().toLowerCase(Locale.ROOT);
        var userAndPass = new UsernamePasswordAuthenticationToken(email, loginRequest.password());
        Authentication authentication = authenticationManager.authenticate(userAndPass);

        Usuario user = (Usuario) authentication.getPrincipal();
        String token = tokenConfig.generateToken(user);

        log.info("Login realizado: {} ({})", user.getEmail(), user.getPerfil());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    /** Cadastro público: sempre cria um usuário com perfil CLIENTE. */
    @Override
    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }

        Usuario newUser = new Usuario();
        newUser.setNome(request.name().trim());
        newUser.setEmail(email);
        newUser.setSenha(passwordEncoder.encode(request.password()));
        newUser.setPerfil(Perfil.CLIENTE);

        userRepository.save(newUser);
        log.info("Novo usuário cadastrado: {}", email);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterUserResponse(newUser.getId(), newUser.getNome(), newUser.getEmail(), newUser.getPerfil()));
    }
}
