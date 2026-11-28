package com.voluntared.voluntia.config;

import com.voluntared.voluntia.entity.Usuario;
import com.voluntared.voluntia.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.web.cors.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(@Value("${voluntia.admin.password}") String password,
                                                  @Value("${voluntia.admin.email}") String adminEmail,
                                                  PasswordEncoder encoder, UsuarioRepository usuarios) {
        String adminHash = password.isBlank() ? null : encoder.encode(password);
        return nombre -> {
            String login = nombre.trim();
            if (adminEmail.equalsIgnoreCase(login)) {
                if (adminHash == null) throw new UsernameNotFoundException("Credenciales incorrectas");
                return User.withUsername(adminEmail).password(adminHash).roles("ADMIN").build();
            }
            return usuarios.findByEmailIgnoreCase(login)
                    .filter(Usuario::isActivo)
                    .filter(u -> Set.of("VOLUNTARIO", "ORGANIZACION", "ADMIN").contains(u.getRol()))
                    .map(PerfilAutenticado::new)
                    .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
        };
    }

    @Bean AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        var proveedor = new DaoAuthenticationProvider(users);
        proveedor.setPasswordEncoder(encoder);
        return new ProviderManager(proveedor);
    }

    @Bean org.springframework.security.web.csrf.CsrfTokenRepository csrfRepository() {
        return new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository();
    }

    @Bean SecurityContextRepository contextRepository() {
        return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }

    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${voluntia.cors.origins}") String origins) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean SecurityFilterChain security(HttpSecurity http, AuthenticationManager manager,
                                       SecurityContextRepository contextRepository,
                                       @org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource") CorsConfigurationSource cors, UsuarioRepository usuarios,
                                       org.springframework.security.web.csrf.CsrfTokenRepository csrf) throws Exception {
        return http.authenticationManager(manager).cors(c -> c.configurationSource(cors))
                .csrf(c -> c.csrfTokenRepository(csrf))
                .securityContext(c -> c.securityContextRepository(contextRepository))
                .addFilterBefore(new VigenciaSesionFilter(usuarios, contextRepository),
                        org.springframework.security.web.csrf.CsrfFilter.class)
                .authorizeHttpRequests(a -> a
                    .requestMatchers("/error").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/csrf", "/api/auth/sesion", "/api/distritos", "/api/campanias",
                            "/api/campanias/{id}", "/api/campanias/{id}/portada/{portadaId}",
                            "/api/organizaciones/{id}", "/api/organizaciones/{id}/campanias").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/ingresar", "/api/auth/registro", "/api/auth/salir").permitAll()
                    .requestMatchers("/api/voluntarios/**", "/api/organizaciones/**").hasRole("ADMIN")
                    .requestMatchers("/api/mi-cuenta").hasAnyRole("VOLUNTARIO", "ORGANIZACION")
                    .requestMatchers("/api/mis-participaciones",
                            "/api/campanias/{id}/mi-inscripcion").hasRole("VOLUNTARIO")
                    .requestMatchers(HttpMethod.POST, "/api/campanias").hasRole("ORGANIZACION")
                    .requestMatchers("/api/campanias/**", "/api/impacto").hasAnyRole("ADMIN", "ORGANIZACION")
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll())
                .logout(l -> l.disable())
                .requestCache(c -> c.disable())
                .exceptionHandling(e -> e
                    .authenticationEntryPoint((req, res, ex) -> error(res, 401, "Inicia sesión para continuar"))
                    .accessDeniedHandler((req, res, ex) -> error(res, 403, "No tienes permiso o la sesión de seguridad expiró")))
                .build();
    }

    private static void error(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}
