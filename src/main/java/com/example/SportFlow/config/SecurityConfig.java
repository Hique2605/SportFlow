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
 * ADMIN .............. /usuarios/** (exceto GET /usuarios/me), /api/funcionarios/**,
 *                      escrita (POST/PUT/DELETE) em esportes, estabelecimentos, quadras e horários,
 *                      PATCH /agendamentos/{id}/status, DELETE /agendamentos/{id},
 *                      PATCH /pagamentos/{id}/confirmar, PUT e DELETE /pagamentos/{id}
 * Qualquer logado .... leitura do catálogo, agendamentos, pagamentos e avaliações
 *                      (os services ainda verificam se o recurso é do próprio usuário)
 *
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
                        .requestMatchers(HttpMethod.GET, "/usuarios/me").authenticated()
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")

                        // Funcionários
                        .requestMatchers("/api/funcionarios/**").hasRole("ADMIN")

                        // Catálogo: todos leem, só ADMIN altera
                        .requestMatchers(HttpMethod.GET, CATALOGO).authenticated()
                        .requestMatchers(CATALOGO).hasRole("ADMIN")

                        // Agendamentos
                        .requestMatchers(HttpMethod.PATCH, "/agendamentos/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/agendamentos/**").hasRole("ADMIN")

                        // Pagamentos
                        .requestMatchers(HttpMethod.PATCH, "/pagamentos/*/confirmar").hasRole("ADMIN")
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
