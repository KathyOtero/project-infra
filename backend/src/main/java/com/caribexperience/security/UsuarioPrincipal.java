package com.caribexperience.security;

import com.caribexperience.domain.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador entre la entidad de dominio {@link Usuario} y el contrato
 * {@link UserDetails} que exige Spring Security. Se opta por envolver la
 * entidad (en vez de que Usuario implemente UserDetails directamente) para
 * no acoplar la capa de dominio a Spring Security.
 *
 * El rol se expone como authority con el prefijo "ROLE_" (convencion que
 * espera Spring Security para que hasRole("GUIA") funcione en @PreAuthorize).
 */
@Getter
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String nombre;
    private final String email;
    private final String passwordHash;
    private final String rol;
    private final boolean activo;

    public UsuarioPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.nombre = usuario.getNombre() + " " + usuario.getApellido();
        this.email = usuario.getEmail();
        this.passwordHash = usuario.getPasswordHash();
        this.rol = usuario.getRol().getNombre();
        this.activo = Boolean.TRUE.equals(usuario.getActivo());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
