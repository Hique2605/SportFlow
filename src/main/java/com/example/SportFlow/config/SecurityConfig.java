package com.example.SportFlow.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Regras de acesso (autenticação + autorização).
 *
 * Público ............ POST /auth/login, POST /auth/register, Swagger
 * ADMIN .............. /usuarios/** (exceto /usuarios/me), /api/funcionarios/**,
 *                      escrita em esportes, criar/excluir estabelecimento,
 *                      DELETE /agendamentos/{id}, PUT e DELETE /pagamentos/{id}
 * ADMIN ou GESTOR .... editar o estabelecimento, criar/editar/excluir quadras e horários,
 *                      PATCH /agendamentos/{id}/status, PATCH /pagamentos/{id}/confirmar
 *                      -> aqui o SecurityConfig só exige login; quem confere se o usuário
 *                         gerencia AQUELE estabelecimento são os services (UsuarioLogado.verificarGestor)
 * Qualquer logado .... leitura do catálogo, agendamentos, pagamentos e avaliações
 *                      (os services ainda verificam se o recurso é do próprio usuário)
 *
 * GESTOR = usuário com vínculo de funcionário ativo no estabelecimento.
 * Sem token / token inválido -> 401 | Logado sem permissão -> 403
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] CATALOGO = {
            "/api/esportes/**",
            "/api/estabelecimentos/**",
            "/api/quadras/**",
            "/api/horarios/**"
    };

    private final SecurityFilter securityFilter;

    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults()) // usa o CorsConfigurationSource de CorsConfig
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        // Sem token ou token inválido: 401 (antes era 403)
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // Públicas
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Usuários
                        .requestMatchers(HttpMethod.GET, "/usuarios/me", "/usuarios/me/**").authenticated()
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")

                        // Funcionários
                        .requestMatchers("/api/funcionarios/**").hasRole("ADMIN")

                        // Catálogo: todos leem
                        .requestMatchers(HttpMethod.GET, CATALOGO).authenticated()
                        // Esportes e criar/excluir estabelecimento: só ADMIN
                        .requestMatchers("/api/esportes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/estabelecimentos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/estabelecimentos/**").hasRole("ADMIN")
                        // Editar estabelecimento, quadras e horários: ADMIN ou gestor (conferido no service)
                        .requestMatchers(CATALOGO).authenticated()

                        // Agendamentos (mudar status: ADMIN ou gestor, conferido no service)
                        .requestMatchers(HttpMethod.DELETE, "/agendamentos/**").hasRole("ADMIN")

                        // Pagamentos (confirmar: ADMIN ou gestor, conferido no service)
                        .requestMatchers(HttpMethod.PUT, "/pagamentos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/pagamentos/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
