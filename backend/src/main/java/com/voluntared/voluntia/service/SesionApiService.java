package com.voluntared.voluntia.service;

import com.voluntared.voluntia.config.PerfilAutenticado;
import com.voluntared.voluntia.dto.CuentaDTO;
import com.voluntared.voluntia.repository.UsuarioRepository;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.stereotype.Service;

@Service
public class SesionApiService {
    private final AuthenticationManager autenticador;
    private final SecurityContextRepository contextos;
    private final CsrfTokenRepository csrf;
    private final CuentaService cuentas;
    private final AccesoService acceso;
    private final String adminEmail;
    private final UsuarioRepository usuarios;

    public SesionApiService(AuthenticationManager autenticador, SecurityContextRepository contextos,
                            CsrfTokenRepository csrf, CuentaService cuentas, AccesoService acceso, UsuarioRepository usuarios,
                            @Value("${voluntia.admin.email}") String adminEmail) {
        this.autenticador = autenticador;
        this.contextos = contextos;
        this.csrf = csrf;
        this.cuentas = cuentas;
        this.acceso = acceso;
        this.adminEmail = adminEmail;
        this.usuarios = usuarios;
    }

    public CuentaDTO sesion() {
        if (acceso.admin()) {
            var cuenta = new CuentaDTO();
            cuenta.setId(0);
            cuenta.setRol("ADMIN");
            cuenta.setNombre("Administración Voluntia");
            cuenta.setEmail(adminEmail);
            cuenta.setUbicacion("");
            if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof PerfilAutenticado perfil) {
                var usuario = usuarios.findById(perfil.getUsuarioId()).orElseThrow(
                        () -> new BadCredentialsException("La cuenta ya no está disponible"));
                cuenta.setId(usuario.getId());
                cuenta.setNombre(usuario.getNombre());
                cuenta.setEmail(usuario.getEmail());
                cuenta.setUbicacion(usuario.getUbicacion());
            }
            return cuenta;
        }
        return acceso.rol("VOLUNTARIO") || acceso.rol("ORGANIZACION") ? cuentas.miCuenta() : null;
    }

    public CuentaDTO ingresar(String email, String clave, HttpServletRequest request, HttpServletResponse response) {
        if (clave.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new BadCredentialsException("Credenciales incorrectas");
        var autenticacion = autenticador.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, clave));
        new ChangeSessionIdAuthenticationStrategy().onAuthentication(autenticacion, request, response);
        new CsrfAuthenticationStrategy(csrf).onAuthentication(autenticacion, request, response);
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
        contextos.saveContext(contexto, request, response);
        return sesion();
    }

    public void salir(HttpServletRequest request, HttpServletResponse response) {
        var logout = new SecurityContextLogoutHandler();
        logout.setSecurityContextRepository(contextos);
        logout.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    }
}
