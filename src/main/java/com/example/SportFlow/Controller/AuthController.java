package com.example.SportFlow.Controller;

import com.example.SportFlow.config.TokenConfig;
import com.example.SportFlow.doc.AuthControllerDoc;
import com.example.SportFlow.Dto.User.Auth.request.LoginRequest;
import com.example.SportFlow.Dto.User.Auth.request.RegisterUserRequest;
import com.example.SportFlow.Dto.User.Auth.response.LoginResponse;
import com.example.SportFlow.Dto.User.Auth.response.RegisterUserResponse;
import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.Repository.UsuarioRepository;
import jakarta.validation.Valid;
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

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken userAndPass = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());
        Authentication authentication = authenticationManager.authenticate(userAndPass);
        Usuario user = (Usuario) authentication.getPrincipal();
        String token = tokenConfig.generateToken(user);
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(
            @Valid @RequestBody RegisterUserRequest request) {

        Usuario newUser = new Usuario();

        newUser.setSenha(passwordEncoder.encode(request.password()));
        newUser.setNome(request.name());
        newUser.setEmail(request.email());

        userRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterUserResponse(
                        newUser.getNome(),
                        newUser.getEmail()
                ));
    }

}







