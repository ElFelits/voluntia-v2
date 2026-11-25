package com.voluntared.voluntia.config;

import com.voluntared.voluntia.entity.Usuario;
import com.voluntared.voluntia.repository.UsuarioRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

final class VigenciaSesionFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarios;
    private final SecurityContextRepository contextos;

    VigenciaSesionFilter(UsuarioRepository usuarios, SecurityContextRepository contextos) {
        this.usuarios = usuarios;
        this.contextos = contextos;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof PerfilAutenticado perfil
                && usuarios.findById(perfil.getUsuarioId()).filter(Usuario::isActivo)
                    .filter(u -> autenticacion.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_" + u.getRol()))).isEmpty()) {
            var contexto = SecurityContextHolder.createEmptyContext();
            SecurityContextHolder.setContext(contexto);
            contextos.saveContext(contexto, request, response);
        }
        chain.doFilter(request, response);
    }
}
