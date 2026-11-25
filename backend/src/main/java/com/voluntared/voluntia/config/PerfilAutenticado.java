package com.voluntared.voluntia.config;

import com.voluntared.voluntia.entity.Usuario;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

public final class PerfilAutenticado extends User {
    private final Integer usuarioId;

    public PerfilAutenticado(Usuario usuario) {
        super(usuario.getEmail(), usuario.getContrasena(), usuario.isActivo(), true, true, true,
                AuthorityUtils.createAuthorityList("ROLE_" + usuario.getRol()));
        this.usuarioId = usuario.getId();
    }

    public Integer getUsuarioId() { return usuarioId; }
}
